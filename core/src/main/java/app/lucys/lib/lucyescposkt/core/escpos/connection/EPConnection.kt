package app.lucys.lib.lucyescposkt.core.escpos.connection

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintResult
import app.lucys.lib.lucyescposkt.core.printer.PrinterConnectionSpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface EPConnection {
    val spec: PrinterConnectionSpec

    suspend fun isConnected(): Boolean

    suspend fun connect(timeout: Duration = 3.seconds): Boolean
    suspend fun disconnect()


    suspend fun send(command: ByteArray, timeout: Duration = 5.seconds): EPPrintResult
}
