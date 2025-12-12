package app.lucys.lib.lucyescposkt.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowConversionToBitmap
import coil3.request.allowHardware
import coil3.toBitmap

class CoilImageLoader(private val context: Context) {
    suspend fun load(@DrawableRes res: Int): Bitmap? {
        val request = ImageRequest
            .Builder(context)
            .data(res)
            .allowConversionToBitmap(true)
            .allowHardware(false)
            .size(width = 32, height = 32)
            .build()

        val loader = ImageLoader
            .Builder(context)
            .build()

        val bitmap = loader
            .execute(request)
            .image
            ?.toBitmap(width = 32, height = 32, Bitmap.Config.ARGB_8888)
            ?.removeAlpha()

        return bitmap
    }

    suspend fun load(url: String): Bitmap? {
        val request =
            ImageRequest
                .Builder(context)
                .data(url)
                .allowConversionToBitmap(true)
                .allowHardware(false)
                .size(width = 128, height = 128)
                .build()

        val loader =
            ImageLoader
                .Builder(context)
                .build()

        val bitmap =
            loader
                .execute(request)
                .image
                ?.toBitmap(128, 128, Bitmap.Config.ARGB_8888)
                ?.removeAlpha()

        return bitmap
    }


    private fun removeAlpha(
        @ColorInt color: Int,
    ): Int {
        val a = Color.alpha(color)
        if (a == 255) return color // opaque already

        // Composite over white background
        val r = (Color.red(color) * a + 255 * (255 - a)) / 255
        val g = (Color.green(color) * a + 255 * (255 - a)) / 255
        val b = (Color.blue(color) * a + 255 * (255 - a)) / 255

        return Color.argb(255, r, g, b)
    }

    private fun Bitmap.removeAlpha(): Bitmap = mapPixels { px -> removeAlpha(px) }

    private fun Bitmap.mapPixels(transform: (Int) -> Int): Bitmap {
        val width = this.width
        val height = this.height
        val bitmap = createBitmap(width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = this[x, y]
                val newPixel = transform(pixel)
                bitmap[x, y] = newPixel
            }
        }

        return bitmap
    }
}