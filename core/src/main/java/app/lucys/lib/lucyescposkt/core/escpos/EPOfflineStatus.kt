package app.lucys.lib.lucyescposkt.core.escpos

data class EPOfflineStatus(
    val isCoverOpen: Boolean,
    val isFeedPressed: Boolean,
    val isOutOfPaper: Boolean,
    val didErrorOccur: Boolean,
) {
    companion object {
        fun outOfPaper() = EPOfflineStatus(
            isCoverOpen = false,
            isFeedPressed = false,
            isOutOfPaper = true,
            didErrorOccur = false
        )
    }
}
