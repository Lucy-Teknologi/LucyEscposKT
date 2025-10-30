package app.lucys.lib.lucyescposkt.core.escpos

sealed interface EPPrintResult {
    data class Success(val status: EPPrinterStatus, val paperStatus: EPPaperStatus) : EPPrintResult
    data class Timeout(val status: EPPrinterStatus) : EPPrintResult
    data class Failed(val offlineStatus: EPOfflineStatus) : EPPrintResult
    data object NotConnected : EPPrintResult
}
