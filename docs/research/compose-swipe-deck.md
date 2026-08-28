# Research: Compose swipe-deck approach (Tinder-style card, v1)

**Ticket:** edrobertsrayne/fine#5
**Date:** 2026-08-28
**Status:** Resolved — recommendation below.

## Question

What is the simplest way to build a Tinder-style one-card-at-a-time swipe deck
(right = complete, left = discard, rejected-left state when the discard budget
is spent) in Jetpack Compose for v1? Optimise for least code and rapid
agentic iteration.

## Recommendation (TL;DR)

**Hand-roll the card with standard Compose APIs — no third-party library.**
Use `pointerInput` + `detectDragGestures` for the gesture, `Animatable` for the
commit / spring-back animation, and `graphicsLayer` (translation + rotationZ)
for the Tinder tilt. All of it fits in one file of roughly 100–150 lines, has
zero dependency risk, and is fully agentic-iterable (pure function of state,
previewable, testable with Compose UI tests).

Keep the deck screen **non-scrollable** (single flat deck, one card at a time).
That eliminates the horizontal-drag vs vertical-scroll conflict class entirely.

For the spent-discard-budget rejection: check the budget at commit time; if
spent, animate the card back to centre with `Animatable.animateTo(0f, spring())`
and show a `Snackbar` ("No discards left") via `SnackbarHostState`.

## Why not a library?

Current library options and maintenance status (checked 2026-08-28):

