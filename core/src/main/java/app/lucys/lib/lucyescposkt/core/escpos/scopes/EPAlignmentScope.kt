package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPPrintCommandBuilder

class EPAlignmentScope(private val builder: EPPrintCommandBuilder) {
    fun text(content: String) {
        builder.text(content)
    }
}
