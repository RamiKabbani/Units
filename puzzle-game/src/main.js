import { SlidingPuzzleGame } from './game.js';
import { solveOptimallyAStar, solveHeuristic, isSolved } from './solver.js';
import { createConfetti } from './confetti.js';
import { sleep } from './utils.js';

const boardEl = document.getElementById('board');
const moveCountEl = document.getElementById('moveCount');
const timeEl = document.getElementById('time');
const statusEl = document.getElementById('status');

const sizeSelect = document.getElementById('size');
const shuffleBtn = document.getElementById('shuffleBtn');
const resetBtn = document.getElementById('resetBtn');
const undoBtn = document.getElementById('undoBtn');
const redoBtn = document.getElementById('redoBtn');
const solveBtn = document.getElementById('solveBtn');
const stopSolveBtn = document.getElementById('stopSolveBtn');

const canvas = document.getElementById('confetti');
const confetti = createConfetti(canvas);

let abortController = null;

const game = new SlidingPuzzleGame({
  boardEl,
  moveCountEl,
  timeEl,
  statusEl,
  onWin: () => confetti.celebrate(),
  getSize: () => parseInt(sizeSelect.value, 10),
});

sizeSelect.addEventListener('change', async () => {
  await game.reset();
});

shuffleBtn.addEventListener('click', () => game.shuffle(180));
resetBtn.addEventListener('click', () => game.reset());
undoBtn.addEventListener('click', () => game.undo());
redoBtn.addEventListener('click', () => game.redo());

async function playSolution(path) {
  for (const toIndex of path) {
    await game.tryMove(toIndex, false);
    await sleep(90);
  }
}

solveBtn.addEventListener('click', async () => {
  if (abortController) abortController.abort();
  abortController = new AbortController();

  const state = game.state.slice();
  const size = game.size;
  statusEl.textContent = 'Solving…';

  try {
    let path = null;
    if (size <= 3) path = await solveOptimallyAStar(state, size, abortController.signal);
    else path = await solveHeuristic(state, size, abortController.signal);

    if (path && path.length) {
      await playSolution(path);
      statusEl.textContent = isSolved(game.state) ? 'Solved' : 'In play';
    } else {
      statusEl.textContent = 'No solution found';
    }
  } catch (e) {
    if (e?.name === 'AbortError') statusEl.textContent = 'Solve stopped';
    else statusEl.textContent = 'Solve failed';
  }
});

stopSolveBtn.addEventListener('click', () => {
  abortController?.abort();
});

// Initialize with a light shuffle so the board animates into place
(async () => {
  await game.shuffle(20);
})();