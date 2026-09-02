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
    private val rows = mutableListOf<Pair<String, String>>()
    private var pendingLeft: String = ""
    private var pendingRight: String = ""

    fun set(left: String, right: String) {
        commitPending()
        rows.add(left to right)
    }

    fun setLeft(string: String) {
        pendingLeft = string
    }

    fun setRight(string: String) {
        pendingRight = string
    }

    private fun commitPending() {
        if (pendingLeft.isNotEmpty() || pendingRight.isNotEmpty()) {
            rows.add(pendingLeft to pendingRight)
            pendingLeft = ""
            pendingRight = ""
        }
    }

    private fun setTabPosition(pos: Int) {
        builder.raw(*ESC, 0x44, pos.toByte(), 0x00)
    }

    private fun moveToTab() {
        builder.raw(*byteArrayOf(0x09))
    }

    private fun fixed(leftText: String, rightText: String, value: Int, spacing: Int, alignment: EPTabHorAlignment) {
        val leftMaxLength = maxOf(1, value - spacing)
        val rightMaxLength = maxOf(1, builder.cpl - value)

        setTabPosition(value)

        val chunkedLeft = accumulateTexts(leftText.split(" "), leftMaxLength)
        val chunkedRight = accumulateTexts(rightText.split(" "), rightMaxLength)

        if (chunkedLeft.size == 1 && chunkedRight.size == 1) {
            builder.raw(*chunkedLeft.first().toByteArray())
            moveToTab()

            if (alignment == EPTabHorAlignment.RIGHT) {
                val paddingLength = maxOf(0, rightMaxLength - chunkedRight.first().length)
                if (paddingLength > 0) {
                    val padding = " ".repeat(paddingLength)
                    builder.raw(*padding.toByteArray())
                }
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
                val leftover = maxOf(0, rightMaxLength - right.length)

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

    private fun weighted(leftText: String, rightText: String, weight: Double, spacing: Int, alignment: EPTabHorAlignment) {
        val weightedCPL = floor(builder.cpl * weight).toInt()
        fixed(leftText, rightText, weightedCPL, spacing, alignment)
    }

    private tailrec fun accumulateTexts(
        texts: List<String>, // in split by space
        limit: Int,
        accumulator: List<String> = emptyList(), //in split by lines
    ): List<String> {
        if (texts.isEmpty()) {
            return accumulator
        }

        val effectiveLimit = maxOf(1, limit)
        val firstText = texts.first()

        // If the first word itself exceeds limit, chunk it so we make guaranteed progress
        if (firstText.length > effectiveLimit) {
            val chunk = firstText.take(effectiveLimit)
            val remainder = firstText.substring(effectiveLimit)
            val nextTexts = listOf(remainder) + texts.drop(1)
            return accumulateTexts(
                texts = nextTexts,
                limit = limit,
                accumulator = accumulator + chunk,
            )
        }

        val string = StringBuilder()
        var counter = 0

        for (text in texts) {
            val additionalLength = if (counter > 0) text.length + 1 else text.length
            if (string.length + additionalLength > effectiveLimit) {
                break
            }

            if (counter > 0) {
                string.append(" ")
            }

            string.append(text)
            counter += 1
        }

        return accumulateTexts(
            texts = texts.drop(maxOf(1, counter)),
            limit = limit,
            accumulator = accumulator + string.trim().toString(),
        )
    }

    private fun renderRow(leftText: String, rightText: String) {
        if (leftText.isEmpty() && rightText.isEmpty()) {
            return
        }

        if (leftText.isEmpty() || rightText.isEmpty()) {
            if (leftText.isNotEmpty()) builder.left { text(leftText) }
            if (rightText.isNotEmpty()) builder.right { text(rightText) }
            return
        }

        when (tab) {
            is EPTabPosition.Fixed -> fixed(leftText, rightText, tab.value, tab.spacing, tab.alignment)
            is EPTabPosition.Weighted -> weighted(leftText, rightText, tab.weight, tab.spacing, tab.alignment)
        }
    }

    /**
     * Process all accumulated rows to the command buffer then reset the queue.
     */
    fun flush() {
        commitPending()
        if (rows.isEmpty()) {
            return
        }

        for ((left, right) in rows) {
            renderRow(left, right)
        }

        rows.clear()
    }
}