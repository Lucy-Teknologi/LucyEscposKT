package app.lucys.lib.lucyescposkt.core.escpos

sealed interface EPStreamData {
    data class Log(val value: String) : EPStreamData
    data class Result(val value: EPPrintResult) : EPStreamData
}