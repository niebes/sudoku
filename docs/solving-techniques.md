# Sudoku solving techniques: a levelled reference

Every non-brute-force technique worth knowing, ordered into levels by cost and
payoff, with worked examples. Each level assumes the ones above it.

The organising idea is that solving is a **cascade**: apply the cheapest technique
that fires, take the change it produces, and start again from level 1. Almost every
deduction unlocks a cheaper one. Expensive techniques exist only to break a deadlock
and hand control back to the cheap ones.

---

## Notation

Single-digit diagrams show where **one** digit may still go:

```
 .   that digit cannot go here
 5   that digit is still a candidate here
 #   a cell taking part in the pattern
 x   a candidate the pattern eliminates
```

Cell-level diagrams show candidate lists: `{3,7}` means the cell is down to 3 or 7.

Two words used throughout:

- **House** — a row, a column, or a 3x3 box. Every cell belongs to exactly three.
- **See** — two cells *see* each other if they share a house. Every cell sees 20 others.

---

## The solving loop

```
  repeat:
      for level in 1, 2, 3, ... :
          if a technique at this level changes anything:
              apply it and restart from level 1
      if nothing changed at any level:
          stop - the puzzle is beyond the implemented techniques
```

Restarting from level 1 matters for two reasons. It keeps expensive searches off the
hot path, and it means the trace records *the easiest technique available at each
step* — which is what a difficulty rating actually is.

The alternative — running every level on every pass and taking the combined result —
looks equivalent and is not. It is slower, because the expensive levels run constantly
instead of rarely. And it destroys the rating: a jellyfish and a naked single that fire
on the same pass are both credited, so a puzzle that singles could have finished on
their own comes out looking like it needed a jellyfish.

**A worked cascade.** One level-3 deduction paying for three level-1 placements:

```
row:  {1,2}  {1,2}  {1,2,5}  {5,8,9}  {5,9}  4  7  3  6
```

Level 3 spots that two cells both read `{1,2}`. Between them they use up 1 and 2, so
no other cell in the row can hold either — the third cell drops to `{5}`:

```
row:  {1,2}  {1,2}    {5}    {5,8,9}  {5,9}  4  7  3  6
```

Now hand back to level 1, which places that 5. Removing it from the rest of the row
leaves `{8,9}` and `{9}` — another naked single. Place the 9, remove it, and `{8}`
falls out after it:

```
row:  {1,2}  {1,2}     5        8       9    4  7  3  6
```

Three cells placed off the back of one pair. The remaining `{1,2}` needs a column or
box to settle it — which is the normal way a cascade ends: not with the house
finished, but with the cheap techniques exhausted and progress made elsewhere.

---

## The honest boundary

Three facts in tension.

**Every proper puzzle is deducible.** A proper Sudoku has exactly one solution, so
every cell is entailed by the givens. Nothing is ever genuinely down to chance.

**No fixed rule set is complete.** The ladder is open-ended — each level solves
puzzles the level above cannot, and there is no point at which you have them all. The
most credible completeness claim in the literature is that *every known sudoku can be
solved using only chains of various degrees of complexity and singles*. Note the
wording: **known**, and **of various degrees of complexity**, which is not a bounded
set.

**The top of the ladder is search wearing a proof coat.** A forcing chain picks a
cell with two candidates, follows both branches, and concludes from the outcome: if
both branches place the same digit somewhere, it is true; if one branch contradicts,
it is false. Structurally that is trial and error. It differs from guessing in its
bookkeeping — it converts a branch's outcome into an elimination in the original grid
and never keeps a tentative state, so it yields certainty rather than a trial that
might be abandoned. By convention these count as logic, not guessing.

So "solve without guessing" resolves differently depending on what is meant:

| Reading | Achievable? |
|---|---|
| Every deduction is certain when made; no branch is ever abandoned | Yes for any proper puzzle — but only by climbing as far as forcing chains |
| No tentative value is ever written into a cell, even internally | **No.** Forcing chains and Nishio assign internally; they just discard the branch |
| Finish the puzzles people actually publish without branching | Yes, comfortably, with levels 1–5 |

**A practical target:** implement levels 1–5, keep a backstop, and aim for a solver
where the backstop never fires on a real puzzle.

---

# Level 1 — Singles

The only techniques that *place* digits. Everything below merely eliminates
candidates, and does so to create work for this level.

### Full house / last digit

A house with exactly one unsolved cell. The cheapest check in the game.

```
row:  4  1  7  9  {6}  2  8  5  3      ->  the gap must be 6
```

### Naked single

A cell down to one candidate.

