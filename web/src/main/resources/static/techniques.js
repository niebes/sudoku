// The lesson behind each technique, condensed from docs/solving-techniques.md - the levelled
// reference this site teaches from. Each entry: what the technique is in general; the step card
// next to it says what it did on this grid.
const TECHNIQUES = {
  FULL_HOUSE: {
    name: 'Full house',
    level: 'Level 1: singles',
    text: 'A house (a row, column or box) with exactly one unsolved cell. Eight of its nine ' +
      'values are placed, so the gap takes the ninth. The cheapest check in the game, and the ' +
      'first thing a person looks for.',
    example: 'row:  4  1  7  9  {6}  2  8  5  3      ->  the gap must be 6',
  },
  NAKED_SINGLE: {
    name: 'Naked single',
    level: 'Level 1: singles',
    text: 'A cell down to a single candidate: every other value already appears somewhere in ' +
      'its row, column or box. Nothing is left to decide, so the cell takes that value. The ' +
      'singles are where digits actually get placed; every fancier technique below exists to ' +
      'create work for this level.',
    example: '{5}  ->  5',
  },
  HIDDEN_SINGLE: {
    name: 'Hidden single',
    level: 'Level 1: singles',
    text: 'A value that fits in only one cell of a house, even though that cell still has other ' +
      'candidates. The single hides behind them: find it by asking where the value can go rather ' +
      'than what the cell can be. This is what crosshatching finds, and it carries most easy ' +
      'puzzles on its own.',
    example: '        c1 c2 c3\n r1      .  .  .      7 fits only at r2c2 in this box,\n' +
      ' r2      .  #  .      so r2c2 = 7 - whatever else it could hold\n r3      .  .  .',
  },
  PEER_ELIMINATION: {
    name: 'Peer elimination',
    level: 'Level 1: singles',
    text: 'The bookkeeping behind everything else. A value placed in a cell cannot appear again ' +
      'in that cell’s row, column or box, so the solver crosses it off the pencil marks of ' +
      'the 20 cells it sees. Every technique above reads the pencil marks this maintains.',
  },
  POINTING: {
    name: 'Pointing (locked candidates)',
    level: 'Level 2: locked candidates',
    text: 'Inside one box, every place a value can still go lies on a single row or column. The ' +
      'box must contain that value somewhere, so the value has to sit in that overlap, and the ' +
      'solver can cross it off everywhere else along the line. Two rules like this one carry ' +
      'most medium-difficulty puzzles.',
    example: '        c1 c2 c3 | c4 c5 c6 | c7 c8 c9\n r1      .  .  . |\n' +
      ' r2      #  .  # |  x  .  x |  .  x  .\n r3      .  .  . |\n\n' +
      'In box 1, 3 fits only on row 2 - so row 2’s 3 is inside box 1,\n' +
      'and every 3 elsewhere on row 2 goes.',
  },
  CLAIMING: {
    name: 'Claiming (locked candidates)',
    level: 'Level 2: locked candidates',
    text: 'The mirror image of pointing. Along one row or column, every place a value can go ' +
      'lies inside a single box. The line must put its value there, which uses up that box’s ' +
      'copy, so the value leaves every other cell of the box.',
    example: '        c1 c2 c3 | c4 c5 c6\n r4      #  .  # |  .  .  .   <- row 4’s 6 fits only in box 4\n' +
      ' r5      x  x  x |\n r6      x  .  x |',
  },
  NAKED_SUBSET: {
    name: 'Naked pair / triple / quad',
    level: 'Level 3: subsets',
    text: 'k cells of one house whose candidates, taken together, come to exactly k values. ' +
      'Those cells use those values up between them (no single cell needs to hold all of them), ' +
      'so the values vanish from every other cell of the house. Watch what it leaves behind: ' +
      'collapsing one cell to a single candidate hands the grid straight back to the singles.',
    example: ' {2,7}   {2,9}   {7,9}   {2,4,7}   {1,9,5}\n   #       #       #        x         x\n\n' +
      'Three cells sharing {2,7,9}: 2, 7 and 9 leave the rest of the house,\n' +
      'so {2,4,7} becomes {4} - a naked single.',
  },
  HIDDEN_SUBSET: {
    name: 'Hidden pair / triple / quad',
    level: 'Level 3: subsets',
    text: 'The dual of the naked subset: k values of a house that fit in only k cells. Those ' +
      'cells must hold those values between them, so everything else pencilled into them goes. ' +
      'In a house with n open cells, a naked k-subset and a hidden (n-k)-subset are the same ' +
      'fact seen from opposite sides.',
    example: ' {2,5,8}  {2,5,8}  {1,3,5,8}  {1,3,5,8}  {2,5,8}\n                       #          #\n\n' +
      '1 and 3 fit only in the marked cells, so both collapse to {1,3}.',
  },
  BASIC_FISH: {
    name: 'X-Wing / Swordfish / Jellyfish',
    level: 'Level 4: basic fish',
    text: 'The first pattern that spans the whole grid, and it works on a single value. Take N ' +
      'rows in which the value is confined to the same N columns. Each of those rows needs the ' +
      'value, and every place it can go lies in one of the columns: N values into N columns, one ' +
      'each. The columns are spoken for, so the value goes nowhere else in them. N=2 is the ' +
      'X-Wing, 3 the Swordfish, 4 the Jellyfish; rows and columns swap freely.',
    example: '      c3       c7\n r2    #  ....  #    <- 4 fits only in c3, c7\n' +
      ' r5    #  ....  #    <- 4 fits only in c3, c7\n\n' +
      'The 4s of rows 2 and 5 occupy c3 and c7 in some order -\nevery other 4 in those columns goes.',
  },
  TURBOT_FISH: {
    name: 'Turbot fish',
    level: 'Level 5: single-digit patterns',
    text: 'Built from conjugate pairs: houses where a value has exactly two homes, so one of ' +
      'them is the value. Take two such pairs, joined by two ends that see each other. Those two ' +
      'ends cannot both be true, so at least one of the far ends must be, and any cell seeing ' +
      'both far ends can drop the value. The skyscraper and the 2-string kite are this same ' +
      'argument with the links in particular places.',
    example: ' r2    #₁ .... #₂      <- 9 only here in row 2\n r5    #₁ .... .  #₂   <- 9 only here in row 5\n\n' +
      'The two left ends share a column, so they are not both 9 -\nat least one right end is.',
  },
  XY_WING: {
    name: 'XY-Wing',
    level: 'Level 5: wings',
    text: 'Three cells with two candidates each: a pivot {x,y}, and two pincers {x,z} and {y,z} ' +
      'that each see the pivot. Whichever value the pivot takes, it strips that value from one ' +
      'pincer and forces it onto z. One of the pincers is z. You cannot say which, and you do ' +
      'not need to: any cell that sees both pincers cannot be z.',
    example: '        c2                  c7\n r2   {5,8} . . . . . .  {3,5}     pivot, pincer A\n' +
      ' r6   {3,8} . . . . . .    x       pincer B, elimination\n\n' +
      'Pivot 5 -> A is 3.  Pivot 8 -> B is 3.  Either way a 3 stares at r6c7.',
  },
  XYZ_WING: {
    name: 'XYZ-Wing',
    level: 'Level 5: wings',
    text: 'Like the XY-Wing, but the pivot keeps the shared value too: pivot {x,y,z}, pincers ' +
      '{x,z} and {y,z}. Now all three cells could be z, which weakens the conclusion: only a ' +
      'cell that sees all three of them can drop z.',
  },
  W_WING: {
    name: 'W-Wing',
    level: 'Level 5: wings',
    text: 'Two cells holding the same pair {x,y} that do not see each other, bridged by a strong ' +
      'link on x: a house where x has exactly two places, one end seeing each pair cell. One end ' +
      'of the link is x, and whichever it is, the pair cell it sees cannot be x, so it is y. One ' +
      'of the pair is y either way, and any cell seeing both drops y.',
  },
  EMPTY_RECTANGLE: {
    name: 'Empty rectangle',
    level: 'Level 5: single-digit patterns',
    text: 'Inside one box, every place for a value fits on one row plus one column, leaving the ' +
      'opposite 2×2 corner empty. That cross shape is itself a strong statement: the box’s ' +
      'value is on the row arm or the column arm. Combined with a conjugate pair elsewhere, it ' +
      'pins a cell that would push the value off both arms at once.',
  },
  UNIQUE_RECTANGLE: {
    name: 'Unique rectangle',
    level: 'Level 6: uniqueness',
    text: 'The one family that reasons from the puzzle rather than the grid: a published puzzle ' +
      'has exactly one solution. Four cells on two rows, two columns and two boxes, three of them ' +
      'holding the identical pair. If the fourth held just that pair too, the two values could ' +
      'swap around the rectangle and both grids would satisfy every rule: two solutions. So the ' +
      'fourth corner keeps neither value of the pair.',
    example: '          c1        c4\n r1     {3,7}     {3,7}\n r2     {3,7}    {3,7,5}   ->  r2c4 = 5',
  },
  BUG_PLUS_ONE: {
    name: 'BUG+1',
    level: 'Level 6: uniqueness',
    text: 'BUG stands for bivalue universal grave: every unsolved cell a pair, every value left ' +
      'with exactly two homes per house. Such a grid always has an even number of solutions, so ' +
      'a proper puzzle can never reach one. When a single cell stands between the grid and a ' +
      'grave, that cell must hold the one candidate whose removal would dig it.',
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
    example: ' r1c1{4,9} --(9)-- r1c7{2,9} --(2)-- r5c7{2,4}\n\n' +
      'Not 4 at r1c1 -> 9 -> r1c7 is 2 -> r5c7 is 4.\nOne end is 4 either way; r5c1 sees both.',
  },
  SIMPLE_COLOURING: {
    name: 'Simple colouring',
    level: 'Level 7: colouring',
    text: 'Follow one value’s strong links and paint the connected cells in two alternating ' +
      'colours; exactly one colour tells the truth. A cell outside the pattern that sees both ' +
      'colours can never hold the value (trap); a colour that puts the value twice into one house ' +
      'is false everywhere at once (wrap).',
  },
  FINNED_FISH: {
    name: 'Finned fish',
    level: 'Level 8: exotic fish',
    text: 'A fish spoiled by extra candidates (the fins) in one of its base lines. Split the ' +
      'argument: if no fin holds the value, the fish is real and clears as usual; if a fin does, ' +
      'everything seeing the fin clears instead. The eliminations that survive are the ones both ' +
      'stories agree on.',
  },
  ALS_XZ: {
    name: 'ALS-XZ',
    level: 'Level 8: almost locked sets',
    text: 'An almost locked set is n cells of a house holding n+1 values: one short of locked, ' +
      'so removing any single value locks the rest. Two such sets sharing a restricted value X ' +
      '(spendable in only one of them) must see one of themselves locked, and a locked set uses ' +
      'every value it holds. Their other shared value Z therefore sits inside one of them for ' +
      'certain.',
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
  },
};
