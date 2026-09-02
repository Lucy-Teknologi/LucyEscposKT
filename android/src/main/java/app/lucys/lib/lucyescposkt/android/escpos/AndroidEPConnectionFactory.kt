package app.lucys.lib.lucyescposkt.android.escpos

import app.lucys.lib.lucyescposkt.core.escpos.command.EPWaitType
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnection
import app.lucys.lib.lucyescposkt.core.escpos.connection.EPConnectionFactory
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec

class AndroidEPConnectionFactory : EPConnectionFactory {
    override fun create(
        spec: PrinterConnectionSpec,
        wait: EPWaitType,
    ): EPConnection {
        return when (spec) {
            is PrinterConnectionSpec.Bluetooth -> BTManagerEPConnection(spec, waitType = wait)
            is PrinterConnectionSpec.TCP -> KtorEPConnection(spec, waitType = wait)
        }
    }
}