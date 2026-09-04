// The player: enter a puzzle, ask the api to solve it, then replay the steps client-side.
// State is recomputed from the givens up to the current step - the api's steps are deltas, and
// the server verifies against its own replay that applying them reproduces the solution.
'use strict';

const $ = (id) => document.getElementById(id);
const ALL = 0x1ff; // nine candidate bits, mirroring the solver's representation

// ---------- entry grid ----------

const entryCells = [];

function buildEntryGrid() {
  const grid = $('entry-grid');
  for (let i = 0; i < 81; i++) {
    const cell = document.createElement('div');
    cell.className = 'cell' + boxEdges(i);
    cell.tabIndex = 0;
    cell.dataset.index = i;
    cell.addEventListener('keydown', onEntryKey);
    cell.addEventListener('focus', () => cell.classList.add('focused'));
    grid.appendChild(cell);
    entryCells.push(cell);
  }
}

function boxEdges(i) {
  const row = Math.floor(i / 9), col = i % 9;
  return (col === 2 || col === 5 ? ' box-right' : '') + (row === 2 || row === 5 ? ' box-bottom' : '');
}

function onEntryKey(event) {
  const cell = event.currentTarget;
  const i = Number(cell.dataset.index);
  if (event.key >= '1' && event.key <= '9') {
    cell.textContent = event.key;
    focusEntry(i + 1);
  } else if (event.key === 'Backspace' || event.key === 'Delete' || event.key === '0' || event.key === ' ') {
    cell.textContent = '';
  } else if (event.key === 'ArrowRight') focusEntry(i + 1);
  else if (event.key === 'ArrowLeft') focusEntry(i - 1);
  else if (event.key === 'ArrowDown') focusEntry(i + 9);
  else if (event.key === 'ArrowUp') focusEntry(i - 9);
  else return;
  event.preventDefault();
  $('compact').value = entryCompact();
}

function focusEntry(i) {
  if (i >= 0 && i < 81) entryCells[i].focus();
}

function entryCompact() {
  return entryCells.map((cell) => cell.textContent || '.').join('');
}

function setEntry(compact) {
  const cleaned = (compact || '').replace(/\s/g, '');
  entryCells.forEach((cell, i) => {
    const ch = cleaned[i];
    cell.textContent = ch >= '1' && ch <= '9' ? ch : '';
  });
}

// ---------- solving ----------

let solve = null;   // the api response
let states = null;  // states[k] = 81 cells after step k; states[0] = the givens
let current = 0;    // 0 = before any step; k = after step k
let playing = null;

async function requestSolve() {
  const puzzle = $('compact').value.trim() || entryCompact();
  $('setup-error').hidden = true;
  let response;
  try {
    response = await fetch('/solve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ puzzle, allowGuessing: $('allow-guessing').checked }),
    });
  } catch (unreachable) {
    return showSetupError('The solver is not reachable. Is the api running?');
  }
  const body = await response.json();
  if (body.outcome === 'invalid' || !body.steps || body.steps.length === 0) {
    return showSetupError(body.message || 'That puzzle could not be solved.');
  }
  startPlayer(body);
}

function showSetupError(message) {
  const error = $('setup-error');
  error.textContent = message;
  error.hidden = false;
}

// ---------- replay ----------

function computeStates(givens, steps) {
  const start = [...givens].map((ch) => (ch >= '1' && ch <= '9' ? { value: +ch, given: true } : { mask: ALL }));
  const all = [start];
  let grid = start;
  for (const step of steps) {
    grid = grid.slice();
    const i = step.at.row * 9 + step.at.column;
    if (step.kind === 'elimination') {
      let mask = grid[i].mask;
      for (const v of step.values) mask &= ~(1 << (v - 1));
      grid[i] = { mask };
    } else {
      grid[i] = { value: step.value };
    }
    all.push(grid);
  }
  return all;
}

function startPlayer(body) {
  solve = body;
  states = computeStates(body.givens, body.steps);
  current = 0;
  $('setup').hidden = true;
  $('player').hidden = false;
  $('scrub').max = body.steps.length;

  const banner = $('banner');
  if (body.outcome === 'stalled') {
    banner.textContent = body.message + ' You can still step through everything it could deduce.';
    banner.classList.remove('bad');
    banner.hidden = false;
  } else if (body.guesses > 0) {
    banner.textContent = 'This puzzle outran all twenty techniques: ' + body.guesses +
      (body.guesses === 1 ? ' step is an assumption' : ' steps are assumptions') +
      ', shown honestly as guesses - watch for the amber steps.';
    banner.classList.remove('bad');
    banner.hidden = false;
  } else {
    banner.hidden = true;
  }

  buildPlayGrid();
  render();
}

function buildPlayGrid() {
  const grid = $('play-grid');
  grid.innerHTML = '';
  for (let i = 0; i < 81; i++) {
    const cell = document.createElement('div');
    cell.className = 'cell' + boxEdges(i);
    grid.appendChild(cell);
  }
}