```
{5}  ->  5
```

### Hidden single

A digit that fits in only one cell of a house, even though that cell has other
candidates. Here, 7 in a box:

```
        c1 c2 c3
 r1      .  .  .          7 is a candidate only at r2c2 within this box,
 r2      .  #  .          so r2c2 = 7 - regardless of what else it could hold
 r3      .  .  .
```

Hidden singles are what "crosshatching" finds, and they carry most easy puzzles on
their own.

---

# Level 2 — Locked candidates (intersections)

The best value in the entire list: two rules, each one pass over the houses, and
between them they carry most medium puzzles. Both exploit the overlap between a box
and a line.

### Pointing (locked candidates type 1)

If within a box all candidates for a digit lie in a single row or column, the digit
must be somewhere in that overlap — so it cannot be anywhere else along that line.

Digit 3, box 1:

```
        c1 c2 c3 | c4 c5 c6 | c7 c8 c9
 r1      .  .  . |
 r2      #  .  # |  x  .  x |  .  x  .
 r3      .  .  . |
```

Inside box 1, 3 can only go in row 2. Box 1 must contain a 3 somewhere, so row 2's
3 is inside box 1 — and every 3 elsewhere in row 2 goes.

### Claiming (locked candidates type 2, box-line reduction)

The mirror image. If within a row or column all candidates for a digit lie in a
single box, the digit must be in that overlap — so it cannot be elsewhere in the box.

Digit 6, row 4:

```
        c1 c2 c3 | c4 c5 c6 | c7 c8 c9
 r4      #  .  # |  .  .  . |  .  .  .
 r5      x  x  x |
 r6      x  .  x |
```

Row 4 must contain a 6, and its only places are inside box 4. So box 4's 6 lies on
row 4, and every 6 elsewhere in box 4 goes.

---

# Level 3 — Subsets

Reasoning about groups of cells inside a single house.

### Naked subset of size k

*k* cells in a house whose candidates, taken together, are exactly *k* digits. Those
*k* digits are used up by those *k* cells, so no other cell in the house can hold any
of them.

A naked triple — note that no cell needs all three digits:

```
 {2,7}   {2,9}   {7,9}   {2,4,7}   {1,9,5}   ...
   #       #       #        x         x
```

The union of the three marked cells is `{2,7,9}` — three digits in three cells. So 2,
7 and 9 vanish from every other cell in the house: `{2,4,7}` becomes `{4}`, a naked
single, and `{1,9,5}` becomes `{1,5}`.

### Hidden subset of size k

The dual. *k* digits in a house that occur in exactly *k* cells. Those cells must
hold those digits between them, so every *other* candidate in them goes.

A hidden pair. Five cells left in this row, holding 1, 2, 3, 5 and 8 between them:

```
 {2,5,8}  {2,5,8}  {1,3,5,8}  {1,3,5,8}  {2,5,8}
                       #          #
```

1 and 3 occur only in the two marked cells. Those cells must therefore take 1 and 3
in some order, so every other candidate in them goes: both collapse to `{1,3}`.

Note what that leaves — three cells reading `{2,5,8}`, which is a naked triple, and
the row is fully determined by two level-3 deductions in sequence.

> **Worth knowing:** in a house with *n* unsolved cells, a naked *k*-subset is the
> same fact as a hidden *(n−k)*-subset. Implementing both directions for *k* = 2, 3, 4
> therefore covers subsets well past size 4, which is where everyone stops.

### Locked pair / locked triple

A naked subset whose cells happen to lie in a box *and* a line — it eliminates along
both at once. Free if the subset code already knows which houses a cell belongs to.

---

# Level 4 — Basic fish

The first technique spanning the whole grid, and the first to work on a **single
digit** across many houses.

Pick *N* rows in which the digit is confined to the same *N* columns (or the
transpose). Those *N* rows must place the digit in those *N* columns, one per column
— so the digit cannot appear anywhere else in those columns.

### X-Wing (N = 2)

Digit 4:

```
      c1 c2 c3 c4 c5 c6 c7 c8 c9
 r1    .  .  x  .  .  .  .  .  .
 r2    .  .  #  .  .  .  #  .  .     <- 4 fits only in c3 and c7
 r3    .  .  .  .  4  .  x  .  .
 r4    .  .  .  .  .  .  .  .  .
 r5    .  .  #  .  .  .  #  .  .     <- 4 fits only in c3 and c7
 r6    .  .  x  .  .  .  .  .  .
 r7    .  .  .  4  .  .  .  .  .
 r8    .  .  .  .  .  .  x  .  .
 r9    .  .  .  .  .  .  .  .  .
```

