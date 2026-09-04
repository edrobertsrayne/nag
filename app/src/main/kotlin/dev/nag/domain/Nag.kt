package dev.nag.domain

/**
 * What a nag says about the deck: the first card's title, how many chores are
 * due, and the current streak.
 */
data class NagContent(
    val firstCardTitle: String?,
    val dueCount: Int,
    val streak: Int,
)

object Nag {

    /**
     * Nag content from the same state the deck uses. The due count includes
     * discarded-but-due chores: putting a card off doesn't take it off the
     * nag's books. When every due card is discarded, the title falls back to
     * the leading due chore in deck order so the nag still names what waits.
     */
    fun content(chores: List<Chore>, completionDays: Set<Long>, today: Long): NagContent {
        val due = chores.filter { !it.archived && it.isDue(today) }
        val firstCard = Deck.order(chores, today).firstOrNull()
            ?: due.minWithOrNull(Deck.orderingOn(today))
        return NagContent(
            firstCardTitle = firstCard?.name,
            dueCount = due.size,
            streak = Streak.of(completionDays, today),
        )
    }
}

/**
 * The nag copy templates: one line per level, with k-more and streak-0
 * variants. Gentle never mentions the streak; frequent puts it on the line;
 * last-chance warns it dies at midnight. A zero streak swaps in the
 * start-again lines instead.
 */
object NagCopy {

    fun text(content: NagContent, level: NagLevel): String {
        val title = content.firstCardTitle ?: return ""
        val more = content.dueCount - 1
        val head = when (level) {
            NagLevel.GENTLE ->
                if (more > 0) "$title + $more more are due today" else "$title is due today"
            NagLevel.FREQUENT ->
                if (more > 0) "$title + $more more still waiting" else "$title is still waiting"
            NagLevel.LAST_CHANCE ->
                if (more > 0) "$title + $more more left" else "$title left"
        }
        val tail = when (level) {
            NagLevel.GENTLE -> ""
            NagLevel.FREQUENT ->
                if (content.streak == 0) " — start a new streak today"
                else " — ${content.streak}-day streak on the line"
            NagLevel.LAST_CHANCE ->
                if (content.streak == 0) " — complete one before midnight"
                else " — ${content.streak}-day streak dies at midnight"
        }
        return head + tail
    }
}
