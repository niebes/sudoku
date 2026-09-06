// The lesson behind each technique, condensed from docs/solving-techniques.md - the levelled
// reference this site teaches from. Each entry:
//   text     what the technique is and why it is sound - the step card next to it says what it
//            did on this grid
//   scan     how a person finds it while staring at a real grid
//   diagram  a mini-grid spec (see minigrid.js) drawn in the player's own colour language
//   caption  the sentence under the diagram
//   relatives  connective tissue: [{ key, why }] links to the techniques this one mirrors,
//              generalises or hands over to
//   note     present only on techniques the solver implements but deliberately keeps out of
//            its chain, saying why
// Entry order is display order in the library; keep it sorted by level.
const TECHNIQUES = {
  FULL_HOUSE: {
    name: 'Full house',
    level: 'Level 1: singles',
    text: 'A house (a row, column or box) with exactly one unsolved cell. Eight of its nine ' +
      'values are placed, so the gap takes the ninth. The cheapest check in the game, and the ' +
      'first thing a person looks for.',
    scan: 'Count filled cells per house as you go; any house at eight is a free placement. ' +
      'After every placement, glance at the three houses it just touched.',
    diagram: {
      rows: [''], cols: ['', '', '', '', '', '', '', '', ''], seamCols: [2, 5],
      cells: [
        { r: 0, c: 0, v: 4, given: true }, { r: 0, c: 1, v: 1, given: true },
        { r: 0, c: 2, v: 7, given: true }, { r: 0, c: 3, v: 9, given: true },
        { r: 0, c: 4, m: [6], role: 'target' },
        { r: 0, c: 5, v: 2, given: true }, { r: 0, c: 6, v: 8, given: true },
        { r: 0, c: 7, v: 5, given: true }, { r: 0, c: 8, v: 3, given: true },
      ],
    },
    caption: 'Eight of nine placed; the one value missing from them is 6, so the gap takes it.',
    relatives: [
      { key: 'NAKED_SINGLE', why: 'the same conclusion read from the cell instead of the house' },
    ],
  },

  NAKED_SINGLE: {
    name: 'Naked single',
    level: 'Level 1: singles',
    text: 'A cell down to a single candidate: every other value already appears somewhere in ' +
      'its row, column or box. Nothing is left to decide, so the cell takes that value. The ' +
      'singles are where digits actually get placed; every fancier technique below exists to ' +
      'create work for this level.',
    scan: 'Keep pencil marks honestly and naked singles announce themselves: watch for any ' +
      'cell whose marks have thinned to one, especially right after an elimination lands near it.',
    diagram: {
      rows: [''], cols: ['', '', '', '', ''],
      cells: [
        { r: 0, c: 0, v: 9, given: true }, { r: 0, c: 1, v: 3, given: true },
        { r: 0, c: 2, m: [5], role: 'target' },
        { r: 0, c: 3, m: [4, 7] }, { r: 0, c: 4, m: [1, 4, 7] },
      ],
    },
    caption: 'The marks have thinned to {5}: the 20 cells this one sees hold everything else.',
    relatives: [
      { key: 'HIDDEN_SINGLE', why: 'found by asking where a value goes, not what a cell holds' },
      { key: 'PEER_ELIMINATION', why: 'the bookkeeping that thins the marks down to one' },
    ],
  },

  HIDDEN_SINGLE: {
    name: 'Hidden single',
    level: 'Level 1: singles',
    text: 'A value that fits in only one cell of a house, even though that cell still has other ' +
      'candidates. The single hides behind them: find it by asking where the value can go rather ' +
      'than what the cell can be. This is what crosshatching finds, and it carries most easy ' +
      'puzzles on its own.',
    scan: 'Crosshatch: pick a value and a box, mentally strike the rows and columns that ' +
      'already hold that value, and see how many cells survive. One survivor is a placement. ' +
      'Values placed six or seven times already are the quickest wins.',
    diagram: {
      rows: ['r1', 'r2', 'r3'], cols: ['c1', 'c2', 'c3'],
      cells: [{ r: 1, c: 1, m: [7], role: 'target' }],
    },
    caption: 'Within this box a 7 fits nowhere else (blank = impossible), so r2c2 is 7 - ' +
      'whatever else it might have held.',
    relatives: [
      { key: 'NAKED_SINGLE', why: 'the same conclusion read from the cell instead of the value' },
    ],
  },

  PEER_ELIMINATION: {
    name: 'Peer elimination',
    level: 'Level 1: singles',
    text: 'The bookkeeping behind everything else. A value placed in a cell cannot appear again ' +
      'in that cell’s row, column or box, so the solver crosses it off the pencil marks of ' +
      'the 20 cells it sees. Every technique above reads the pencil marks this maintains.',
    scan: 'Not spotted so much as kept up: every time a value lands, sweep its row, column and ' +
      'box and cross it out. Let this slip once and every later deduction reasons from marks ' +
      'that are no longer true.',
    diagram: {
      rows: [''], cols: ['', '', '', ''],
      cells: [
        { r: 0, c: 0, v: 5, role: 'pattern' },
        { r: 0, c: 1, m: [2, 5], gone: [5] },
        { r: 0, c: 2, m: [5, 8, 9], gone: [5] },
        { r: 0, c: 3, m: [3, 4] },
      ],
    },
    caption: 'A placed 5 leaves the marks of everything it sees.',
    relatives: [
      { key: 'NAKED_SINGLE', why: 'what the thinned marks eventually produce' },
    ],
  },

  POINTING: {
    name: 'Pointing (locked candidates)',
    level: 'Level 2: locked candidates',
    text: 'Inside one box, every place a value can still go lies on a single row or column. The ' +
      'box must contain that value somewhere, so the value has to sit in that overlap, and the ' +
      'solver can cross it off everywhere else along the line. Two rules like this one carry ' +
      'most medium-difficulty puzzles.',
    scan: 'For each box and each missing value, look at which of the box’s three rows and ' +
      'three columns still admit it. All on one line is the pattern. Do this whenever ' +
      'crosshatching a box leaves two or three cells instead of one.',
    diagram: {
      rows: ['r1', 'r2', 'r3'], cols: ['c1', 'c2', 'c3', 'c4', 'c5', 'c6', 'c7', 'c8', 'c9'],
      seamCols: [2, 5],
      cells: [
        { r: 1, c: 0, m: [3], role: 'pattern' }, { r: 1, c: 2, m: [3], role: 'pattern' },
        { r: 1, c: 3, m: [3], gone: [3] }, { r: 1, c: 5, m: [3], gone: [3] },
        { r: 1, c: 7, m: [3], gone: [3] },
      ],
    },
    caption: 'Inside box 1 the 3 fits only on row 2, so row 2’s 3 is in box 1 - and every ' +
      '3 elsewhere on the row goes.',
    relatives: [
      { key: 'CLAIMING', why: 'its mirror image, arguing from the line instead of the box' },
    ],
  },

  CLAIMING: {
    name: 'Claiming (locked candidates)',
    level: 'Level 2: locked candidates',
    text: 'The mirror image of pointing. Along one row or column, every place a value can go ' +
      'lies inside a single box. The line must put its value there, which uses up that box’s ' +
      'copy, so the value leaves every other cell of the box.',
    scan: 'For each line and each missing value, note which of the three boxes it crosses still ' +
      'admit it. One box is the pattern. Lines with only two or three open cells are the ' +
      'cheapest to check.',
    diagram: {
      rows: ['r4', 'r5', 'r6'], cols: ['c1', 'c2', 'c3', 'c4', 'c5', 'c6'], seamCols: [2],
      cells: [
        { r: 0, c: 0, m: [6], role: 'pattern' }, { r: 0, c: 2, m: [6], role: 'pattern' },
        { r: 1, c: 0, m: [6], gone: [6] }, { r: 1, c: 1, m: [6], gone: [6] },
        { r: 1, c: 2, m: [6], gone: [6] },
        { r: 2, c: 0, m: [6], gone: [6] }, { r: 2, c: 2, m: [6], gone: [6] },
      ],
    },
    caption: 'Row 4’s 6 fits only inside box 4, which uses up the box’s 6 - the rest ' +
      'of the box loses it.',
    relatives: [
      { key: 'POINTING', why: 'its mirror image, arguing from the box instead of the line' },
    ],
  },

  NAKED_SUBSET: {
    name: 'Naked pair / triple / quad',
    level: 'Level 3: subsets',
    text: 'k cells of one house whose candidates, taken together, come to exactly k values. ' +
      'Those cells use those values up between them (no single cell needs to hold all of them), ' +
      'so the values vanish from every other cell of the house. Watch what it leaves behind: ' +
      'collapsing one cell to a single candidate hands the grid straight back to the singles.',
    scan: 'Pairs first: two cells in a house showing the identical two marks jump out, and they ' +
      'are most of the value. For triples, look in houses with many two- and three-mark cells ' +
      'and try uniting the candidates of any three of them.',
    diagram: {
      rows: [''], cols: ['', '', '', '', ''],
      cells: [
        { r: 0, c: 0, m: [2, 7], role: 'pattern' }, { r: 0, c: 1, m: [2, 9], role: 'pattern' },
        { r: 0, c: 2, m: [7, 9], role: 'pattern' },
        { r: 0, c: 3, m: [2, 4, 7], gone: [2, 7] },
        { r: 0, c: 4, m: [1, 5, 9], gone: [9] },
      ],
    },
    caption: 'Three cells sharing {2,7,9} use those values up: {2,4,7} collapses to {4} - a ' +
      'naked single, which is the point.',
    relatives: [
      { key: 'HIDDEN_SUBSET', why: 'the same fact seen from the values’ side' },
    ],
  },

  HIDDEN_SUBSET: {
    name: 'Hidden pair / triple / quad',
    level: 'Level 3: subsets',
    text: 'The dual of the naked subset: k values of a house that fit in only k cells. Those ' +
      'cells must hold those values between them, so everything else pencilled into them goes. ' +
      'In a house with n open cells, a naked k-subset and a hidden (n-k)-subset are the same ' +
      'fact seen from opposite sides.',
    scan: 'Per house, count the homes of each missing value. Two values that both have exactly ' +
      'two homes, and the same two, are a hidden pair - easiest to see on values crosshatching ' +
      'has already squeezed.',
    diagram: {
      rows: [''], cols: ['', '', '', '', ''],
      cells: [
        { r: 0, c: 0, m: [2, 5, 8] }, { r: 0, c: 1, m: [2, 5, 8] },
        { r: 0, c: 2, m: [1, 3, 5, 8], role: 'pattern', gone: [5, 8] },
        { r: 0, c: 3, m: [1, 3, 5, 8], role: 'pattern', gone: [5, 8] },
        { r: 0, c: 4, m: [2, 5, 8] },
      ],
    },
    caption: '1 and 3 fit only in the two marked cells, so those cells are 1 and 3 in some ' +
      'order and everything else in them goes.',
    relatives: [
      { key: 'NAKED_SUBSET', why: 'the same fact seen from the cells’ side' },
    ],
  },

  BASIC_FISH: {
    name: 'X-Wing / Swordfish / Jellyfish',
    level: 'Level 4: basic fish',
    text: 'The first pattern that spans the whole grid, and it works on a single value. Take N ' +
      'rows in which the value is confined to the same N columns. Each of those rows needs the ' +
      'value, and every place it can go lies in one of the columns: N values into N columns, one ' +
      'each. The columns are spoken for, so the value goes nowhere else in them. N=2 is the ' +
      'X-Wing, 3 the Swordfish, 4 the Jellyfish; rows and columns swap freely.',
    scan: 'Work one value at a time. List the rows where it has exactly two or three places ' +
      'left and compare their column sets; two rows sharing the same two columns is the X-Wing. ' +
      'Then transpose and do columns against rows.',
    diagram: {
      rows: ['r1', 'r2', 'r3', 'r4', 'r5', 'r6'],
      cols: ['c1', 'c2', 'c3', 'c4', 'c5', 'c6', 'c7', 'c8', 'c9'],
      seamRows: [2], seamCols: [2, 5],
      cells: [
        { r: 0, c: 2, m: [4], gone: [4] },
        { r: 1, c: 2, m: [4], role: 'pattern' }, { r: 1, c: 6, m: [4], role: 'pattern' },
        { r: 2, c: 4, m: [4] }, { r: 2, c: 6, m: [4], gone: [4] },
        { r: 4, c: 2, m: [4], role: 'pattern' }, { r: 4, c: 6, m: [4], role: 'pattern' },
        { r: 5, c: 2, m: [4], gone: [4] },
      ],
    },
    caption: 'Rows 2 and 5 each keep their 4 in c3 or c7, so the two columns are spoken for and ' +
      'every other 4 in them goes. The 4 at r3c5 sits outside the pattern and is untouched.',
    relatives: [
      { key: 'FINNED_FISH', why: 'what remains of a fish when one base row carries extras' },
      { key: 'TURBOT_FISH', why: 'the same single-value reasoning built from links instead of lines' },
    ],
  },

  TURBOT_FISH: {
    name: 'Turbot fish',
    level: 'Level 5: single-digit patterns',
    text: 'Built from conjugate pairs: houses where a value has exactly two homes, so one of ' +
      'them is the value. Take two such pairs, joined by two ends that see each other. Those two ' +
      'ends cannot both be true, so at least one of the far ends must be, and any cell seeing ' +
      'both far ends can drop the value. The skyscraper and the 2-string kite are this same ' +
      'argument with the links in particular places.',
    scan: 'Per value, mark the houses down to two homes - the strong links. Any two links whose ' +
      'near ends share a house form the pattern; then ask what sees both far ends. Values with ' +
      'many conjugate pairs are the hunting ground.',
    diagram: {
      rows: ['r1', 'r2', 'r3', 'r4', 'r5', 'r6'],
      cols: ['c1', 'c2', 'c3', 'c4', 'c5', 'c6', 'c7', 'c8', 'c9'],
      seamRows: [2], seamCols: [2, 5],
      cells: [
        { r: 0, c: 8, m: [9], gone: [9] },
        { r: 1, c: 1, m: [9], role: 'pattern' }, { r: 1, c: 7, m: [9], role: 'pattern' },
        { r: 3, c: 7, m: [9], gone: [9] },
        { r: 4, c: 1, m: [9], role: 'pattern' }, { r: 4, c: 8, m: [9], role: 'pattern' },
      ],
    },
    caption: 'A skyscraper: rows 2 and 5 each hold 9 twice, sharing column 2. The two left ends ' +
      'are not both 9, so a far end is - and r1c9 and r4c8 see both far ends.',
    relatives: [
      { key: 'SIMPLE_COLOURING', why: 'the same links followed further than two' },
      { key: 'AIC', why: 'the fully general chain this is the two-link case of' },
      { key: 'EMPTY_RECTANGLE', why: 'a box-shaped strong statement used the same way' },
    ],
  },

  XY_WING: {
    name: 'XY-Wing',
    level: 'Level 5: wings',
    text: 'Three cells with two candidates each: a pivot {x,y}, and two pincers {x,z} and {y,z} ' +
      'that each see the pivot. Whichever value the pivot takes, it strips that value from one ' +
      'pincer and forces it onto z. One of the pincers is z. You cannot say which, and you do ' +
      'not need to: any cell that sees both pincers cannot be z.',
    scan: 'Collect the two-candidate cells - there are rarely many. For each as pivot {x,y}, ' +
      'look among the cells it sees for an {x,z} and a {y,z}; then ask what sees both of those.',
    diagram: {
      rows: ['r2', 'r6'], cols: ['c2', 'c7'],
      cells: [
        { r: 0, c: 0, m: [5, 8], role: 'pattern' }, { r: 0, c: 1, m: [3, 5], role: 'pattern' },
        { r: 1, c: 0, m: [3, 8], role: 'pattern' }, { r: 1, c: 1, m: [3, 6], gone: [3] },
      ],
    },
    caption: 'Pivot r2c2: a 5 makes r2c7 the 3, an 8 makes r6c2 the 3. Either way a 3 stares ' +
      'at r6c7 from a cell it sees.',
    relatives: [
      { key: 'XYZ_WING', why: 'the pivot keeps the shared value, weakening the conclusion' },
      { key: 'W_WING', why: 'two equal pairs bridged by a strong link instead of a pivot' },
      { key: 'AIC', why: 'an XY-Wing is a three-cell chain read off in one look' },
    ],
  },

  XYZ_WING: {
    name: 'XYZ-Wing',
    level: 'Level 5: wings',
    text: 'Like the XY-Wing, but the pivot keeps the shared value too: pivot {x,y,z}, pincers ' +
      '{x,z} and {y,z}. Now all three cells could be z, which weakens the conclusion: only a ' +
      'cell that sees all three of them can drop z.',
    scan: 'Start from three-candidate cells as pivots. A matching pincer inside the pivot’s ' +
      'box and another along its row or column is the shape - the elimination zone is that ' +
      'same box-line overlap.',
    diagram: {
      rows: ['r2'], cols: ['c1', 'c2', 'c3', 'c4', 'c5', 'c6', 'c7', 'c8'], seamCols: [2, 5],
      cells: [
        { r: 0, c: 0, m: [3, 5, 8], role: 'pattern' },
        { r: 0, c: 1, m: [3, 9], gone: [3] },
        { r: 0, c: 2, m: [3, 5], role: 'pattern' },
        { r: 0, c: 7, m: [3, 8], role: 'pattern' },
      ],
    },
    caption: 'Pivot {3,5,8} at r2c1, pincers {3,5} in its box and {3,8} on its row: one of the ' +
      'three is the 3, and only r2c2 sees all of them.',
    relatives: [
      { key: 'XY_WING', why: 'the cleaner form, when the pivot lacks the shared value' },
      { key: 'ALS_XZ', why: 'the pivot-plus-pincer is a small almost locked set in disguise' },
    ],
  },

  W_WING: {
    name: 'W-Wing',
    level: 'Level 5: wings',
    text: 'Two cells holding the same pair {x,y} that do not see each other, bridged by a strong ' +
      'link on x: a house where x has exactly two places, one end seeing each pair cell. One end ' +
      'of the link is x, and whichever it is, the pair cell it sees cannot be x, so it is y. One ' +
      'of the pair is y either way, and any cell seeing both drops y.',
    scan: 'Note every cell showing the same bare pair. For each such pair of cells that do not ' +
      'see each other, hunt for a house where one of their two values has exactly two places, ' +
      'arranged so each end sees one pair cell.',
    diagram: {
      rows: ['r2', 'r5'], cols: ['c2', 'c5', 'c8'],
      cells: [
        { r: 0, c: 0, m: [4, 7], role: 'pattern' },
        { r: 0, c: 1, m: [4], role: 'alt' },
        { r: 0, c: 2, m: [2, 7], gone: [7] },
        { r: 1, c: 1, m: [4], role: 'alt' },
        { r: 1, c: 2, m: [4, 7], role: 'pattern' },
      ],
    },
    caption: 'Column 5 holds its 4 at r2 or r5 (amber). Either end pushes the pair cell it sees ' +
      'onto 7, so one of the pair is 7 - and r2c8 sees both.',
    relatives: [
      { key: 'XY_WING', why: 'the pivot version of the same one-of-these-two-is-z argument' },
      { key: 'AIC', why: 'a W-Wing is a four-node chain read off as a shape' },
    ],
  },

  EMPTY_RECTANGLE: {
    name: 'Empty rectangle',
    level: 'Level 5: single-digit patterns',
    text: 'Inside one box, every place for a value fits on one row plus one column, leaving the ' +
      'opposite 2×2 corner empty. That cross shape is itself a strong statement: the box’s ' +
      'value is on the row arm or the column arm. Combined with a conjugate pair elsewhere, it ' +
      'pins a cell that would push the value off both arms at once.',
    scan: 'Per value, look for boxes whose surviving places form a cross - a row and a column ' +
      'with an empty 2×2 corner - then for a conjugate pair on the same value whose ends line ' +
      'up with the arms.',
    diagram: {
      rows: ['r2', 'r4', 'r5', 'r6'], cols: ['c4', 'c5', 'c6', 'c7', 'c8'],
      seamRows: [0], seamCols: [2],
      cells: [
        { r: 0, c: 1, m: [4], gone: [4] },
        { r: 0, c: 4, m: [4], role: 'alt' },
        { r: 1, c: 1, m: [4], role: 'pattern' },
        { r: 2, c: 0, m: [4], role: 'pattern' }, { r: 2, c: 1, m: [4], role: 'pattern' },
        { r: 2, c: 2, m: [4], role: 'pattern' }, { r: 2, c: 4, m: [4], role: 'alt' },
      ],
    },
    caption: 'Box 5’s 4s form a cross on row 5 and column 5. If r2c5 were 4, the pair in ' +
      'column 8 (amber) would force a second 4 into row 2 - so it is not.',
    relatives: [
      { key: 'TURBOT_FISH', why: 'the same two-strong-statements argument with plainer links' },
      { key: 'POINTING', why: 'the degenerate cross with only one arm' },
    ],
  },

  UNIQUE_RECTANGLE: {
    name: 'Unique rectangle',
    level: 'Level 6: uniqueness',
    text: 'The one family that reasons from the puzzle rather than the grid: a published puzzle ' +
      'has exactly one solution. Four cells on two rows, two columns and two boxes, three of them ' +
      'holding the identical pair. If the fourth held just that pair too, the two values could ' +
      'swap around the rectangle and both grids would satisfy every rule: two solutions. So the ' +
      'fourth corner keeps neither value of the pair.',
    scan: 'Bare pairs again: two cells of one box showing the same pair are a rectangle waiting ' +
      'for its other two corners - follow their rows or columns and check what those hold. Mind ' +
      'the box condition: all four corners in four different boxes proves nothing.',
    diagram: {
      rows: ['r1', 'r2'], cols: ['c1', 'c4'],
      cells: [
        { r: 0, c: 0, m: [3, 7], role: 'pattern' }, { r: 0, c: 1, m: [3, 7], role: 'pattern' },
        { r: 1, c: 0, m: [3, 7], role: 'pattern' },
        { r: 1, c: 1, m: [3, 5, 7], role: 'target', gone: [3, 7] },
      ],
    },
    caption: 'If r2c4 were down to {3,7} too, the pair could swap around the rectangle: two ' +
      'solutions. A proper puzzle has one, so r2c4 = 5.',
    relatives: [
      { key: 'BUG_PLUS_ONE', why: 'the same uniqueness argument taken to the whole grid' },
    ],
  },

  BUG_PLUS_ONE: {
    name: 'BUG+1',
    level: 'Level 6: uniqueness',
    text: 'BUG stands for bivalue universal grave: every unsolved cell a pair, every value left ' +
      'with exactly two homes per house. Such a grid always has an even number of solutions, so ' +
      'a proper puzzle can never reach one. When a single cell stands between the grid and a ' +
      'grave, that cell must hold the one candidate whose removal would dig it.',
    scan: 'Late-game only: when nearly every unsolved cell shows a pair, look for the lone cell ' +
      'with three marks. The keeper is the value appearing three times in one of its houses.',
    diagram: {
      rows: ['r5'], cols: ['c2', 'c4', 'c5', 'c7'],
      cells: [
        { r: 0, c: 0, m: [2, 6] }, { r: 0, c: 1, m: [2, 9] },
        { r: 0, c: 2, m: [4, 6, 9], role: 'target', gone: [4, 6] },
        { r: 0, c: 3, m: [4, 9] },
      ],
    },
    caption: 'Every other unsolved cell is a pair. In this row 9 appears three times, the rest ' +
      'twice: remove it and the grid is a grave, so r5c5 = 9.',
    relatives: [
      { key: 'UNIQUE_RECTANGLE', why: 'the same argument on four cells instead of the grid' },
    ],
  },

  AIC: {
    name: 'Alternating inference chain',
    level: 'Level 7: chains',
    text: 'The general form of chain reasoning, over (cell, value) nodes. A strong link means ' +
      'that if one end is false, the other is true; a weak link the reverse. A chain that ' +
      'alternates strong, weak, strong and ends on a strong link proves that one of its two ends ' +
      'is true. Follow it: if the first node is false the second is true, so the third is false, ' +
      'and so on to the last. Both ends the same value: cells seeing both drop it. Both ends in ' +
      'one cell: that cell holds one of the two, and its other candidates go.',
    scan: 'Start from a strong link - a two-candidate cell or a conjugate pair - and walk: ' +
      'strong, weak, strong, always ending strong. Chains of two-candidate cells sharing values ' +
      '(XY-chains) are the easiest to follow by hand.',
    diagram: {
      rows: ['r1', 'r5'], cols: ['c1', 'c7'],
      cells: [
        { r: 0, c: 0, m: [4, 9], role: 'pattern' }, { r: 0, c: 1, m: [2, 9], role: 'pattern' },
        { r: 1, c: 0, m: [4, 6], gone: [4] }, { r: 1, c: 1, m: [2, 4], role: 'pattern' },
      ],
    },
    caption: 'Not 4 at r1c1 means 9 there, 2 at r1c7, 4 at r5c7: one end is 4 either way, and ' +
      'r5c1 sees both.',
    relatives: [
      { key: 'TURBOT_FISH', why: 'the two-link case, worth having as a named shape' },
      { key: 'XY_WING', why: 'the three-cell case, likewise' },
      { key: 'SIMPLE_COLOURING', why: 'chains on one value, drawn as two colours' },
    ],
  },

  SIMPLE_COLOURING: {
    name: 'Simple colouring',
    level: 'Level 7: colouring',
    text: 'Follow one value’s strong links and paint the connected cells in two alternating ' +
      'colours; exactly one colour tells the truth. A cell outside the pattern that sees both ' +
      'colours can never hold the value (trap); a colour that puts the value twice into one house ' +
      'is false everywhere at once (wrap).',
    scan: 'Per value, chain the conjugate pairs and alternate colours as you go. Then look ' +
      'twice: for an uncoloured cell seeing both colours, and for two same-coloured cells ' +
      'sharing a house.',
    diagram: {
      rows: ['r1', 'r6'], cols: ['c1', 'c5', 'c9'],
      cells: [
        { r: 0, c: 0, m: [7], role: 'pattern' }, { r: 0, c: 1, m: [7], role: 'alt' },
        { r: 1, c: 0, m: [7], gone: [7] },
        { r: 1, c: 1, m: [7], role: 'pattern' }, { r: 1, c: 2, m: [7], role: 'alt' },
      ],
    },
    caption: 'Strong links on 7, coloured blue and amber; one colour is the truth. r6c1 sees ' +
      'blue up its column and amber along its row, so it is never 7.',
    note: 'Implemented and tested, but deliberately not in this solver’s chain: turbot fish ' +
      'catches the short chains two levels earlier and AIC covers the rest, and measuring it in ' +
      'and out changed nothing - not one extra puzzle solved, not one guess saved.',
    relatives: [
      { key: 'TURBOT_FISH', why: 'the two-link core it grows out of' },
      { key: 'AIC', why: 'the generalisation that covers what colouring finds' },
    ],
  },

  FINNED_FISH: {
    name: 'Finned fish',
    level: 'Level 8: exotic fish',
    text: 'A fish spoiled by extra candidates (the fins) in one of its base lines. Split the ' +
      'argument: if no fin holds the value, the fish is real and clears as usual; if a fin does, ' +
      'everything seeing the fin clears instead. The eliminations that survive are the ones both ' +
      'stories agree on.',
    scan: 'When a promising fish has one row with a candidate too many, check whether that ' +
      'extra sits in the same box as one of the pattern’s corners - the surviving ' +
      'eliminations are confined to that box.',
    diagram: {
      rows: ['r2', 'r4', 'r5', 'r6'],
      cols: ['c1', 'c2', 'c3', 'c4', 'c5', 'c6', 'c7', 'c8', 'c9'],
      seamRows: [0], seamCols: [2, 5],
      cells: [
        { r: 0, c: 2, m: [5], role: 'pattern' }, { r: 0, c: 6, m: [5], role: 'pattern' },
        { r: 1, c: 6, m: [5], gone: [5] },
        { r: 2, c: 6, m: [5], gone: [5] },
        { r: 3, c: 2, m: [5], role: 'pattern' }, { r: 3, c: 6, m: [5], role: 'pattern' },
        { r: 3, c: 7, m: [5], role: 'alt' },
      ],
    },
    caption: 'An X-Wing on 5 but for the fin at r6c8 (amber). Fish real, or fin true: the 5s in ' +
      'column 7 that share the fin’s box go either way.',
    note: 'Implemented and tested, but deliberately not in this solver’s chain: AIC at nine ' +
      'links already finds what it finds, and measuring it in and out changed no outcome on any ' +
      'corpus puzzle.',
    relatives: [
      { key: 'BASIC_FISH', why: 'the unspoiled pattern this degrades gracefully from' },
    ],
  },

  ALS_XZ: {
    name: 'ALS-XZ',
    level: 'Level 8: almost locked sets',
    text: 'An almost locked set is n cells of a house holding n+1 values: one short of locked, ' +
      'so removing any single value locks the rest. Two such sets sharing a restricted value X ' +
      '(spendable in only one of them) must see one of themselves locked, and a locked set uses ' +
      'every value it holds. Their other shared value Z therefore sits inside one of them for ' +
      'certain.',
    scan: 'Almost locked sets are everywhere - any k cells of a house with k+1 values between ' +
      'them, a single bivalue cell included. The work is in the X: a value whose homes in the ' +
      'two sets all see each other.',
    diagram: {
      rows: ['r2', 'r8'], cols: ['c1', 'c2'],
      cells: [
        { r: 0, c: 0, m: [1, 3], role: 'pattern' }, { r: 0, c: 1, m: [1, 2], role: 'pattern' },
        { r: 1, c: 0, m: [2, 3], role: 'alt' },
        { r: 1, c: 1, m: [2, 7], gone: [2] },
      ],
    },
    caption: 'Blue set {1,2,3}, amber set {2,3}; their 3s share column 1, so 3 locks at most ' +
      'one of them. Whichever locks contains a 2 - and r8c2 sees every 2 in both.',
    note: 'Implemented and tested, but deliberately not in this solver’s chain: AIC covers ' +
      'what it finds on the corpus, and measuring it in and out saved not a single guess.',
    relatives: [
      { key: 'XYZ_WING', why: 'the smallest ALS pattern, seen before the general idea' },
      { key: 'AIC', why: 'grouped chains subsume the two-set argument' },
    ],
  },

  GUESS: {
    name: 'A guess: the honest boundary',
    level: 'Level 9: methods of last resort',
    text: 'Every proper puzzle is fully deducible in principle, but no fixed set of techniques ' +
      'is complete: each level solves puzzles the one above cannot, without end. When this ' +
      'solver’s twenty techniques all stall, it assumes a value in the cell with the fewest ' +
      'candidates and follows the consequences, withdrawing the assumption if it collapses into ' +
      'contradiction. That is search, and the site does not dress it up as deduction: a step ' +
      'marked as a guess is a guess, and the branches that failed are not shown, because they ' +
      'were assumptions that did not survive rather than reasoning.',
    scan: 'For a person: pick a cell with two candidates, pencil one in lightly, and follow the ' +
      'consequences - ready to erase back to the fork. The honest name for this is trial, and ' +
      'everyone who solves hard puzzles does it.',
    relatives: [
      { key: 'AIC', why: 'the last rung where following consequences still counts as deduction' },
    ],
  },
};
