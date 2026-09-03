package app.lucys.lib.lucyescposkt.android

import app.lucys.lib.lucyescposkt.android.escpos.KtorEPConnection
import app.lucys.lib.lucyescposkt.core.escpos.EPPrintResult
import app.lucys.lib.lucyescposkt.core.escpos.command.EPWaitType
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import kotlin.time.Duration.Companion.seconds

class KtorEPConnectionTest {
    @Test
    fun testConsecutiveSendsOnWaitMode() = runBlocking {
        val server = ServerSocket(0)
        val port = server.localPort

        val serverJob = launch(Dispatchers.IO) {
            val clientSocket = server.accept()
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            val buffer = ByteArray(1024)
            while (!clientSocket.isClosed) {
                val read = input.read(buffer)
                if (read == -1) break
                for (i in 0 until read - 2) {
                    if (buffer[i] == 0x1D.toByte() && buffer[i + 1] == 0x72.toByte() && buffer[i + 2] == 0x01.toByte()) {
                        output.write(byteArrayOf(0x00))
                        output.flush()
                    }
                }
            }
            clientSocket.close()
            server.close()
        }

        val conn = KtorEPConnection(
            spec = PrinterConnectionSpec.TCP("127.0.0.1", port.toString()),
            waitType = EPWaitType.WAIT,
        )

        val connected = conn.connect(5.seconds)
        assertTrue("Should connect", connected)

        for (i in 1..10) {
            val res = conn.send("Job $i".toByteArray(), 5.seconds)
            assertTrue("Job $i should succeed: $res", res is EPPrintResult.Success)
        }

        conn.disconnect()
        serverJob.join()
    }

    @Test
    fun testConsecutiveSendsOnRtMode() = runBlocking {
        val server = ServerSocket(0)
        val port = server.localPort

        val serverJob = launch(Dispatchers.IO) {
            val clientSocket = server.accept()
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            val buffer = ByteArray(1024)
            while (!clientSocket.isClosed) {
                val read = input.read(buffer)
                if (read == -1) break
                // DLE EOT 1: 0x10 0x04 0x01 -> reply 0x12 (online, ready)
                for (i in 0 until read - 2) {
                    if (buffer[i] == 0x10.toByte() && buffer[i + 1] == 0x04.toByte()) {
                        output.write(byteArrayOf(0x12))
                        output.flush()
                    }
                }
            }
            clientSocket.close()
            server.close()
        }

        val conn = KtorEPConnection(
            spec = PrinterConnectionSpec.TCP("127.0.0.1", port.toString()),
            waitType = EPWaitType.RT,
        )

        val connected = conn.connect(5.seconds)
        assertTrue("Should connect", connected)

        for (i in 1..10) {
            val res = conn.send("Job $i".toByteArray(), 5.seconds)
            println("RT Result $i: $res")
            assertTrue("Job $i should succeed: $res", res is EPPrintResult.Success)
        }

        conn.disconnect()
        serverJob.join()
    }
}