Rows 2 and 5 each need a 4, and each can only take it in column 3 or column 7. Two
rows, two columns: the 4s occupy c3 and c7 in some order, using up both columns. So
every other 4 in c3 and c7 goes. The 4s at r3c5 and r7c4 are untouched — they are
outside the pattern's columns.

### Swordfish (N = 3), Jellyfish (N = 4)

The same shape, wider. Rows need not have exactly *N* candidates — two is fine, as
long as they fall inside the *N* columns.

Digit 7, a swordfish on rows 1, 3, 5 and columns 2, 5, 8:

```
      c1 c2 c3 c4 c5 c6 c7 c8 c9
 r1    .  #  .  .  #  .  .  .  .     <- 7 only in c2, c5
 r2    .  .  .  .  .  .  .  .  .
 r3    .  #  .  .  .  .  .  #  .     <- 7 only in c2, c8
 r4    .  x  .  .  x  .  .  x  .
 r5    .  .  .  .  #  .  .  #  .     <- 7 only in c5, c8
 r6    .  .  .  .  .  .  .  .  .
 r7    .  x  .  .  .  .  .  .  .
 r8    .  .  .  .  x  .  .  .  .
 r9    .  .  .  .  .  .  .  x  .
```

Three rows confined to three columns, so those three columns are spoken for.

Beyond N = 4 the patterns get rarer than they are worth; a size-5 fish (squirmbag) is
almost always visible as something cheaper.

---

# Level 5 — Single-digit patterns and wings

Chain-flavoured reasoning that needs no chain machinery. Everything here rests on
**conjugate pairs**: a house where a digit has exactly two possible cells, so one of
them is the digit and the other is not.

### Skyscraper

Two rows where the digit has exactly two places, sharing one column.

Digit 9:

```
      c1 c2 c3  c4 c5 c6  c7 c8 c9
 r1    .  .  .   .  .  .   .  .  x
 r2    .  #  .   .  .  .   .  #  .     <- 9 only in c2, c8
 r3    .  .  .   .  .  .   .  .  x
 r4    .  .  .   .  .  .   .  x  .
 r5    .  #  .   .  .  .   .  .  #     <- 9 only in c2, c9
 r6    .  .  .   .  .  .   .  x  .
```

Column 2 can hold only one 9, so r2c2 and r5c2 are not both 9 — meaning at least one
of the far ends, r2c8 or r5c9, **is** 9. Any cell seeing both far ends therefore
cannot be 9. r1c9 and r3c9 share a box with r2c8 and a column with r5c9; r4c8 and
r6c8 share a box with r5c9 and a column with r2c8.

### 2-String Kite

A row and a column, each with two places for the digit, with one end from each in the
same box.

Digit 4: row 2 has 4 only at c1 and c8; column 3 has 4 only at r1 and r7. The near
ends r2c1 and r1c3 share box 1, so they cannot both be 4 — meaning at least one far
end, r2c8 or r7c3, is 4. **Eliminate 4 from r7c8**, which sees both.

### Turbot fish

The general form: any two conjugate pairs on one digit linked by a cell that sees one
end of each. Skyscraper and 2-String Kite are its two special cases, so implementing
Turbot Fish subsumes both.

### Empty rectangle

Within a box, all candidates for the digit fit into one row plus one column of that
box — leaving a 2x2 "empty rectangle" in the corner. Combine with a conjugate pair
elsewhere.

Digit 4 in box 5 occupies only row 5 and column 5 of that box. Column 8 has 4 only at
r2 and r5. Then **4 goes from r2c5**:

> If r2c5 were 4, column 5's 4 is at row 2, so box 5's 4 must be on row 5 — which
> makes row 5's 4 land inside box 5, so r5c8 is not 4. The conjugate pair then forces
> r2c8 = 4. But now row 2 holds two 4s. Contradiction.

### XY-Wing

Three cells with two candidates each. A **pivot** `{x,y}`, and two **pincers**
`{x,z}` and `{y,z}`, each seeing the pivot.

```
        c2                    c7
 r2   {5,8}  . . . . . . .  {3,5}      pivot at r2c2, pincer A at r2c7 (same row)
        .                      .
 r6   {3,8}  . . . . . . .    x        pincer B at r6c2 (same column)
```

The pivot is 5 or 8. If it is 5, pincer A cannot be 5, so A = 3. If it is 8, pincer B
cannot be 8, so B = 3. Either way **one of the pincers is 3** — so any cell seeing
both pincers loses 3. Here r6c7 sees A down its column and B along its row.

