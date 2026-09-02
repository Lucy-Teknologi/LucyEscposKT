package app.lucys.lib.lucyescposkt.android.escpos

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import app.lucys.lib.lucyescposkt.core.escpos.EPOfflineStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPaperStatus
import app.lucys.lib.lucyescposkt.core.escpos.EPPrintResult
import app.lucys.lib.lucyescposkt.core.escpos.EPPrinterStatus
import app.lucys.lib.lucyescposkt.core.escpos.command.EPWaitType
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnection
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPStatusConstants
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.experimental.and
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class BTManagerEPConnection(
    override val spec: PrinterConnectionSpec.Bluetooth,
    override val waitType: EPWaitType = EPWaitType.WAIT,
) : EPConnection {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var _socket: BluetoothSocket? = null

    override suspend fun isConnected(): Boolean {
        return _socket != null && _socket?.isConnected == true
    }

    override suspend fun connect(timeout: Duration): Boolean = withContext(Dispatchers.IO) {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return@withContext false

        try {
            // Cancel discovery to prevent slow RFCOMM negotiation
            try {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
            } catch (_: SecurityException) {}

            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(spec.mac)

            withTimeout(timeout) {
                var socket: BluetoothSocket? = null

                // Method 1: Standard Secure RFCOMM
                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    _socket = socket
                    socket.connect()
                } catch (_: Exception) {
                    try { socket?.close() } catch (_: Exception) {}
                    socket = null
                }

                // Method 2: Insecure RFCOMM (bypasses PIN / pairing quirks on budget printers)
                if (socket == null || !socket.isConnected) {
                    try {
                        socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                        _socket = socket
                        socket.connect()
                    } catch (_: Exception) {
                        try { socket?.close() } catch (_: Exception) {}
                        socket = null
                    }
                }

                // Method 3: Direct Channel 1 Reflection (fallback for budget thermal printers)
                if (socket == null || !socket.isConnected) {
                    try {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = method.invoke(device, 1) as BluetoothSocket
                        _socket = socket
                        socket.connect()
                    } catch (_: Exception) {
                        try { socket?.close() } catch (_: Exception) {}
                        socket = null
                    }
                }

                if (socket != null && socket.isConnected) {
                    _socket = socket
                    true
                } else {
                    false
                }
            }
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
    }

    private suspend fun asyncGetPaperStatus(
        input: InputStream,
        output: OutputStream,
        timeout: Duration = 2.seconds,
    ): EPPaperStatus = withTimeout(timeout) {
        output.write(EPStatusConstants.PAPER_SENSOR_STATUS)
        output.flush()

        delay(50)

        if (input.available() <= 0) {
            return@withTimeout EPPaperStatus.AVAILABLE
        }

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

        val isOutOfPaper = response.and(EPStatusConstants.PAPER_EMPTY_STATUS) != 0.toByte()
        if (isOutOfPaper) {
            return@withTimeout EPPaperStatus.EMPTY
        }

        val isLowOnPaper = response.and(EPStatusConstants.PAPER_LOW_STATUS) != 0.toByte()
        if (isLowOnPaper) {
            return@withTimeout EPPaperStatus.LOW
        }

        EPPaperStatus.AVAILABLE
    }

    private suspend fun asyncGetStatusOverview(
        input: InputStream,
        output: OutputStream,
        timeout: Duration = 2.seconds,
    ): EPPrinterStatus? = withTimeout(timeout) {
        output.write(EPStatusConstants.PRINTER_STATUS)
        output.flush()

        delay(50)

        if (input.available() <= 0) {
            return@withTimeout EPPrinterStatus(isOnline = true, isBusy = false)
        }

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

        val isOffline = response.and(EPStatusConstants.STATUS_CHECK_OFFLINE) != 0.toByte()
        val isBusy = response.and(EPStatusConstants.STATUS_CHECK_BUSY) != 0.toByte()

        EPPrinterStatus(isOnline = !isOffline, isBusy = isBusy)
    }

    private suspend fun asyncGetOfflineStatus(
        input: InputStream,
        output: OutputStream,
        timeout: Duration = 2.seconds,
    ): EPOfflineStatus? = withTimeout(timeout) {
        output.write(EPStatusConstants.OFFLINE_CAUSE_STATUS)
        output.flush()

        delay(50)

        if (input.available() <= 0) {
            return@withTimeout null
        }

        val buffer = ByteArray(1)
        input.read(buffer)

        val response = buffer.first()

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

    private suspend fun asyncWaitUntilReady(
        input: InputStream,
        output: OutputStream,
        timeout: Duration = 5.seconds,
    ): EPPaperStatus = withTimeout(timeout) {
        output.write(EPStatusConstants.PRINTER_STATUS_AWAIT)
        output.flush()

        val buffer = ByteArray(1)
        var bytesRead = -1
        while (bytesRead <= 0 && isActive) {
            if (input.available() > 0) {
                bytesRead = input.read(buffer)
                break
            }
            delay(50)
        }

        if (bytesRead <= 0) {
            return@withTimeout EPPaperStatus.AVAILABLE
        }

        val response = buffer.first()
        val isOutOfPaper = response.and(EPStatusConstants.PAPER_EMPTY_STATUS) != 0.toByte()
        if (isOutOfPaper) {
            return@withTimeout EPPaperStatus.EMPTY
        }

        val isLowOnPaper = response.and(EPStatusConstants.PAPER_LOW_STATUS) != 0.toByte()
        if (isLowOnPaper) {
            return@withTimeout EPPaperStatus.LOW
        }

        EPPaperStatus.AVAILABLE
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun send(
        command: ByteArray,
        timeout: Duration,
    ): EPPrintResult = withContext(Dispatchers.IO) {
        val socket = _socket
        if (socket == null || !socket.isConnected) return@withContext EPPrintResult.NotConnected

        try {
            val writer = socket.outputStream
            val reader = socket.inputStream

            // Direct byte transmission to thermal printer
            writer.write(command)
            writer.flush()

            when (waitType) {
                EPWaitType.RT -> {
                    // Real-time mode: probe with fast DLE EOT micro-timeouts (ignores GS r)
                    val status = withTimeoutOrNull(500.milliseconds) {
                        asyncGetStatusOverview(reader, writer, timeout = 300.milliseconds)
                    }

                    if (status != null && !status.isOnline) {
                        val offline = withTimeoutOrNull(300.milliseconds) {
                            asyncGetOfflineStatus(reader, writer, timeout = 200.milliseconds)
                        }
                        return@withContext EPPrintResult.Failed(offline ?: EPOfflineStatus.outOfPaper())
                    }

                    val paperStatus = withTimeoutOrNull(300.milliseconds) {
                        asyncGetPaperStatus(reader, writer, timeout = 200.milliseconds)
                    }

                    if (paperStatus == EPPaperStatus.EMPTY) {
                        return@withContext EPPrintResult.Failed(offlineStatus = EPOfflineStatus.outOfPaper())
                    }

                    EPPrintResult.Success(
                        status = status ?: EPPrinterStatus(isOnline = true, isBusy = false),
                        paperStatus = paperStatus ?: EPPaperStatus.AVAILABLE,
                    )
                }

                EPWaitType.WAIT -> {
                    // Synchronous wait: send GS r 1 to wait for buffer drain / paper status
                    val paperStatus = withTimeoutOrNull(timeout) {
                        asyncWaitUntilReady(reader, writer, timeout = timeout)
                    } ?: EPPaperStatus.AVAILABLE

                    if (paperStatus == EPPaperStatus.EMPTY) {
                        return@withContext EPPrintResult.Failed(offlineStatus = EPOfflineStatus.outOfPaper())
                    }

                    EPPrintResult.Success(
                        status = EPPrinterStatus(isOnline = true, isBusy = false),
                        paperStatus = paperStatus,
                    )
                }
            }
        } catch (e: Exception) {
            disconnect()
            EPPrintResult.NotConnected
        }
    }
}