// Shows the grid BEFORE the current step and overlays what the step does to it, so an
// elimination's candidates are still visible - struck through - at the moment they go.
function render() {
  const step = current > 0 ? solve.steps[current - 1] : null;
  const grid = states[current > 0 ? current - 1 : 0];
  const cells = $('play-grid').children;
  const becauseSet = new Set(step ? step.because.map((c) => c.row * 9 + c.column) : []);
  const targetIndex = step ? step.at.row * 9 + step.at.column : -1;

  for (let i = 0; i < 81; i++) {
    const cell = cells[i];
    const state = grid[i];
    cell.className = 'cell' + boxEdges(i);
    cell.innerHTML = '';
    if (state.value !== undefined) {
      cell.textContent = state.value;
      if (state.given) cell.classList.add('given');
    } else if (i === targetIndex && step.kind !== 'elimination') {
      cell.textContent = step.value;
      cell.classList.add(step.kind === 'guess' ? 'guessed-now' : 'placed-now');
    } else {
      cell.appendChild(renderMarks(state.mask, i === targetIndex ? step.values : []));
    }
    if (becauseSet.has(i)) cell.classList.add('because');
    if (i === targetIndex) cell.classList.add(step.kind === 'guess' ? 'guess-target' : 'target');
  }

  renderCards(step);
  $('scrub').value = current;
  $('step-counter').textContent = current === 0
    ? 'The puzzle as given - press play, or step through it'
    : 'Step ' + current + ' of ' + solve.steps.length;
  $('btn-play').textContent = playing ? '❚❚ pause' : '▶︎ play';
}

function renderMarks(mask, removed) {
  const marks = document.createElement('div');
  marks.className = 'marks';
  for (let v = 1; v <= 9; v++) {
    const span = document.createElement('span');
    if (removed.includes(v)) {
      span.textContent = v;
      span.className = 'gone';
    } else if (mask & (1 << (v - 1))) {
      span.textContent = v;
    }
    marks.appendChild(span);
  }
  return marks;
}

function techniqueTitle(step) {
  const lesson = TECHNIQUES[step.technique];
  return lesson ? lesson.name : step.technique;
}

function renderCards(step) {
  const stepCard = $('step-card');
  if (!step) {
    $('step-title').textContent = 'The givens';
    $('step-explanation').textContent =
      'Every empty cell starts with all nine pencil marks. Each step that follows removes marks ' +
      'or places a value - and says why.';
    stepCard.classList.remove('guess-step');
    $('technique-card').hidden = true;
    return;
  }
  const what = step.kind === 'elimination'
    ? 'not ' + step.values.join(', ')
    : '= ' + step.value;
  $('step-title').textContent = techniqueTitle(step) + ': r' + (step.at.row + 1) + 'c' + (step.at.column + 1) + ' ' + what;
  $('step-explanation').textContent = step.explanation;
  stepCard.classList.toggle('guess-step', step.kind === 'guess');

  const lesson = TECHNIQUES[step.technique];
  $('technique-card').hidden = !lesson;
  if (lesson) {
    $('technique-name').textContent = lesson.name;
    $('technique-level').textContent = lesson.level;
    $('technique-text').textContent = lesson.text;
    const example = $('technique-example');
    example.hidden = !lesson.example;
    if (lesson.example) example.textContent = lesson.example;
  }
}

// ---------- controls ----------

const BASICS = new Set(['FULL_HOUSE', 'NAKED_SINGLE', 'PEER_ELIMINATION']);

function goTo(step) {
  current = Math.max(0, Math.min(solve.steps.length, step));
  render();
}

function stopPlaying() {
  if (playing) { clearInterval(playing); playing = null; }
}

function togglePlay() {
  if (playing) { stopPlaying(); render(); return; }
  playing = setInterval(() => {
    if (current >= solve.steps.length) { stopPlaying(); render(); return; }
    goTo(current + 1);
  }, 700);
  render();
}

function skipBasics() {
  for (let k = current + 1; k <= solve.steps.length; k++) {
    if (!BASICS.has(solve.steps[k - 1].technique)) return goTo(k);
  }
  goTo(solve.steps.length);
}

function wire() {
  buildEntryGrid();
  document.querySelectorAll('[data-sample]').forEach((button) =>
    button.addEventListener('click', () => {
      $('compact').value = button.dataset.sample;
      setEntry(button.dataset.sample);
    }));
  $('compact').addEventListener('input', () => setEntry($('compact').value));
  $('solve').addEventListener('click', requestSolve);
  $('clear').addEventListener('click', () => { $('compact').value = ''; setEntry(''); });
  $('btn-first').addEventListener('click', () => { stopPlaying(); goTo(0); });
  $('btn-back').addEventListener('click', () => { stopPlaying(); goTo(current - 1); });
  $('btn-next').addEventListener('click', () => { stopPlaying(); goTo(current + 1); });
  $('btn-last').addEventListener('click', () => { stopPlaying(); goTo(solve.steps.length); });
  $('btn-skip').addEventListener('click', () => { stopPlaying(); skipBasics(); });
  $('btn-play').addEventListener('click', togglePlay);
  $('scrub').addEventListener('input', () => { stopPlaying(); goTo(Number($('scrub').value)); });
  $('btn-restart').addEventListener('click', () => {
    stopPlaying();
    $('player').hidden = true;
    $('setup').hidden = false;
  });
  document.addEventListener('keydown', (event) => {
    if ($('player').hidden || event.target.tagName === 'TEXTAREA') return;
    if (event.key === 'ArrowRight') { stopPlaying(); goTo(current + 1); event.preventDefault(); }
    if (event.key === 'ArrowLeft') { stopPlaying(); goTo(current - 1); event.preventDefault(); }
    if (event.key === ' ') { togglePlay(); event.preventDefault(); }
  });
}

wire();
