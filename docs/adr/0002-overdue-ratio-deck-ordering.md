---
status: accepted, supersedes ADR-0001
---

# Deck orders by overdue-ratio, not cadence

ADR 0001 made cadence the primary sort key so a frequent chore always outranked an infrequent one, regardless of how overdue the infrequent one was. In practice this let a handful of frequent chores permanently bury infrequent ones: an infrequent chore just keeps losing every day, no matter how overdue it gets. We replaced the primary key with overdue-ratio (days-overdue / cadence), which measures how many full cycles of *itself* a chore has missed. This makes a daily chore one day late and a 30-day chore 30 days late rank equally, instead of cadence alone deciding. Cadence (shortest first) and creation order (most-recent first) remain as tiebreakers for equal ratios — most commonly chores that just became due today, which all start at ratio 0. We considered keeping cadence as a co-primary key alongside the ratio, but that would have partially reproduced the exact swamping problem this change exists to fix.
