package app.lucys.lib.lucyescposkt.core.escpos.constants

object EPPrintConstants {
    val ESC = byteArrayOf(0x1b)
    val GS = byteArrayOf(0x1d)
    val LF = byteArrayOf(0x0a)
    val CR = byteArrayOf(0x0d)

    val START = ESC + byteArrayOf(0x40)

    val CUT_A0 = GS + byteArrayOf(0x56, 0x00)
    val CUT_A1 = GS + byteArrayOf(0x56, 0x01)

    val CUT_B0 = GS + byteArrayOf(0x65)
    val CUT_B1 = GS + byteArrayOf(0x66)

    val ALIGN_LEFT = ESC + byteArrayOf(0x61, 0x00)
    val ALIGN_CENTER = ESC + byteArrayOf(0x61, 0x01)
    val ALIGN_RIGHT = ESC + byteArrayOf(0x61, 0x02)

    val BOLD_ON = ESC + byteArrayOf(0x21, 0x08)
    val BOLD_OFF = ESC + byteArrayOf(0x21, 0x00)

    val TALL_ON = ESC + byteArrayOf(0x21, 0x10)
    val TALL_OFF = ESC + byteArrayOf(0x21, 0x00)

    val WIDE_ON = ESC + byteArrayOf(0x21, 0x20)
    val WIDE_OFF = ESC + byteArrayOf(0x21, 0x00)

    val RASTER_IMAGE = GS + byteArrayOf(0x76, 0x30, 0x00)
}
