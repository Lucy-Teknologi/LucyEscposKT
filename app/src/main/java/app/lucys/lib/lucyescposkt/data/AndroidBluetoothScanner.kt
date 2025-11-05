package app.lucys.lib.lucyescposkt.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import app.lucys.lib.lucyescposkt.core.printer.PrinterModel
import java.util.UUID
import javax.inject.Inject

// Not really a scanner and just provides paired devices for simplicity
class AndroidBluetoothPrinterScanner @Inject constructor(
    private val context: Context,
) : BluetoothPrinterScanner {
    companion object {
        private val PRINTER_UUID: UUID
            get() = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Serial Port
    }

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun getPairedDevices(): List<PrinterModel> {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        ) {
            return emptyList()
        }

        if (
            !hasPermission(Manifest.permission.BLUETOOTH) &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            return emptyList()
        }

        val devices = BluetoothAdapter.getDefaultAdapter()
            .bondedDevices
            .map { device ->
                PrinterModel(
                    name = device.name,
                    characterCount = 32,
                    connectionSpec = PrinterConnectionSpec.Bluetooth(device.address),
                )
            }

        return devices
    }

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}