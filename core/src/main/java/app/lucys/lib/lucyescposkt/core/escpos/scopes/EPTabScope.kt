package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder
import app.lucys.lib.lucyescposkt.core.escpos.EPTabHorAlignment
import app.lucys.lib.lucyescposkt.core.escpos.EPTabPosition
import app.lucys.lib.lucyescposkt.core.escpos.constants.EPPrintConstants.ESC
import kotlin.math.floor
import kotlin.math.max

class EPTabScope(
    private val builder: EPPrintCommandBuilder,
    private val tab: EPTabPosition,
) {
    private var leftText: String = ""
    private var rightText: String = ""

    fun set(left: String, right: String) {
        leftText = left
        rightText = right
    }

    fun setLeft(string: String) {
        leftText = string
    }

    fun setRight(string: String) {
        rightText = string
    }

    private fun setTabPosition(pos: Int) {
        builder.raw(*ESC, 0x44, pos.toByte(), 0x00)
    }

    private fun moveToTab() {
        builder.raw(*byteArrayOf(0x09))
    }

    private fun fixed(value: Int, spacing: Int, alignment: EPTabHorAlignment) {
        val leftMaxLength = value - spacing
        val rightMaxLength = builder.cpl - value

        setTabPosition(value)

        val chunkedLeft = accumulateTexts(leftText.split(" "), leftMaxLength)
        val chunkedRight = accumulateTexts(rightText.split(" "), rightMaxLength)

        if (chunkedLeft.size == 1 && chunkedRight.size == 1) {
            builder.raw(*chunkedLeft.first().toByteArray())
            moveToTab()

            if (alignment == EPTabHorAlignment.RIGHT) {
                val padding = " ".repeat(rightMaxLength - chunkedRight.first().length)
                builder.raw(*padding.toByteArray())
            }

            builder.raw(*chunkedRight.first().toByteArray())
            builder.feed()
            return
        }

        val maxSize = max(chunkedLeft.size, chunkedRight.size)

        for (i in 0 until maxSize) {
            val left = chunkedLeft.getOrNull(i)
            val right = chunkedRight.getOrNull(i)

            if (left != null) {
                builder.raw(*left.toByteArray())
            }

            moveToTab()

            if (right != null) {
                val leftover = rightMaxLength - right.length

                // Indent text to make it look like it's aligned to the right
                if (leftover > 0 && alignment == EPTabHorAlignment.RIGHT) {
                    val padding = " ".repeat(leftover)
                    builder.raw(*padding.toByteArray())
                }

                builder.raw(*right.toByteArray())
            }

            builder.feed(1)
        }
    }

    private fun weighted(weight: Double, spacing: Int, alignment: EPTabHorAlignment) {
        val weightedCPL = floor(builder.cpl * weight).toInt()
        fixed(weightedCPL, spacing, alignment)
    }

    private tailrec fun accumulateTexts(
        texts: List<String>, // in split by space
        limit: Int,
        accumulator: List<String> = emptyList(), //in split by lines
    ): List<String> {
        if (texts.isEmpty()) {
            return accumulator
        }

        val string = StringBuilder()
        var counter = 0

        for (text in texts) {
            val length = string.length + text.length

            if (length > limit) {
                break
            }

            if (counter > 0 && length + 1 <= limit) {
                string.append(" ")
            }

            string.append(text)
            counter += 1
        }

        return accumulateTexts(
            texts = texts.drop(counter),
            limit = limit,
            accumulator = accumulator + string.trim().toString(),
        )
    }

    /**
     * Process the current text to the command buffer then reset the
     * texts to empty.
     */
    fun flush() {
        if (leftText.isEmpty() && rightText.isEmpty()) {
            return
        }

        if (leftText.isEmpty() || rightText.isEmpty()) {
            if (leftText.isNotEmpty()) builder.left { text(leftText) }
            if (rightText.isNotEmpty()) builder.right { text(rightText) }
            return
        }

        when (tab) {
            is EPTabPosition.Fixed -> fixed(tab.value, tab.spacing, tab.alignment)
            is EPTabPosition.Weighted -> weighted(tab.weight, tab.spacing, tab.alignment)
        }

        this.leftText = ""
        this.rightText = ""
    }
}