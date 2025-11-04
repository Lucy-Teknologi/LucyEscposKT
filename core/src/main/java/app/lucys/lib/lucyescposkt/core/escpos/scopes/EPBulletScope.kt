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

        val string = StringBuilder()
        var counter = 0

        for (text in texts) {
            if (string.length + text.length + 1 > limit) {
                break
            }

            if (counter > 0) {
                string.append(" ")
            }

            string.append(text)
            counter += 1
        }

        return accumulateTexts(
            texts = texts.drop(counter),
            limit = limit,
            accumulator = accumulator + string.toString(),
        )
    }

    internal fun build() {
        for (item in items) {
            val prepend = " ".repeat(indent).toByteArray()

            builder.raw(*prepend)
            builder.raw(*symbol.toByteArray())

            val indentation = indent + spacing + 1
            val limit = builder.cpl - indentation

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
