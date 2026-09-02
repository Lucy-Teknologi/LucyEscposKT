package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder

class EPBulletScope(
    private val indent: Int,
    private val spacing: Int,
    private val symbol: String,
    private val builder: EPPrintCommandBuilder,
) {
    private val items = mutableListOf<String>()

    fun text(content: String) {
        items.add(content)
    }

    private fun processText(text: List<String>, indent: Int) {
        val prepend = " ".repeat(indent).toByteArray()

        for (value in text) {
            builder.raw(*prepend)
            builder.text(value)
        }
    }

    internal tailrec fun accumulateTexts(
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
            accumulator = accumulator + string.toString(),
        )
    }

    internal fun build() {
        for (item in items) {
            val prepend = " ".repeat(indent).toByteArray()

            builder.raw(*prepend)
            builder.raw(*symbol.toByteArray())

            val spaceAppend = " ".repeat(spacing).toByteArray()
            builder.raw(*spaceAppend)

            val indentation = indent + spacing + symbol.length
            val limit = maxOf(1, builder.cpl - indentation)

            if (item.length <= limit) {
                builder.text(item)
                continue
            }

            val texts = accumulateTexts(item.split(" "), limit)
            val first = texts.first()
            val rest = texts.drop(1)

            builder.text(first)
            processText(rest, indentation)
        }
    }
}
