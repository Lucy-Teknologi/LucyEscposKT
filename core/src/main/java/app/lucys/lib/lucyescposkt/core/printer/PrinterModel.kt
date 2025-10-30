package app.lucys.lib.lucyescposkt.core.printer

data class PrinterModel(
    val name: String,
    val connectionSpec: PrinterConnectionSpec,
    val characterCount: Int,
)
