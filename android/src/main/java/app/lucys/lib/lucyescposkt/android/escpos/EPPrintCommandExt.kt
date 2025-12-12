package app.lucys.lib.lucyescposkt.android.escpos

import android.graphics.Bitmap
import androidx.core.graphics.get
import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.RASTER_IMAGE
import java.io.ByteArrayOutputStream

fun EPPrintCommandBuilder.image(bitmap: Bitmap) {
    raw(*convertBitmapToRasterBytes(bitmap))
    feed(1)
}

fun EPPrintCommandBuilder.imageWithLabel(bitmap: Bitmap, label: String) {
    raw(*convertBitmapToRasterBytes(bitmap))
    raw(*label.toByteArray())
    feed(1)
}

private fun EPPrintCommandBuilder.convertBitmapToRasterBytes(bitmap: Bitmap): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    val bytesPerRow = (width + 7) / 8

    val imageBytes = ByteArrayOutputStream()

    // ESC/POS raster bit image command header
    // 0x1D 0x76 0x30 0x00 nL nH mL mH
    imageBytes.write(RASTER_IMAGE)

    val nL = (bytesPerRow and 0xFF).toByte()
    val nH = ((bytesPerRow shr 8) and 0xFF).toByte()
    val mL = (height and 0xFF).toByte()
    val mH = ((height shr 8) and 0xFF).toByte()

    imageBytes.write(byteArrayOf(nL, nH, mL, mH))

    // Process each row
    // Grayscale by luminescence
    for (y in 0 until height) {
        for (xByte in 0 until bytesPerRow) {
            var byte = 0
            for (bit in 0..7) {
                val x = xByte * 8 + bit
                if (x < width) {
                    val pixel = bitmap[x, y]
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
                    if (luminance < 128) {
                        byte = byte or (1 shl (7 - bit))
                    }
                }
            }
            imageBytes.write(byte)
        }
    }

    return imageBytes.toByteArray()
}
