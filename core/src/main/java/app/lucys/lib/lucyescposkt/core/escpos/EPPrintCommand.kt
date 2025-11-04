package app.lucys.lib.lucyescposkt.core.escpos

import app.lucys.lib.lucyescposkt.core.escpos.constants.EPCutType
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintAlignment
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.CR
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.LF
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.START
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPAlignmentScope
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPBoldScope
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPTabScope
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPTallScope
import app.lucys.lib.lucyescposkt.core.escpos.scopes.EPWideScope

// TODO: Update, account for max character count when needed
class EPPrintCommandBuilder(val cpl: Int) {
    private val commands = mutableListOf<Byte>()

    fun raw(vararg bytes: Byte) {
        commands.addAll(bytes.toList())
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
            raw(*CR)
            raw(*LF)
        }
    }

    fun bold(setup: EPBoldScope.() -> Unit) {
        raw(*EPPrintConstants.BOLD_ON)
        EPBoldScope(this).setup()
        raw(*EPPrintConstants.BOLD_OFF)
    }

    fun wide(setup: EPWideScope.() -> Unit) {
        raw(*EPPrintConstants.WIDE_ON)
        EPWideScope(this).setup()
        raw(*EPPrintConstants.WIDE_OFF)
    }

    fun tall(setup: EPTallScope.() -> Unit) {
        raw(*EPPrintConstants.TALL_ON)
        EPTallScope(this).setup()
        raw(*EPPrintConstants.TALL_OFF)
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
        scope.resetTab()
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
        return commands.toByteArray()
    }
}

fun escpos(cpl: Int, setup: EPPrintCommandBuilder.() -> Unit): ByteArray {
    val builder = EPPrintCommandBuilder(cpl)
    builder.initialize()
    builder.setup()
    return builder.build()
}

