package dev.nag.domain

object Streak {

    fun of(completionDays: Set<Long>, today: Long): Int {
        var anchor = when {
            today in completionDays -> today
            today - 1 in completionDays -> today - 1
            else -> return 0
        }
        var count = 0
        while (anchor in completionDays) {
            count++
            anchor--
        }
        return count
    }
}
