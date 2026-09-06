// The technique library: every technique the solver knows, browsable without a puzzle in
// flight. Built from TECHNIQUES (the lessons) and TECHNIQUE_EXAMPLES (where each technique
// actually fires on the corpus, found at build time by the solver itself - see-it-live jumps
// the player straight to that step, so the example is generated, never drawn).
'use strict';

function buildLibrary() {
  const container = document.getElementById('library-entries');
  let currentLevel = null;

  Object.keys(TECHNIQUES).forEach((key) => {
    const lesson = TECHNIQUES[key];

    if (lesson.level !== currentLevel) {
      currentLevel = lesson.level;
      const heading = document.createElement('h2');
      heading.className = 'library-level';
      heading.textContent = levelTitle(currentLevel);
      container.appendChild(heading);
    }

    container.appendChild(libraryEntry(key, lesson));
  });
}

// 'Level 3: subsets' -> 'Level 3 — Subsets'
function levelTitle(level) {
  const split = level.indexOf(':');
  const label = level.slice(split + 2);
  return level.slice(0, split) + ' — ' + label.charAt(0).toUpperCase() + label.slice(1);
}

function libraryEntry(key, lesson) {
  const article = document.createElement('article');
  article.className = 'tech-entry card';
  article.id = 'tech-' + key;

  const heading = document.createElement('h3');
  heading.textContent = lesson.name;
  article.appendChild(heading);

  const text = document.createElement('p');
  text.textContent = lesson.text;
  article.appendChild(text);

  if (lesson.diagram) {
    const figure = document.createElement('figure');
    figure.className = 'tech-figure';
    figure.appendChild(renderMiniGrid(lesson.diagram));
    if (lesson.caption) {
      const caption = document.createElement('figcaption');
      caption.textContent = lesson.caption;
      figure.appendChild(caption);
    }
    article.appendChild(figure);
  }

  if (lesson.scan) {
    const scan = document.createElement('p');
    const label = document.createElement('strong');
    label.textContent = 'How to spot it. ';
    scan.appendChild(label);
    scan.appendChild(document.createTextNode(lesson.scan));
    article.appendChild(scan);
  }

  if (lesson.note) {
    const note = document.createElement('p');
    note.className = 'tech-note';
    note.textContent = lesson.note;
    article.appendChild(note);
  }

  if (lesson.relatives && lesson.relatives.length > 0) {
    const related = document.createElement('p');
    related.className = 'tech-related';
    related.appendChild(document.createTextNode('Related: '));
    lesson.relatives.forEach((relative, i) => {
      if (i > 0) related.appendChild(document.createTextNode(' · '));
      const link = document.createElement('a');
      link.href = '#technique/' + relative.key;
      link.textContent = TECHNIQUES[relative.key].name;
      related.appendChild(link);
      related.appendChild(document.createTextNode(' — ' + relative.why));
    });
    article.appendChild(related);
  }

  const example = typeof TECHNIQUE_EXAMPLES !== 'undefined' && TECHNIQUE_EXAMPLES[key];
  if (example) {
    const live = document.createElement('button');
    live.textContent = 'See it live ▸';
    live.title = 'Solve a real puzzle and jump to the step where this technique fires';
    live.addEventListener('click', () => playExample(key, example));
    article.appendChild(live);
  }

  return article;
}

function showLibraryEntry(key) {
  const entry = document.getElementById('tech-' + key);
  if (entry) entry.scrollIntoView({ block: 'start' });
}
