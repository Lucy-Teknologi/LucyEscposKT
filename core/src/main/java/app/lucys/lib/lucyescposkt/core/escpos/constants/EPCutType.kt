package app.lucys.lib.lucyescposkt.core.escpos.constants

enum class EPCutType {
    FULL,
    PARTIAL,
    FULL_FEED,
    PARTIAL_FEED;

    fun bytes() = when (this) {
        FULL -> EPPrintConstants.CUT_A0
        PARTIAL -> EPPrintConstants.CUT_A1
        FULL_FEED -> EPPrintConstants.CUT_B0
        PARTIAL_FEED -> EPPrintConstants.CUT_B1
    }
}