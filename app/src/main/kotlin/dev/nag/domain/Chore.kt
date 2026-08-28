package dev.nag.domain

data class Chore(
    val id: Long,
    val name: String,
    val cadenceDays: Int,
    val nextDueDay: Long,
    val creationOrder: Long,
) {

    fun isDue(today: Long): Boolean = today >= nextDueDay

    fun completedOn(completionDay: Long): Chore = copy(nextDueDay = completionDay + cadenceDays)

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
