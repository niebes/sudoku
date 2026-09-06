// Renders the small teaching diagrams as real grids - the same cells, pencil marks and colour
// language the player uses, instead of a second notation the reader would have to learn.
//
// A spec describes a fragment, not necessarily 9x9:
//   {
//     rows: ['r1', 'r2'],  cols: ['c1', 'c2', 'c3'],   // axis labels; length sets the size
//     seamRows: [0], seamCols: [2],                     // draw a box seam AFTER this index
//     cells: [ { r, c, v, given, m: [..], gone: [..], role } ],
//   }
// r and c index into rows/cols. A cell may hold a value `v` (bold if `given`) or pencil marks
// `m`, with `gone` listing the marks the technique removes. `role` colours the cell the way the
// player does: 'pattern' = the cells that prove it (blue), 'target' = what changes (green),
// 'alt' = the second colour or the fin/assumption side (amber). Unlisted cells stay blank.
'use strict';

function renderMiniGrid(spec) {
  const wrap = document.createElement('div');
  wrap.className = 'mini-grid-wrap';

  const grid = document.createElement('div');
  grid.className = 'mini-grid';
  const hasRowLabels = spec.rows.some((label) => label !== '');
  grid.style.gridTemplateColumns =
    (hasRowLabels ? 'auto ' : '') + 'repeat(' + spec.cols.length + ', var(--mini-cell))';

  const byPosition = new Map();
  (spec.cells || []).forEach((cell) => byPosition.set(cell.r + ',' + cell.c, cell));

  // Column labels along the top, over an empty corner when there are row labels too.
  if (spec.cols.some((label) => label !== '')) {
    if (hasRowLabels) grid.appendChild(miniLabel(''));
    spec.cols.forEach((label) => grid.appendChild(miniLabel(label)));
  }

  spec.rows.forEach((rowLabel, r) => {
    if (hasRowLabels) grid.appendChild(miniLabel(rowLabel));
    spec.cols.forEach((unusedLabel, c) => {
      const el = document.createElement('div');
      el.className = 'cell mini-cell';
      if ((spec.seamCols || []).includes(c)) el.classList.add('box-right');
      if ((spec.seamRows || []).includes(r)) el.classList.add('box-bottom');
      if (r === 0) el.classList.add('edge-top');
      if (c === 0) el.classList.add('edge-left');
      if (c === spec.cols.length - 1) el.classList.add('edge-right');
      if (r === spec.rows.length - 1) el.classList.add('edge-bottom');

      const cell = byPosition.get(r + ',' + c);
      if (cell) {
        if (cell.role === 'pattern') el.classList.add('because');
        if (cell.role === 'target') el.classList.add('target');
        if (cell.role === 'alt') el.classList.add('guess-target');
        if (cell.v !== undefined) {
          el.textContent = cell.v;
          if (cell.given) el.classList.add('given');
        } else if (cell.m) {
          el.appendChild(miniMarks(cell.m, cell.gone || []));
        }
      }
      grid.appendChild(el);
    });
  });

  wrap.appendChild(grid);
  return wrap;
}

function miniLabel(text) {
  const el = document.createElement('div');
  el.className = 'mini-label';
  el.textContent = text;
  return el;
}

// Marks keep their home position in the 3x3 layout, exactly as the player draws them, so a
// diagram's 4 sits where the player's 4 sits.
function miniMarks(present, gone) {
  const marks = document.createElement('div');
  marks.className = 'marks';
  for (let v = 1; v <= 9; v++) {
    const span = document.createElement('span');
    if (gone.includes(v)) {
      span.textContent = v;
      span.className = 'gone';
    } else if (present.includes(v)) {
      span.textContent = v;
    }
    marks.appendChild(span);
  }
  return marks;
}
