package app.lucys.lib.lucyescposkt.core.escpos

sealed interface EPTabPosition {
    /**
     * Place the tab at a fixed value
     */
    data class Fixed(
        val value: Int,
        val spacing: Int = 2,
        val alignment: EPTabHorAlignment = EPTabHorAlignment.RIGHT,
    ) : EPTabPosition

    /**
     * Place the tab at printer max column * weight rounded down
     */
    data class Weighted(
        val weight: Double,
        val spacing: Int = 2,
        val alignment: EPTabHorAlignment = EPTabHorAlignment.RIGHT,
    ) : EPTabPosition

    // TODO: MOVE IMPLEMENTATION HERE, USE THIS LIKE A PLUGGABLE STRATEGY PATTERN
}

enum class EPTabHorAlignment {
    RIGHT, LEFT;
}