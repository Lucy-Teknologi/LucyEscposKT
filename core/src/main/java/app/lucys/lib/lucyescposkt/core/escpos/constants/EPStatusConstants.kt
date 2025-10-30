package app.lucys.lib.lucyescposkt.core.escpos.constants

object EPStatusConstants {
    val DLE_EOT = byteArrayOf(0x10, 0x04)
    val GS_R = byteArrayOf(0x1D, 0x72)

    val PRINTER_STATUS = DLE_EOT + byteArrayOf(0x01)
    val PRINTER_STATUS_AWAIT = GS_R + byteArrayOf(0x01)

    val OFFLINE_CAUSE_STATUS = DLE_EOT + byteArrayOf(0x02)
    val ERROR_CAUSE_STATS = DLE_EOT + byteArrayOf(0x03)
    val PAPER_SENSOR_STATUS = DLE_EOT + byteArrayOf(0x04)

    const val DEFAULT_RESPONSE_BYTE = 18.toByte() // 00010010b

    const val PAPER_LOW_STATUS = 0x0C.toByte()
    const val PAPER_EMPTY_STATUS = 0x60.toByte()

    const val STATUS_CHECK_OFFLINE = 0x08.toByte()
    const val STATUS_CHECK_BUSY = 0x20.toByte()

    const val OFFLINE_COVER_OPEN = 0x04.toByte()
    const val OFFLINE_PAPER_FEED = 0x08.toByte()
    const val OFFLINE_PAPER_OUT = 0x20.toByte()
    const val OFFLINE_UNKNOWN_ERROR = 0x40.toByte()
}
