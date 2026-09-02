package app.lucys.lib.lucyescposkt.core.escpos

import app.lucys.lib.lucyescposkt.core.escpos.constants.EPCutType
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintAlignment
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.CR
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.LF
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.START
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPAlignmentScope
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPBulletScope
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPStyleScope
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPTabScope
import java.io.ByteArrayOutputStream

// TODO: Update, account for max character count when needed
class EPPrintCommandBuilder(val cpl: Int) {
    private val buffer = ByteArrayOutputStream()

    fun raw(vararg bytes: Byte) {
        buffer.write(bytes)
    }

    fun initialize() {
        raw(*START)
    }

    fun text(content: String) {
        raw(*content.toByteArray(Charsets.UTF_8))
        feed() // Always include a line feed after text content
    }

    fun feed(lines: Int = 1) {
        repeat(lines) {
            raw(*LF)
        }
    }

    fun bold(setup: EPStyleScope.() -> Unit) {
        raw(*EPPrintConstants.STYLE_ON, EPPrintConstants.STYLE_BOLD.toByte())
        val scope = EPStyleScope(currentStyle = EPPrintConstants.STYLE_BOLD, builder = this)
        scope.setup()
        raw(*EPPrintConstants.STYLE_OFF)
    }

    fun wide(setup: EPStyleScope.() -> Unit) {
        raw(*EPPrintConstants.STYLE_ON, EPPrintConstants.STYLE_WIDE.toByte())
        val scope = EPStyleScope(currentStyle = EPPrintConstants.STYLE_WIDE, builder = this)
        scope.setup()
        raw(*EPPrintConstants.STYLE_OFF)
    }

    fun tall(setup: EPStyleScope.() -> Unit) {
        raw(*EPPrintConstants.STYLE_ON, EPPrintConstants.STYLE_TALL.toByte())
        val scope = EPStyleScope(currentStyle = EPPrintConstants.STYLE_TALL, builder = this)
        scope.setup()
        raw(*EPPrintConstants.STYLE_OFF)
    }

    fun align(alignment: EPPrintAlignment, setup: EPAlignmentScope.() -> Unit) {
        when (alignment) {
            EPPrintAlignment.LEFT -> raw(*EPPrintConstants.ALIGN_LEFT)
            EPPrintAlignment.CENTER -> raw(*EPPrintConstants.ALIGN_CENTER)
            EPPrintAlignment.RIGHT -> raw(*EPPrintConstants.ALIGN_RIGHT)
        }

        EPAlignmentScope(this).setup()

        if (alignment != EPPrintAlignment.LEFT) {
            raw(*EPPrintConstants.ALIGN_LEFT)
        }
    }

    fun left(setup: EPAlignmentScope.() -> Unit) {
        align(EPPrintAlignment.LEFT, setup)
    }

    fun center(setup: EPAlignmentScope.() -> Unit) {
        align(EPPrintAlignment.CENTER, setup)
    }

    fun right(setup: EPAlignmentScope.() -> Unit) {
        align(EPPrintAlignment.RIGHT, setup)
    }

    fun tab(position: EPTabPosition, setup: EPTabScope.() -> Unit) {
        val scope = EPTabScope(this, position)
        scope.setup()
        scope.flush()
    }

    fun bullet(symbol: String, indent: Int = 2, spacing: Int = 1, setup: EPBulletScope.() -> Unit) {
        val scope = EPBulletScope(indent, spacing, symbol, this)
        scope.setup()
        scope.build()
    }

    fun cut(type: EPCutType = EPCutType.FULL) {
        raw(*type.bytes())
    }

    fun feedAndCut(lines: Int = 4, type: EPCutType = EPCutType.FULL) {
        when (type) {
            EPCutType.FULL_FEED -> {
                cut(type)
                raw(lines.toByte())
            }

            EPCutType.PARTIAL_FEED -> {
                cut(type)
                raw(lines.toByte())
            }

            else -> {
                feed(lines)
                cut(type)
            }
        }
    }

    fun build(): ByteArray {
        return buffer.toByteArray()
    }
}

fun escpos(cpl: Int, setup: EPPrintCommandBuilder.() -> Unit): ByteArray {
    val builder = EPPrintCommandBuilder(cpl)
    builder.initialize()
    builder.setup()
    return builder.build()
}

