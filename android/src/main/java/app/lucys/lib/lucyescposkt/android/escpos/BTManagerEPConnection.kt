package app.lucys.lib.lucyescposkt.android.escpos

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import app.lucys.lib.lucyescposkt.core.escpos.EPOfflineStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPaperStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPrintResult
import app.lucys.lib.lucyescposkt.core.escpos.EPPrinterStatus
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnection
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPStatusConstants
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.experimental.and
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class BTManagerEPConnection(
    override val spec: PrinterConnectionSpec.Bluetooth
) : EPConnection {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var _socket: BluetoothSocket? = null

    override suspend fun isConnected(): Boolean {
        return _socket?.isConnected == true
    }

    override suspend fun connect(timeout: Duration): Boolean = withContext(Dispatchers.IO) {
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(spec.mac)

        try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            _socket = socket
            socket.connect()

            true
        } catch (e: Exception) {
            Log.e("BTManagerEPConnection", "Error connecting to printer", e)

            _socket?.close()
            _socket = null
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        _socket?.close()
        _socket = null
    }

    private suspend fun asyncGetPaperStatus(
        input: InputStream,
        output: OutputStream,
    ): EPPaperStatus {
        output.write(EPStatusConstants.PAPER_SENSOR_STATUS)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()
        Log.d("BTManagerEPConnection", "Paper status response: $response")


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

    private suspend fun asyncGetStatusOverview(
        input: InputStream,
        output: OutputStream,
    ): EPPrinterStatus? {
        Log.d("BTManagerEPConnection", "Sending printer status request")

        output.write(EPStatusConstants.PRINTER_STATUS)
        output.flush()

        delay(10)

        val buffer = ByteArray(1)
        val executionTime = measureTime {
            input.read(buffer)
        }
        Log.d("BTManagerEPConnection", "Reading printer status time: $executionTime")

        val response = buffer.first()
        Log.d("BTManagerEPConnection", "Status response: $response")
        val isOffline = response.and(EPStatusConstants.STATUS_CHECK_OFFLINE) != 0.toByte()
        val isBusy = response.and(EPStatusConstants.STATUS_CHECK_BUSY) != 0.toByte()

        Log.d("BTManagerEPConnection", "Status is offline: $isOffline, busy: $isBusy")
        return EPPrinterStatus(isOnline = !isOffline, isBusy = isBusy)
    }

    private suspend fun asyncGetOfflineStatus(
        input: InputStream,
        output: OutputStream,
    ): EPOfflineStatus? {
        Log.d("BTManagerEPConnection", "Sending offline status request")
        output.write(EPStatusConstants.OFFLINE_CAUSE_STATUS)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()
        Log.d("BTManagerEPConnection", "offline status response: $response")

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

    private suspend fun getStatusOverview(
        input: InputStream,
        output: OutputStream,
        timeout: Duration,
    ): EPPrinterStatus? {
        output.write(EPStatusConstants.PRINTER_STATUS_AWAIT)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)

        try {
            withTimeout(timeout) {
                input.read(buffer)
            }
        } catch (_: Exception) {
            return null
        }

        val response = buffer.first()
        Log.d("BTManagerEPConnection", "Status response: $response")

        val isOffline = response.and(EPStatusConstants.STATUS_CHECK_OFFLINE) != 0.toByte()
        val isBusy = response.and(EPStatusConstants.STATUS_CHECK_BUSY) != 0.toByte()

        Log.d("BTManagerEPConnection", "Status is offline: $isOffline, busy: $isBusy")
        return EPPrinterStatus(isOnline = !isOffline, isBusy = isBusy)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun send(
        command: ByteArray,
        timeout: Duration
    ): EPPrintResult = withContext(Dispatchers.IO) {
        _socket?.let { socket ->
            if (!socket.isConnected) return@withContext EPPrintResult.NotConnected

            val reader = socket.inputStream
            val writer = socket.outputStream

            val initialStatus = asyncGetStatusOverview(reader, writer)
            Log.d("BTManagerEPConnection", "Initial status: $initialStatus")

            if (initialStatus == null) {
                writer.close()
                reader.close()
                Log.d("BTManagerEPConnection", "Initial status is null")
                return@withContext EPPrintResult.NotConnected
            }

            if (!initialStatus.isOnline) {
                val offline = asyncGetOfflineStatus(reader, writer)
                writer.flush()
                writer.close()
                reader.close()

                Log.d("BTManagerEPConnection", "Initial status is offline")
                return@withContext EPPrintResult.Failed(offline ?: EPOfflineStatus.outOfPaper())
            }

            val paperStatus = asyncGetPaperStatus(reader, writer)
            Log.d("BTManagerEPConnection", "Paper status: $paperStatus")

            if (paperStatus == EPPaperStatus.EMPTY) {
                writer.close()
                reader.close()

                Log.d("BTManagerEPConnection", "Paper is empty")
                return@withContext EPPrintResult.Failed(offlineStatus = EPOfflineStatus.outOfPaper())
            }

            writer.write(command)
            writer.flush()

            var status: EPPrinterStatus
            val timestamp = Clock.System.now()

            do {
                Log.d("BTManagerEPConnection", "Trying to read status")
                val res = asyncGetStatusOverview(reader, writer)

                if (res == null) {
                    writer.flush()
                    writer.close()
                    reader.close()
                    Log.d("BTManagerEPConnection", "getStatusOverview returned null")
                    return@withContext EPPrintResult.NotConnected
                }

                status = res
                Log.d("BTManagerEPConnection", "GET STATUS $res")

                val timeDiff = Clock.System.now().minus(timestamp)
                Log.d("BTManagerEPConnection", "Time diff: $timeDiff")
                delay(50)
            } while (
                res.isBusy && timeDiff < timeout
            )

            if (status.isOnline) {
                Log.d("BTManagerEPConnection", "Status is online")
                val paperStatus = asyncGetPaperStatus(reader, writer)

                writer.flush()
                writer.close()
                reader.close()

                return@withContext EPPrintResult.Success(status = status, paperStatus = paperStatus)
            }

            val offline = asyncGetOfflineStatus(reader, writer)
            writer.flush()
            writer.close()
            reader.close()

            Log.d("BTManagerEPConnection", "Status is not online")
            return@withContext offline
                ?.let { EPPrintResult.Failed(it) }
                ?: EPPrintResult.Timeout(status)
        }

        Log.d("BTManagerEPConnection", "Socket is null")
        EPPrintResult.NotConnected
    }
}