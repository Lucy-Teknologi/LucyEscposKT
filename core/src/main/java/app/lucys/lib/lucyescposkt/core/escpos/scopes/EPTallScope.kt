package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder

class EPTallScope(private val builder: EPPrintCommandBuilder) {
    fun text(content: String) {
        builder.text(content)
    }
}