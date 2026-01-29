package app.lucys.lib.lucyescposkt.android.escpos

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import app.lucys.lib.lucyescposkt.core.escpos.EPOfflineStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPaperStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPrintResult
import app.lucys.lib.lucyescposkt.core.escpos.EPPrinterStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPStreamData
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnection
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPStatusConstants
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.experimental.and
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

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
        } catch (_: Exception) {
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
        output.write(EPStatusConstants.PRINTER_STATUS)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

        val isOffline = response.and(EPStatusConstants.STATUS_CHECK_OFFLINE) != 0.toByte()
        val isBusy = response.and(EPStatusConstants.STATUS_CHECK_BUSY) != 0.toByte()

        return EPPrinterStatus(isOnline = !isOffline, isBusy = isBusy)
    }

    private suspend fun asyncGetOfflineStatus(
        input: InputStream,
        output: OutputStream,
    ): EPOfflineStatus? {
        output.write(EPStatusConstants.OFFLINE_CAUSE_STATUS)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

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

    private suspend fun getPaperStatus(
        input: InputStream,
        output: OutputStream,
    ): Pair<EPPaperStatus, Byte> {
        output.write(EPStatusConstants.PAPER_SENSOR_STATUS)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

        val isOutOfPaper = response.and(EPStatusConstants.PAPER_EMPTY_STATUS) != 0.toByte()
        if (isOutOfPaper) {
            return Pair(EPPaperStatus.EMPTY, response)
        }

        val isLowOnPaper = response.and(EPStatusConstants.PAPER_LOW_STATUS) != 0.toByte()
        if (isLowOnPaper) {
            return Pair(EPPaperStatus.LOW, response)
        }

        return Pair(EPPaperStatus.AVAILABLE, response)
    }

    private suspend fun getStatusOverview(
        input: InputStream,
        output: OutputStream,
    ): Pair<EPPrinterStatus?, Byte> {
        output.write(EPStatusConstants.PRINTER_STATUS)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

        val isOffline = response.and(EPStatusConstants.STATUS_CHECK_OFFLINE) != 0.toByte()
        val isBusy = response.and(EPStatusConstants.STATUS_CHECK_BUSY) != 0.toByte()

        val result = EPPrinterStatus(isOnline = !isOffline, isBusy = isBusy)
        return Pair(result, response)
    }

    private suspend fun getOfflineStatus(
        input: InputStream,
        output: OutputStream,
    ): Pair<EPOfflineStatus?, Byte> {
        output.write(EPStatusConstants.OFFLINE_CAUSE_STATUS)
        output.flush()

        delay(100)

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

        val isCoverOpen = response.and(EPStatusConstants.OFFLINE_COVER_OPEN) != 0.toByte()
        val isFeedPressed = response.and(EPStatusConstants.OFFLINE_PAPER_FEED) != 0.toByte()
        val isOutOfPaper = response.and(EPStatusConstants.OFFLINE_PAPER_OUT) != 0.toByte()
        val didErrorOccur = response.and(EPStatusConstants.OFFLINE_UNKNOWN_ERROR) != 0.toByte()

        val status = EPOfflineStatus(
            isCoverOpen = isCoverOpen,
            isFeedPressed = isFeedPressed,
            isOutOfPaper = isOutOfPaper,
            didErrorOccur = didErrorOccur,
        )

        return Pair(status, response)
    }

    private suspend fun waitUntilReady(
        input: InputStream,
        output: OutputStream,
        timeout: Duration,
    ): Boolean {
        withContext(Dispatchers.IO) {
            output.write(byteArrayOf(0x1D, 0x72, 0x01))
            output.flush()
        }

        delay(100)

        return withTimeout(timeout) {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    if (input.available() > 0) {
                        val status = input.read()
                        return@withContext status != -1
                    }
                    delay(300)
                }

                return@withContext false
            }
        }
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
            if (initialStatus == null) {
                writer.close()
                reader.close()
                return@withContext EPPrintResult.NotConnected
            }

            if (!initialStatus.isOnline) {
                val offline = asyncGetOfflineStatus(reader, writer)
                writer.flush()
                writer.close()
                reader.close()

                return@withContext EPPrintResult.Failed(offline ?: EPOfflineStatus.outOfPaper())
            }

            val paperStatus = asyncGetPaperStatus(reader, writer)

            if (paperStatus == EPPaperStatus.EMPTY) {
                writer.close()
                reader.close()

                return@withContext EPPrintResult.Failed(offlineStatus = EPOfflineStatus.outOfPaper())
            }

            writer.write(command)
            writer.flush()

            val status: EPPrinterStatus?
            try {
                val isReady = waitUntilReady(reader, writer, timeout)
                if (!isReady) return@withContext EPPrintResult.NotConnected
                status = asyncGetStatusOverview(reader, writer)
            } catch (_: TimeoutCancellationException) {
                Log.d("BTManagerEPConnection", "Timeout reached")
                val offline = asyncGetOfflineStatus(reader, writer)

                writer.close()
                reader.close()
                return@withContext EPPrintResult.Failed(
                    offline ?: EPOfflineStatus.outOfPaper()
                )
            }

            if (status == null) {
                writer.flush()
                writer.close()
                reader.close()
                return@withContext EPPrintResult.NotConnected
            }

            if (status.isOnline) {
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

            return@withContext offline
                ?.let { EPPrintResult.Failed(it) }
                ?: EPPrintResult.Timeout(status)
        }

        EPPrintResult.NotConnected
    }

    override fun stream(command: ByteArray, timeout: Duration): Flow<EPStreamData> = flow {
        val socket = _socket
        if (socket == null) {
            emit(EPStreamData.Log("Not connected to printer!"))
            return@flow
        }

        val result = let {
            if (!socket.isConnected) return@let EPPrintResult.NotConnected

            val reader = socket.inputStream
            val writer = socket.outputStream

            emit(EPStreamData.Log("Attempting to read status!"))
            val (initialStatus, statByte) = getStatusOverview(reader, writer)
            emit(EPStreamData.Log("Status byte response $statByte"))

            if (initialStatus == null) {
                emit(EPStreamData.Log("No response from printer read status!"))

                writer.close()
                reader.close()
                return@let EPPrintResult.NotConnected
            }

            if (!initialStatus.isOnline) {
                emit(EPStreamData.Log("Printer is offline!"))

                emit(EPStreamData.Log("Attempting to read offline status"))
                val (offline, byte) = getOfflineStatus(reader, writer)
                emit(EPStreamData.Log("Offline status byte $byte"))

                writer.flush()
                writer.close()
                reader.close()

                return@let EPPrintResult.Failed(offline ?: EPOfflineStatus.outOfPaper())
            }

            emit(EPStreamData.Log("Printer is online!"))
            emit(EPStreamData.Log("Attempting to read paper status"))
            val (paperStatus, paperByte) = getPaperStatus(reader, writer)
            emit(EPStreamData.Log("Paper status byte $paperByte"))

            if (paperStatus == EPPaperStatus.EMPTY) {
                emit(EPStreamData.Log("Paper is empty!"))

                writer.close()
                reader.close()
                return@let EPPrintResult.Failed(offlineStatus = EPOfflineStatus.outOfPaper())
            }

            emit(EPStreamData.Log("Sending command"))
            writer.write(command)
            writer.flush()

            val status: EPPrinterStatus?
            try {
                emit(EPStreamData.Log("Waiting for printer to be ready"))
                val isReady = waitUntilReady(reader, writer, timeout)
                emit(EPStreamData.Log("Printer is ready? $isReady"))
                if (!isReady) return@let EPPrintResult.NotConnected

                emit(EPStreamData.Log("Reading printer status"))
                val (res, byte) = getStatusOverview(reader, writer)
                emit(EPStreamData.Log("Status byte response $byte"))
                status = res
            } catch (_: TimeoutCancellationException) {
                emit(EPStreamData.Log("Timeout waiting for printer ready"))
                emit(EPStreamData.Log("Reading offline status"))
                val (offline, byte) = getOfflineStatus(reader, writer)
                emit(EPStreamData.Log("Offline byte response $byte"))

                writer.flush()
                writer.close()
                reader.close()
                return@let EPPrintResult.Failed(
                    offline ?: EPOfflineStatus.outOfPaper()
                )
            }

            if (status == null) {
                writer.flush()
                writer.close()
                reader.close()

                emit(EPStreamData.Log("No response from printer status"))
                return@let EPPrintResult.NotConnected
            }

            if (status.isOnline) {
                emit(EPStreamData.Log("Printer is online!"))
                emit(EPStreamData.Log("Reading paper status"))

                val (paperStatus, byte) = getPaperStatus(reader, writer)
                emit(EPStreamData.Log("Paper status byte $byte"))

                writer.flush()
                writer.close()
                reader.close()

                return@let EPPrintResult.Success(status = status, paperStatus = paperStatus)
            }

            emit(EPStreamData.Log("Printer is offline!"))
            emit(EPStreamData.Log("Reading offline status"))
            val (offline, offlineByte) = getOfflineStatus(reader, writer)
            emit(EPStreamData.Log("Offline byte response $offlineByte"))

            writer.flush()
            writer.close()
            reader.close()

            return@let offline
                ?.let { EPPrintResult.Failed(it) }
                ?: EPPrintResult.Timeout(status)
        }

        emit(EPStreamData.Result(result))
    }
}