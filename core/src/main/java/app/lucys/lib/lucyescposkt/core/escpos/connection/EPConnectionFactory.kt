package app.lucys.lib.lucyescposkt.core.escpos.connection

import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec

interface EPConnectionFactory {
    fun create(spec: PrinterConnectionSpec): EPConnection
}
