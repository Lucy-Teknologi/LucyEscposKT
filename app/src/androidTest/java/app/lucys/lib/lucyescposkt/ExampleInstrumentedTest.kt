package app.lucys.lib.lucyescposkt

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.lucys.lib.lucyescposkt.android.escpos.AndroidEPConnectionFactory
import app.lucys.lib.lucyescposkt.core.escpos.EPPrintResult
import app.lucys.lib.lucyescposkt.core.escpos.command.EPWaitType
import app.lucys.lib.lucyescposkt.core.escpos.escpos
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private val tag = "BT_BENCHMARK"

    @Test
    fun benchmarkBluetoothPrinting() = runBlocking {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Log.e(tag, "Bluetooth is disabled or not available!")
            return@runBlocking
        }

        val pairedDevices = adapter.bondedDevices.toList()
        Log.i(tag, "Found ${pairedDevices.size} paired devices.")
        for (d in pairedDevices) {
            Log.i(tag, "  Device: ${d.name} (${d.address})")
        }

        val targetDevice = pairedDevices.firstOrNull { d ->
            d.address.equals("66:32:34:6B:06:1C", ignoreCase = true) ||
            d.name?.contains("RPP", ignoreCase = true) == true ||
            d.name?.contains("print", ignoreCase = true) == true
        }

        if (targetDevice == null) {
            Log.e(tag, "No target printer found in paired devices!")
            return@runBlocking
        }

        Log.i(tag, "Targeting printer: ${targetDevice.name} [${targetDevice.address}]")

        val spec = PrinterConnectionSpec.Bluetooth(targetDevice.address)
        val factory = AndroidEPConnectionFactory()

        val command = escpos(32) {
            center {
                bold { text("BENCHMARK TEST") }
                text("Bluetooth Print Test")
            }
            text("Time: " + System.currentTimeMillis())
            feedAndCut()
        }

        Log.i(tag, "Payload size: ${command.size} bytes")

        // -------------------------------------------------------------
        // BENCHMARK 1: Cold Connection + Send in RT Mode
        // -------------------------------------------------------------
        Log.i(tag, "=== BENCHMARK 1: Cold Connect + Send (RT Mode) ===")
        val connectionRt = factory.create(spec, wait = EPWaitType.RT)

        var connectTimeRt = 0L
        var connectedRt = false
        connectTimeRt = measureTimeMillis {
            connectedRt = connectionRt.connect(5.seconds)
        }
        Log.i(tag, "Cold Connect RT: success=$connectedRt in ${connectTimeRt}ms")

        if (connectedRt) {
            var sendTimeRt = 0L
            var resultRt: EPPrintResult
            sendTimeRt = measureTimeMillis {
                resultRt = connectionRt.send(command, 10.seconds)
            }
            Log.i(tag, "Send RT (Cold Socket): in ${sendTimeRt}ms")
            Log.i(tag, "TOTAL Cold Print RT Time: ${connectTimeRt + sendTimeRt}ms")

            // -------------------------------------------------------------
            // BENCHMARK 2: Warm Connection Send in RT Mode (Consecutive Print)
            // -------------------------------------------------------------
            Log.i(tag, "=== BENCHMARK 2: Warm Send (RT Mode, Socket Kept Open) ===")
            var warmSendTime = 0L
            var warmResult: EPPrintResult
            warmSendTime = measureTimeMillis {
                warmResult = connectionRt.send(command, 10.seconds)
            }
            Log.i(tag, "Send RT (Warm Socket): in ${warmSendTime}ms")
            Log.i(tag, "TOTAL Warm Print RT Time: ${warmSendTime}ms")

            connectionRt.disconnect()
            Log.i(tag, "Disconnected RT connection.")
        }

        // -------------------------------------------------------------
        // BENCHMARK 3: Cold Connection + Send in WAIT Mode (GS r 1 status check)
        // -------------------------------------------------------------
        Log.i(tag, "=== BENCHMARK 3: Cold Connect + Send (WAIT Mode) ===")
        val connectionWait = factory.create(spec, wait = EPWaitType.WAIT)

        var connectTimeWait = 0L
        var connectedWait = false
        connectTimeWait = measureTimeMillis {
            connectedWait = connectionWait.connect(5.seconds)
        }
        Log.i(tag, "Cold Connect WAIT: success=$connectedWait in ${connectTimeWait}ms")

        if (connectedWait) {
            var sendTimeWait = 0L
            var resultWait: EPPrintResult
            sendTimeWait = measureTimeMillis {
                resultWait = connectionWait.send(command, 10.seconds)
            }
            Log.i(tag, "Send WAIT: in ${sendTimeWait}ms")
            Log.i(tag, "TOTAL Cold Print WAIT Time: ${connectTimeWait + sendTimeWait}ms")

            connectionWait.disconnect()
            Log.i(tag, "Disconnected WAIT connection.")
        }
    }
}