package app.lucys.lib.lucyescposkt.android.escpos

import app.lucys.lib.lucyescposkt.core.escpos.EPOfflineStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPaperStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPrintResult
import app.lucys.lib.lucyescposkt.core.escpos.EPPrinterStatus
import app.lucys.lib.lucyescposkt.core.escpos.command.EPWaitType
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnection
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPStatusConstants
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.experimental.and
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class KtorEPConnection(
    override val spec: PrinterConnectionSpec.TCP,
    override val waitType: EPWaitType = EPWaitType.WAIT,
) : EPConnection {

    private var _selectorManager: SelectorManager? = null
    private var _socket: Socket? = null

    override suspend fun isConnected(): Boolean {
        return _socket != null && _socket?.isClosed == false
    }

    override suspend fun connect(timeout: Duration): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeout) {
                val selector = SelectorManager(Dispatchers.IO)
                _selectorManager = selector
                val address = InetSocketAddress(spec.ip, spec.port.toInt())
                _socket = aSocket(selector).tcp().connect(address)
            }
            true
        } catch (_: Exception) {
            disconnect()
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            _socket?.close()
        } catch (_: Exception) {}
        _socket = null

        try {
            _selectorManager?.close()
        } catch (_: Exception) {}
        _selectorManager = null
    }

    private suspend fun getPaperStatus(
        input: ByteReadChannel,
        output: ByteWriteChannel,
        timeout: Duration = 2.seconds,
    ): EPPaperStatus = withTimeout(timeout) {
        output.writeFully(EPStatusConstants.PAPER_SENSOR_STATUS)
        output.flush()

        delay(100)

        val hasContent = input.awaitContent()
        if (!hasContent) {
            return@withTimeout EPPaperStatus.UNKNOWN
        }

        val response = input.readByte()

        val isOutOfPaper = response.and(EPStatusConstants.PAPER_EMPTY_STATUS) != 0.toByte()
        if (isOutOfPaper) {
            return@withTimeout EPPaperStatus.EMPTY
        }

        val isLowOnPaper = response.and(EPStatusConstants.PAPER_LOW_STATUS) != 0.toByte()
        if (isLowOnPaper) {
            return@withTimeout EPPaperStatus.LOW
        }

        return@withTimeout EPPaperStatus.AVAILABLE
    }

    private suspend fun getStatusOverview(
        input: ByteReadChannel,
        output: ByteWriteChannel,
        timeout: Duration = 2.seconds,
    ): EPPrinterStatus? = withTimeout(timeout) {
        output.writeFully(EPStatusConstants.PRINTER_STATUS)
        output.flush()

        delay(100)

        val hasContent = input.awaitContent()
        if (!hasContent) {
            return@withTimeout null
        }

        val response = input.readByte()
        val isOffline = response.and(EPStatusConstants.STATUS_CHECK_OFFLINE) != 0.toByte()
        val isBusy = response.and(EPStatusConstants.STATUS_CHECK_BUSY) != 0.toByte()

        return@withTimeout EPPrinterStatus(isOnline = !isOffline, isBusy = isBusy)
    }

    private suspend fun getOfflineStatus(
        input: ByteReadChannel,
        output: ByteWriteChannel,
        timeout: Duration = 2.seconds,
    ): EPOfflineStatus? = withTimeout(timeout) {
        output.writeFully(EPStatusConstants.OFFLINE_CAUSE_STATUS)
        output.flush()

        delay(100)

        val hasContent = input.awaitContent()
        if (!hasContent) {
            return@withTimeout null
        }

        val response = input.readByte()
        val isCoverOpen = response.and(EPStatusConstants.OFFLINE_COVER_OPEN) != 0.toByte()
        val isFeedPressed = response.and(EPStatusConstants.OFFLINE_PAPER_FEED) != 0.toByte()
        val isOutOfPaper = response.and(EPStatusConstants.OFFLINE_PAPER_OUT) != 0.toByte()
        val didErrorOccur = response.and(EPStatusConstants.OFFLINE_UNKNOWN_ERROR) != 0.toByte()

        EPOfflineStatus(
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
        val socket = _socket
        if (socket == null || socket.isClosed) return@withContext EPPrintResult.NotConnected

        try {
            val reader = socket.openReadChannel()
            val writer = socket.openWriteChannel(autoFlush = true)

            // Direct byte transmission to thermal printer
            writer.writeFully(command)
            writer.flush()

            if (waitType == EPWaitType.RT) {
                val status = withTimeoutOrNull(500.milliseconds) {
                    getStatusOverview(reader, writer, timeout = 300.milliseconds)
                }

                if (status != null && !status.isOnline) {
                    val offline = withTimeoutOrNull(300.milliseconds) {
                        getOfflineStatus(reader, writer, timeout = 200.milliseconds)
                    }
                    return@withContext EPPrintResult.Failed(offline ?: EPOfflineStatus.outOfPaper())
                }

                val paperStatus = withTimeoutOrNull(300.milliseconds) {
                    getPaperStatus(reader, writer, timeout = 200.milliseconds)
                }

                if (paperStatus == EPPaperStatus.EMPTY) {
                    return@withContext EPPrintResult.Failed(offlineStatus = EPOfflineStatus.outOfPaper())
                }

                return@withContext EPPrintResult.Success(
                    status = status ?: EPPrinterStatus(isOnline = true, isBusy = false),
                    paperStatus = paperStatus ?: EPPaperStatus.AVAILABLE,
                )
            }

            EPPrintResult.Success(
                status = EPPrinterStatus(isOnline = true, isBusy = false),
                paperStatus = EPPaperStatus.AVAILABLE,
            )
        } catch (e: Exception) {
            disconnect()
            EPPrintResult.NotConnected
        }
    }
}
