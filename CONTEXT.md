# nag

A personal Android nag app: a daily deck of chore cards you swipe to complete, backed by streaks and escalating notifications. Local-only, single user.

## Language

**Chore**:
A recurring task the user tracks. Has a name and a cadence. Nothing else.

**Cadence**:
The every-N-days rhythm of a chore. N is a positive integer; N=1 means daily.

**Next-due**:
The date on which a chore becomes due. A chore is due when today ≥ next-due; overdue chores stay due until completed.

**Completion**:
The act of swiping a due card right (or its equivalent). Recorded with the day it happened; days are local.

**Deck**:
The stack of all due-today chores, shown one card at a time. Most-overdue first.

**Discard**:
Swiping a card left to put a chore off for the rest of the day. The chore stays due; the card is hidden until tomorrow. At most 2 per day, globally.

**Streak**:
The count of consecutive days, ending today or yesterday, with at least one completion of any chore. A day with zero completions breaks it when it ends.

**Archive**:
Removing a chore from active duty without destroying its records. Archived chores leave the queue and deck; their completion history persists and still counts toward the streak.

_Avoid_: delete (for chores), remove

**Queue**:
The screen listing active chores, where chores are added, edited, and archived.

**Nag**:
A notification posted to pull the user back to the deck. Nags escalate across the day and go quiet for the day at the first completion. Tap-to-open only; no nag before 09:00 or after 21:30, any day.

**Silent window**:
The weekday work hours (08:00–17:59 Mon–Fri) during which nothing is posted at all.

**Escalation level**:
The three nag intensities over the day: gentle (silent), frequent (sound), last-chance (heads-up). A last-chance nag warns that the streak dies at midnight.
