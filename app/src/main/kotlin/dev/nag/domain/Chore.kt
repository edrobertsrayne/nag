package dev.nag.domain

data class Chore(
    val id: Long,
    val name: String,
    val cadenceDays: Int,
    val nextDueDay: Long,
    val creationOrder: Long,
    val lastDiscardedDay: Long = 0,
    val archived: Boolean = false,
) {

    fun isDue(today: Long): Boolean = today >= nextDueDay

    fun completedOn(completionDay: Long): Chore = copy(nextDueDay = completionDay + cadenceDays)

    fun discardedOn(day: Long): Chore = copy(lastDiscardedDay = day)

    fun isHiddenOn(day: Long): Boolean = lastDiscardedDay == day

    /**
     * Edits the chore's name and cadence. Next-due is untouched; the new
     * cadence applies from the chore's next completion.
     */
    fun editedTo(name: String, cadenceDays: Int): Chore =
        copy(name = name, cadenceDays = cadenceDays)

    fun archivedOn(): Chore = copy(archived = true)

    companion object {

        fun create(name: String, cadenceDays: Int, today: Long, creationOrder: Long = 0): Chore = Chore(
            id = 0,
            name = name,
            cadenceDays = cadenceDays,
            nextDueDay = today,
            creationOrder = creationOrder,
        )
    }
}
