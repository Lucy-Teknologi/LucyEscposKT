package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_BOLD
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_OFF
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_TALL
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_WIDE

class EPStyleScope(
    private val isBold: Boolean,
    private val isWide: Boolean,
    private val isTall: Boolean,
    private val builder: EPPrintCommandBuilder,
) {
    private val buffer = mutableListOf<String>()

    fun bold(setup: EPStyleScope.() -> Unit) {
        val scope = EPStyleScope(true, isWide, isTall, builder)
        scope.setup()
        scope.build()
    }

    fun wide(setup: EPStyleScope.() -> Unit) {
        val scope = EPStyleScope(isBold, true, isTall, builder)
        scope.setup()
        scope.build()
    }

    fun tall(setup: EPStyleScope.() -> Unit) {
        val scope = EPStyleScope(isBold, isWide, true, builder)
        scope.setup()
        scope.build()
    }

    fun text(string: String) {
        buffer.add(string)
    }

    fun build() {
        if (buffer.isEmpty()) {
            return
        }

        var styleByte = 0
        if (isBold) {
            styleByte = styleByte or STYLE_BOLD
        }
        if (isTall) {
            styleByte = styleByte or STYLE_TALL
        }
        if (isWide) {
            styleByte = styleByte or STYLE_WIDE
        }

        builder.raw(styleByte.toByte())
        for (text in buffer) {
            builder.text(text)
        }
        buffer.clear()
        builder.raw(*STYLE_OFF)
    }
}
