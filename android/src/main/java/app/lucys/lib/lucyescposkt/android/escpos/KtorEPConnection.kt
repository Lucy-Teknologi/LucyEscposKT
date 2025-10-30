package sample.lucys.lucyescposktpoc.data.escpos

import android.util.Log
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnection
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import sample.lucys.lucyescposktpoc.domain.escpos.EPConnection
import sample.lucys.lucyescposktpoc.domain.escpos.EPOfflineStatus
import sample.lucys.lucyescposktpoc.domain.escpos.EPPaperStatus
import sample.lucys.lucyescposktpoc.domain.escpos.EPPrintResult
import sample.lucys.lucyescposktpoc.domain.escpos.EPPrinterStatus
import sample.lucys.lucyescposktpoc.domain.escpos.constants.EPStatusConstants
import sample.lucys.lucyescposktpoc.domain.printer.PrinterConnectionSpec
import kotlin.experimental.and
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class KtorEPConnection(
    override val spec: PrinterConnectionSpec.TCP,
) : EPConnection {

    private var _client: HttpClient? = null
    private var _socket: Socket? = null


    override suspend fun isConnected(): Boolean {
        return _socket?.isClosed == false && _client?.isActive == true
    }

    override suspend fun connect(timeout: Duration): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeout) {
                _client = HttpClient(OkHttp)

                val manager = ActorSelectorManager(Dispatchers.IO)
                val address = InetSocketAddress(spec.ip, spec.port.toInt())
                _socket = aSocket(manager).tcp().connect(address)
            }

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            return@withContext false
        }
    }

    override suspend fun disconnect() {
        _client?.close()
        _client = null

        _socket?.close()
        _socket = null
    }

    private suspend fun getPaperStatus(
        input: ByteReadChannel,
        output: ByteWriteChannel,
    ): EPPaperStatus {
        output.writeFully(EPStatusConstants.PAPER_SENSOR_STATUS)
        output.flush()

        val hasContent = input.awaitContent()
        if (!hasContent) {
            return EPPaperStatus.UNKNOWN
        }

        val response = input.readByte()

        val isOutOfPaper = response.and(EPStatusConstants.PAPER_EMPTY_STATUS) != 0.toByte()
        if (isOutOfPaper) {
            return EPPaperStatus.EMPTY
        }

        val isLowOnPaper = response.and(EPStatusConstants.PAPER_LOW_STATUS) != 0.toByte()
        if (isLowOnPaper) {
            return EPPaperStatus.LOW
        }

        return EPPaperStatus.AVAILABLE
    }

    private suspend fun getStatusOverview(
        input: ByteReadChannel,
        output: ByteWriteChannel,
    ): EPPrinterStatus? {
        Log.d("KtorEPConnection", "Sending printer status request")

        output.writeFully(EPStatusConstants.PRINTER_STATUS)
        output.flush()

        Log.d("KtorEPConnection", "Awaiting status content")
        val hasContent = input.awaitContent()
        if (!hasContent) {
            return null
        }

        val response = input.readByte()
        val isOffline = response.and(EPStatusConstants.STATUS_CHECK_OFFLINE) != 0.toByte()
        val isBusy = response.and(EPStatusConstants.STATUS_CHECK_BUSY) != 0.toByte()

        Log.d("KtorEPConnection", "Status is offline: $isOffline, busy: $isBusy")
        return EPPrinterStatus(isOnline = !isOffline, isBusy = isBusy)
    }

    private suspend fun getOfflineStatus(
        input: ByteReadChannel,
        output: ByteWriteChannel,
    ): EPOfflineStatus? {
        output.writeFully(EPStatusConstants.OFFLINE_CAUSE_STATUS)
        output.flush()

        val hasContent = input.awaitContent()
        if (!hasContent) {
            return null
        }

        val response = input.readByte()
        val isCoverOpen = response.and(EPStatusConstants.OFFLINE_COVER_OPEN) != 0.toByte()
        val isFeedPressed = response.and(EPStatusConstants.OFFLINE_PAPER_FEED) != 0.toByte()
        val isOutOfPaper = response.and(EPStatusConstants.OFFLINE_PAPER_OUT) != 0.toByte()
        val didErrorOccur = response.and(EPStatusConstants.OFFLINE_UNKNOWN_ERROR) != 0.toByte()

        return EPOfflineStatus(
            isCoverOpen = isCoverOpen,
            isFeedPressed = isFeedPressed,
            isOutOfPaper = isOutOfPaper,
            didErrorOccur = didErrorOccur,
        )
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun send(
        command: ByteArray,
        timeout: Duration,
    ): EPPrintResult = withContext(Dispatchers.IO) {
        _socket?.let { socket ->
            if (socket.isClosed) return@withContext EPPrintResult.NotConnected

            val reader = socket.openReadChannel()
            val writer = socket.openWriteChannel(autoFlush = true)

            val paperStatus = getPaperStatus(reader, writer)
            Log.d("KtorEPConnection", "Paper status: $paperStatus")

            if (paperStatus == EPPaperStatus.EMPTY) {
                writer.flushAndClose()
                reader.cancel()

                Log.d("KtorEPConnection", "Paper is empty")
                return@withContext EPPrintResult.Failed(offlineStatus = EPOfflineStatus.outOfPaper())
            }

            writer.writeFully(command)
            writer.flush()

            var status: EPPrinterStatus
            val timestamp = Clock.System.now()

            do {
                val res = getStatusOverview(reader, writer)

                if (res == null) {
                    writer.flushAndClose()
                    reader.cancel()
                    Log.d("KtorEPConnection", "getStatusOverview returned null")
                    return@withContext EPPrintResult.NotConnected
                }

                status = res
                Log.d("KtorEPConnection", "GET STATUS $res")

                val timeDiff = Clock.System.now().minus(timestamp)
                Log.d("KtorEPConnection", "Time diff: $timeDiff")
                delay(500)
            } while (
                res.isBusy && timeDiff < timeout
            )

            if (status.isOnline) {
                Log.d("KtorEPConnection", "Status is online")
                val paperStatus = getPaperStatus(reader, writer)

                writer.flushAndClose()
                reader.cancel()

                return@withContext EPPrintResult.Success(status = status, paperStatus = paperStatus)
            }

            val offline = getOfflineStatus(reader, writer)
            writer.flushAndClose()
            reader.cancel()

            Log.d("KtorEPConnection", "Status is not online")
            return@withContext offline
                ?.let { EPPrintResult.Failed(it) }
                ?: EPPrintResult.Timeout(status)
        }

        Log.d("KtorEPConnection", "Socket is null")
        EPPrintResult.NotConnected
    }

}
