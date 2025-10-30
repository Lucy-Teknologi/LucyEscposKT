package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder

class EPBoldScope(private val builder: EPPrintCommandBuilder) {
    fun text(content: String) {
        builder.text(content)
    }
}
