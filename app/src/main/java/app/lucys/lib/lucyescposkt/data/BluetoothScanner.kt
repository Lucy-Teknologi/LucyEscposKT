package app.lucys.lib.lucyescposkt.data

import app.lucys.lib.lucyescposkt.core.printer.PrinterModel


interface BluetoothPrinterScanner {
    fun getPairedDevices(): List<PrinterModel>
}