### XYZ-Wing

The pivot keeps a third candidate: pivot `{x,y,z}`, pincers `{x,z}` and `{y,z}`. Now
*all three* cells could be z, so eliminations are limited to cells seeing all three.

### W-Wing

Two cells holding the same pair `{x,y}` that do **not** see each other, joined by a
conjugate pair on x whose two ends see one cell each. Then one of the two cells is y,
so y goes from any cell seeing both.

---

# Level 6 — Uniqueness

These exploit the fact that a published puzzle has exactly **one** solution. They are
fast and they fire often — but see the warning below.

### Unique rectangle, type 1

Four cells forming a rectangle across two rows, two columns and exactly **two** boxes,
where three of them hold the identical pair `{a,b}`:

```
          c1        c4
 r1     {3,7}     {3,7}
 r2     {3,7}    {3,7,5}
```

If the fourth cell were 3 or 7, all four corners would read `{3,7}` and the two
digits could be swapped diagonally — giving two valid solutions. A proper puzzle has
one. So **the fourth cell is 5**.

Types 2 to 6, plus hidden and avoidable rectangles, handle the cases where more than
one corner carries extras.

### BUG+1

If every unsolved cell has exactly two candidates except one cell with three, the
grid would have an even number of solutions unless that extra candidate is placed. The
answer is the candidate appearing **three times** in one of that cell's houses:

```
 r5c5 = {2,6,9}, every other unsolved cell bivalue.
 Across row 5:  2 appears twice, 9 appears twice, 6 appears three times.
 -> r5c5 = 6
```

> **These techniques are only valid on a grid known to have exactly one solution.**
> That is true of a puzzle as handed to you, and false of a grid reached by assuming
> a value — where a wrong assumption may have produced many completions or none. Any
> solver that ever branches must switch this level off inside a branch. Given that
> levels 2–5 cover more ground with no such caveat, this level is optional.

---

# Level 7 — Colouring and chains

The point where the unit of reasoning stops being a cell and becomes a
**(cell, digit) node**, connected by two kinds of link:

- **Strong link** — if one end is false, the other is true. A conjugate pair, or the
  two candidates inside one cell.
- **Weak link** — if one end is true, the other is false. Any two candidates for the
  same digit sharing a house, or any two candidates inside one cell.

### Simple colouring

Follow the strong links for a single digit and two-colour the graph. Exactly one
colour is the truth.

Digit 7, conjugate pairs r1c1–r1c5 (row 1), r1c5–r6c5 (column 5), r6c5–r6c9 (row 6):

```
  r1c1 = A        r1c5 = B        r6c5 = A        r6c9 = B
```

Two consequences:

- **Colour trap** — any cell seeing both a colour A cell and a colour B cell cannot
  be 7, since one of the two is 7 whichever colour wins. Here r6c1 sees r1c1 down
  column 1 and r6c9 along row 6, so **7 goes from r6c1**.
- **Colour wrap** — if two cells of the *same* colour ever share a house, that colour
  is impossible, so every cell of the other colour is 7.

### Remote pair

A chain of four or more cells all holding the same pair `{2,7}`, each seeing the next.
The values alternate, so the two ends of an even-length chain are opposite.

```
 r1c1{2,7} -- r1c6{2,7} -- r4c6{2,7} -- r4c9{2,7}
```

Four cells, so r1c1 and r4c9 hold different digits — one is 2 and one is 7. Any cell
seeing both loses **both** digits: r1c9 and r4c1.

### X-chain

Alternating strong and weak links on one digit, starting and ending with a strong
link. One endpoint must hold the digit, so cells seeing both endpoints lose it.

### XY-chain

A chain of two-candidate cells where each shares a digit with the next, and both ends
share a digit z.

```
 r1c1{4,9} -- (9) -- r1c7{2,9} -- (2) -- r5c7{2,4}
```

If r1c1 is not 4 it is 9; then r1c7 is 2; then r5c7 is 4. So **one end is 4** either
way, and any cell seeing both ends loses 4 — here r5c1.

### Nice loops and AIC

The general form. An alternating inference chain that closes on itself: a
*discontinuous* loop yields one elimination or placement at the break point, a
*continuous* loop upgrades every weak link in it to strong and yields many
eliminations at once. **Grouped** variants let a node be a set of candidates — a
box-line intersection, or an almost locked set — rather than a single cell.

---

# Level 8 — Almost locked sets and exotic fish

An **almost locked set** is *n* cells within one house holding *n + 1* candidates: one
digit short of being locked. Remove any one candidate and the rest lock.