| Library | Status | Why not for v1 |
| --- | --- | --- |
| `com.alexstyl.swipeablecard:swipeablecard` (compose-tinder-card) | **ARCHIVED** (repo read-only, note from author, last update Jun 2024) | Archived; author moved on to composables.com and asks people to fork. Never reached stable 1.0. [GitHub](https://github.com/alexstyl/compose-tinder-card) |
| `com.github.smartword-app:compose-swipeable-cards` | JitPack-only, 2 contributors, ~99 stars, last release Jun 2025, 4 open issues | Micro-project, JitPack dependency (non-Maven-Central supply chain), stacked-deck feature set larger than v1 needs |
| `io.github.makzimi:swipingcards` (v0.1.0, Jul 2026) | Brand new, Maven Central, Compose Multiplatform | Wrong semantics (swipe-to-**cycle**-to-back deck, not dismiss-with-action), requires **minSdk 33**, v0.1.0 API churn likely |
| `io.github.aghajari:LazySwipeCards` (1.0.1, 2023) | Effectively dormant | Pinned to a Feb 2024 Compose BOM era; DSL-style stacked deck, more than v1 needs |
| `theapache64/twyper` (2022) | Dormant | Sample-grade, pre-1.0 Compose era |

For a **single flat deck** (one visible card, no stacking, no lazy recycling),
a library buys stacked-card effects and large-deck recycling we don't need,
while adding a dependency whose maintenance we don't control. Every viable
option above is archived, dormant, a 0.x micro-project, or semantically
mismatched. The gesture itself is ~100 lines of standard API.

## The hand-rolled approach (what the build spec should prescribe)

One composable file, e.g. `SwipeDeck.kt`, containing a `SwipeCard` composable:

### Core mechanics

1. **Gesture**: `Modifier.pointerInput(card.id) { detectDragGestures { change, dragAmount -> change.consume(); dragX += dragAmount.x; dragY += dragAmount.y } }`.
   Key the `pointerInput` (and the `Animatable`) on the card id so state resets
   per card. Official docs show exactly this pattern for controlled dragging
   ([Drag, swipe, and fling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling)).
2. **Rendering**: `Modifier.graphicsLayer { translationX = dragX; translationY = dragY; rotationZ = dragX / cardWidthPx * MAX_DEG }`
   — the tilt is derived from the x-drag; no extra animation state. Docs:
   [`graphicsLayer`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/GraphicsLayerScope).
3. **Position updates**: apply position via the **lambda form** `Modifier.offset { IntOffset(...) }` (or inside `graphicsLayer`) so per-frame drag updates don't recompose — they only re-run layout/draw. This is the documented pattern in the drag docs snippet.
4. **Commit on release**: in `onDragEnd`, commit when
   `abs(dragX) > cardWidthPx * 0.4f` (a positional threshold) **or** the drag
   velocity exceeds a threshold; otherwise spring back. Fling-to-commit is the
   UX users expect from Tinder-like decks.
5. **Commit animation**: `Animatable` animating x off-screen in the committed
   direction, then invoke `onComplete` / `onDiscard` (which advances the deck).
   Spring back with `Animatable.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))`.
6. **Optional buttons** (✓ / ✗ under the card): drive the same commit path by
   launching the same `Animatable` off-screen animation — keeps gesture and
   buttons in one code path.

### API choice notes

- Material's `Modifier.swipeable` is **deprecated** in favour of Foundation's
  `anchoredDraggable` (Compose Foundation 1.6+) — do not use `swipeable` in new
  code. ([Migrate from Swipeable to AnchoredDraggable](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/migrate-swipeable))
- `anchoredDraggable` is the "right" axis-safe primitive (anchor at 0f, commit
  at ±threshold), but it is anchored-state machinery sized for bottom sheets /
  drawers; for a free-form Tinder card (two-sided commit + tilt + off-screen
  fly-out) the raw `pointerInput` + `Animatable` version is **less code and
  fewer concepts**. If we later need axis-safety, `draggable(orientation =
  Horizontal)` is the minimal upgrade that keeps the same state code.
- `HorizontalPager` is **not** a good fit: it snaps to adjacent pages, doesn't
  give bidirectional action semantics (left and right mean different things),
  and fighting its snapping for Tinder-style tilt/fly-out costs more than it
  saves.

### Alternative considered: Material3 `SwipeToDismissBox`

Material3 ships `SwipeToDismissBox` /
`rememberSwipeToDismissBoxState(confirmValueChange = …)` — the documented
swipe-to-dismiss component ([Swipe to dismiss or
update](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/swipe-to-dismiss)).
Notable:

- Returning `false` from `confirmValueChange` **rejects the swipe and the box
  springs back** — this is the documented rejection mechanism, which maps
  perfectly onto "discard budget spent".
- `enableDismissFromStartToEnd` / `enableDismissFromEndToStart` can hard-disable
  the discard direction when the budget is spent.
- **Why it's not the primary recommendation:** it's designed for list-row
  dismissal (content over a full-bleed `backgroundContent`), and getting the
  Tinder tilt requires reading the drag offset, which is awkward/experimental
  on `SwipeToDismissBoxState`. If we drop the tilt, `SwipeToDismissBox` is a
  legitimate even-simpler fallback; with the tilt, hand-rolling is simpler.

## Gesture-conflict pitfalls

1. **Raw `detectDragGestures` consumes *all* drag deltas, on every axis**
   ([docs show `change.consume()`](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling)).
   Inside a `verticalScroll` / `LazyColumn` parent this **starves the parent's
   scroll** — the card hijacks vertical drags. Mitigations, in order of
   preference:
   - Keep the deck screen non-scrollable (v1 plan) → no conflict exists.
   - If a scrollable parent is ever needed: axis-lock with
     `Modifier.draggable(orientation = Horizontal)` or `anchoredDraggable`
     (both do touch-slop axis locking and participate in nested scroll), or
     gate `change.consume()` on `abs(dragAmount.x) > abs(dragAmount.y)` in the
     first few deltas.
2. **Touch slop and children:** drag handlers only consume after touch slop, so
   buttons/links inside the card still receive taps — but avoid
   `clickable`-and-draggable double-handling by keeping tappable children
   small and away from card edges.
3. **Single-card edge cases:**
   - **Last card removed → layout collapse.** Keep the card slot in a
     fixed-size `Box` so the screen doesn't jump when the deck empties; render
     an end-of-deck state inside it.
   - **Card cut off mid-flight.** Don't remove the card from state at commit
     *time*; remove it *after* the fly-out animation finishes, or the card
     disappears instantly.
   - **Stale drag state on the next card.** Reset (`Animatable(0f)`, dragX/dragY
     = 0f) per card — keying on `card.id` in `remember`/`pointerInput` handles
     this.
   - **Rejection while a previous animation is running:** launching a new
     `Animatable.animateTo` on the same `Animatable` cancels the in-flight
     animation, which is the desired behaviour (card re-captured under the
     finger).

## Rejection UX for a spent discard budget

Simplest and lowest-code, in order:

1. **Hand-rolled (recommended):** on drag end, if the drag commits left but
   `discardsLeft == 0`, spring the card back to centre
   (`Animatable.animateTo(0f, spring())`) and
   `snackbarHostState.showSnackbar("No discards left")`. Optionally a subtle
   shake or a red-tinted overlay while dragging left once the budget is spent
   (progressive feedback, ~5 extra lines).
2. **SwipeToDismissBox variant:** `confirmValueChange = { it != EndToStart ||
   discardsLeft > 0 }` — returning `false` makes the box spring back
   automatically (documented behaviour), plus
   `enableDismissFromEndToStart = discardsLeft > 0` to stop the drag at the
   source when the budget is spent.
3. Not recommended: `Toast` (unstyled, no queueing, not Material), or dialog
   (too heavy for a repeated, cheap signal).

## Spec-prescription summary

> Build the v1 deck as a single non-scrollable screen with one hand-rolled
> `SwipeCard` composable (~100–150 lines): `pointerInput`/`detectDragGestures`
> for the gesture, `graphicsLayer` for tilt, `Animatable` for commit/spring-back,
> keyed on card id. Right swipe → complete, left swipe → discard (spring back +
> Snackbar when the discard budget is spent). No third-party swipe libraries.
> If a scrollable parent is ever introduced, switch the gesture to
> `Modifier.draggable(Orientation.Horizontal)` / `anchoredDraggable`.

## Sources

- [Drag, swipe, and fling — Jetpack Compose docs](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling) — `draggable`, `pointerInput`/`detectDragGestures`, offset-lambda pattern, `swipeable` deprecation notice.
- [Migrate from Swipeable to AnchoredDraggable](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/migrate-swipeable) — `swipeable` deprecated in favour of Foundation `anchoredDraggable` (Foundation 1.6+).
- [Swipe to dismiss or update — Jetpack Compose docs](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/swipe-to-dismiss) — `SwipeToDismissBox`, `confirmValueChange` returning `false` to reject/spring back, `dismissDirection` background pattern.
- [`anchoredDraggable` API reference](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/anchoredDraggable.modifier) — overloads, experimental status, deprecations in 1.8.
- [alexstyl/compose-tinder-card](https://github.com/alexstyl/compose-tinder-card) — archived (read-only), `com.alexstyl.swipeablecard:swipeablecard`.
- [smartword-app/compose-swipeable-cards](https://github.com/smartword-app/compose-swipeable-cards) — JitPack, last release 1.1.4 (Jun 2025).
- [makzimi/SwipingCards](https://github.com/makzimi/SwipingCards) — v0.1.0 (Jul 2026), Maven Central, minSdk 33, swipe-to-cycle semantics.
- [Aghajari/LazySwipeCards](https://github.com/aghajari/lazyswipecards) — 1.0.1 (2023), pinned to Feb 2024 Compose BOM.
- [theapache64/twyper](https://github.com/theapache64/twyper) — 2022-era sample-grade library.
