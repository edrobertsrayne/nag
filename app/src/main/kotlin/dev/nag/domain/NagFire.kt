package dev.nag.domain

/**
 * The nag to post when an alarm fires: its escalation level and its copy
 * line. Everything about whether to post at all lives in [NagFire.at] —
 * the receiver asks once and either posts or stays quiet.
 */
object NagFire {

    data class Post(val level: NagLevel, val text: String)

    /**
     * What the alarm should post when the clock reads this moment, or null
     * to stay quiet and just reschedule. A post happens only when the minute
     * falls inside one of today's slot windows — the slot minute plus the
     * late-delivery grace — which already sits outside the quiet hours and
     * the weekday silent window; a fire that arrived later is a missed slot
     * and never posts. A completion today quiets the rest of the day
     * (re-derived from state, never persisted); nothing due quiets too.
     */
    fun at(
        chores: List<Chore>,
        completionDays: Set<Long>,
        epochDay: Long,
        minuteOfDay: Int,
    ): Post? {
        val slot = NagSchedule.slotWithinGrace(epochDay, minuteOfDay) ?: return null
        if (epochDay in completionDays) return null
        val content = Nag.content(chores, completionDays, epochDay)
        if (content.dueCount == 0) return null
        return Post(level = slot.level, text = NagCopy.text(content, slot.level))
    }
}
