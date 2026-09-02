package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_BOLD
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_ON
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_TALL
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.STYLE_WIDE

class EPStyleScope(
    private val currentStyle: Int,
    private val builder: EPPrintCommandBuilder,
) {
    fun bold(setup: EPStyleScope.() -> Unit) {
        enterStyle(currentStyle or STYLE_BOLD, setup)
    }

    fun wide(setup: EPStyleScope.() -> Unit) {
        enterStyle(currentStyle or STYLE_WIDE, setup)
    }

    fun tall(setup: EPStyleScope.() -> Unit) {
        enterStyle(currentStyle or STYLE_TALL, setup)
    }

    fun text(string: String) {
        builder.text(string)
    }

    fun raw(vararg bytes: Byte) {
        builder.raw(*bytes)
    }

    fun feed(lines: Int = 1) {
        builder.feed(lines)
    }

    private fun enterStyle(styleByte: Int, setup: EPStyleScope.() -> Unit) {
        builder.raw(*STYLE_ON, styleByte.toByte())
        val nestedScope = EPStyleScope(currentStyle = styleByte, builder = builder)
        nestedScope.setup()
        builder.raw(*STYLE_ON, currentStyle.toByte())
    }
}
