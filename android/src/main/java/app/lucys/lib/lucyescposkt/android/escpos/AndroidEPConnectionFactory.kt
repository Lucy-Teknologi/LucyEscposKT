package app.lucys.lib.lucyescposkt.android.escpos

import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnection
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnectionFactory
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec

class AndroidEPConnectionFactory : EPConnectionFactory {
    override fun create(spec: PrinterConnectionSpec): EPConnection {
        return when (spec) {
            is PrinterConnectionSpec.Bluetooth -> BTManagerEPConnection(spec)
            is PrinterConnectionSpec.TCP -> KtorEPConnection(spec)
        }
    }
}