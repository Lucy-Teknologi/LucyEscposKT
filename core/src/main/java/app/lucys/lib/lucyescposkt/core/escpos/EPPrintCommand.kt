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

    fun bold(setup: EPStyleScope.() -> Unit) {
        val scope = EPStyleScope(isBold = true, isWide = false, isTall = false, builder = this)
        scope.setup()
        scope.build()
    }

    fun wide(setup: EPStyleScope.() -> Unit) {
        val scope = EPStyleScope(isBold = false, isWide = true, isTall = false, builder = this)
        scope.setup()
        scope.build()
    }

    fun tall(setup: EPStyleScope.() -> Unit) {
        val scope = EPStyleScope(isBold = false, isWide = false, isTall = true, builder = this)
        scope.setup()
        scope.build()
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
        return commands.toByteArray()
    }
}

fun escpos(cpl: Int, setup: EPPrintCommandBuilder.() -> Unit): ByteArray {
    val builder = EPPrintCommandBuilder(cpl)
    builder.initialize()
    builder.setup()
    return builder.build()
}