- **ALS-XZ** — two almost locked sets sharing a *restricted common candidate* x, which
  can be true in only one of them. Any digit z common to both can then be eliminated
  from every cell seeing all of z's positions in both sets.
- **ALS-XY-Wing**, **ALS chain**, **Death Blossom** — a stem cell whose every candidate
  links into a different almost locked set.
- **Sue de Coq** — a box-line intersection whose candidates decompose so that the cells
  outside it are constrained from both directions.
- **Finned and sashimi fish** — a fish that would be valid but for extra candidates
  (**fins**) in one base unit. The eliminations survive, restricted to cells that also
  see every fin.

  Digit 5: rows 2 and 6 would form an X-Wing on columns 3 and 7, except row 6 has a
  third candidate at c8 — in the same box as the c7 corner. If the fin is not 5 the
  X-Wing is real; if the fin is 5 then that box's 5 is used up. Either way, cells in
  column 7 *inside that box* lose their 5.

- **Franken** and **mutant fish** — fish whose base and cover sets mix boxes with lines.

---

# Level 9 — Methods of last resort

Listed for completeness. All are search with a notebook.

- **Templates / pattern overlay** — enumerate the 46,656 ways a single digit can fill a
  grid, and intersect with the constraints. Not a human technique in any meaningful
  sense.
- **Forcing chain** — any chain leading to a contradiction or a verity: follow both
  branches of a two-candidate cell and use whatever they agree on.
- **Forcing net** — the branching version, findable by hand only with great patience.
- **Kraken fish** — a finned fish whose fin is resolved by a chain.
- **Nishio, Bowman's bingo** — systematic trial.

---

## Which levels a puzzle needs

Roughly, by published difficulty band:

| Band | Levels required |
|---|---|
| Easy | 1 |
| Medium | 1–2, plus naked and hidden pairs from 3 |
| Hard | 1–3 in full, often X-Wing or Swordfish from 4 |
| Expert | 1–5, sometimes uniqueness or colouring |
| Extreme | Chains, AIC and ALS from 7–8; occasionally 9 |

Levels 1–3 finish the large majority of puzzles in general circulation. Levels 4–5
cover nearly all of the rest. Levels 7 and up exist for a thin tail of deliberately
hard constructions.

Clue count is a **poor** predictor of difficulty — a 30-clue puzzle can need chains
and a 22-clue puzzle can fall to singles. The one reliable signal is at the extreme:
17 is the proven minimum number of clues for a unique solution, and puzzles near that
floor almost always need level 7 or beyond.

---

## Three traps worth designing around

**Never decide against a stale snapshot.** Most techniques begin by grouping cells —
by house, by candidate count, by digit. If eliminations are then applied while that
grouping is still being iterated, later decisions are made against a picture that is
no longer true. Two hidden singles can claim the same cell; a digit can look confined
to one box because a cell that was solved a moment ago quietly dropped out of the
list. The robust shape is to compute every elimination against an unchanging snapshot
and apply them as one batch, so a technique physically cannot observe its own partial
results.

**A digit already placed in a house must disqualify that house, not vanish from it.**
The common form of the previous bug. Filtering a candidate list by "cells not yet
solved" silently drops the placed digit, and the remaining candidates then look
confined when they are merely leftovers. Check for the placement explicitly.

**Completing the grid is not the same as solving it.** Checking that every cell has a
value says nothing about whether the values are legal. Techniques that narrow cells
through different houses can place the same digit twice in one house, and a completed
grid containing a duplicate will pass a naive "is it finished?" test. Validity means
no house repeats a digit — check that, not the fill count.

---

## Sources

- [HoDoKu — Human style solving techniques](https://hodoku.sourceforge.net/en/techniques.php) — the most complete taxonomy available, 70+ techniques
- [HoDoKu — Intersections (locked candidates)](https://hodoku.sourceforge.net/en/tech_intersections.php)
- [HoDoKu — Chains and loops](https://hodoku.sourceforge.net/en/tech_chains.php) — source of the "chains and singles solve every known sudoku" claim
- [HoDoKu — Methods of last resort](https://hodoku.sourceforge.net/en/tech_last.php)
- [SudokuWiki — Strategy list](https://www.sudokuwiki.org/sudoku.htm) — 42 strategies grouped basic / tough / diabolical / extreme
- [Sudopedia — Solving technique](https://www.sudopedia.org/wiki/Solving_Technique) — category index
- [Sudoku A Day — Strategies by difficulty](https://sudokuaday.com/sudoku-strategies) — which techniques each difficulty band needs
