package app.lucys.lib.lucyescposkt.core.printer

sealed interface PrinterConnectionSpec {
    data class TCP(val ip: String, val port: String): PrinterConnectionSpec
    data class Bluetooth(val mac: String): PrinterConnectionSpec
}
