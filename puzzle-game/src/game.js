import { animate, formatTime, createEl } from './utils.js';
import { isSolved, getMoves, applyMove, solvable, indexOfZero } from './solver.js';

export class SlidingPuzzleGame {
  constructor({ boardEl, moveCountEl, timeEl, statusEl, onWin, getSize }) {
    this.boardEl = boardEl;
    this.moveCountEl = moveCountEl;
    this.timeEl = timeEl;
    this.statusEl = statusEl;
    this.onWin = onWin;
    this.getSize = getSize;

    this.size = this.getSize();
    this.state = this.createGoalState(this.size);
    this.initialState = this.state.slice();
    this.tiles = new Map(); // value -> element

    this.moves = []; // stack of toIndex for user moves
    this.redoStack = [];

    this.isAnimating = false;
    this.startTime = 0;
    this.elapsed = 0;
    this.timerRaf = 0;

    this.renderBoard();
    this.updateHUD();
    this.attachEvents();
  }

  createGoalState(size) {
    const arr = [];
    for (let i = 1; i < size * size; i++) arr.push(i);
    arr.push(0);
    return arr;
  }

  resetTimer() {
    cancelAnimationFrame(this.timerRaf);
    this.elapsed = 0;
    this.startTime = performance.now();
    const tick = () => {
      this.elapsed = (performance.now() - this.startTime) / 1000;
      this.timeEl.textContent = formatTime(this.elapsed);
      this.timerRaf = requestAnimationFrame(tick);
    };
    this.timerRaf = requestAnimationFrame(tick);
  }

  stopTimer() { cancelAnimationFrame(this.timerRaf); }

  renderBoard() {
    const size = this.size;
    this.boardEl.innerHTML = '';
    this.boardEl.dataset.size = String(size);

    const gap = 7;
    const w = this.boardEl.clientWidth;
    const tileSize = (w - gap * (size + 1)) / size;

    const createTile = (value, idx) => {
      const r = Math.floor(idx / size);
      const c = idx % size;
      const el = createEl('button', 'tile', { 'data-value': String(value), 'aria-label': String(value), 'tabindex': 0 });
      if (value === 0) el.classList.add('blank');
      const inner = createEl('div', 'label', { text: value === 0 ? '' : String(value) });
      el.appendChild(inner);
      this.positionTile(el, r, c, tileSize, gap);
      this.boardEl.appendChild(el);
      return el;
    };

    this.tiles.clear();
    for (let i = 0; i < this.state.length; i++) {
      const v = this.state[i];
      const el = createTile(v, i);
      this.tiles.set(v, el);
    }
  }

  positionTile(el, row, col, tileSize, gap) {
    const x = gap + col * (tileSize + gap);
    const y = gap + row * (tileSize + gap);
    el.style.width = `${tileSize}px`;
    el.style.height = `${tileSize}px`;
    el.style.transform = `translate(${x}px, ${y}px)`;
  }

  updateHUD() {
    this.moveCountEl.textContent = String(this.moves.length);
    const ready = isSolved(this.state) ? 'Solved' : 'In play';
    this.statusEl.textContent = ready;
  }

  async animateMove(toIndex) {
    if (this.isAnimating) return false;
    const size = this.size;
    const i0 = indexOfZero(this.state);
    const fromIndex = toIndex;
    const movingValue = this.state[fromIndex];
    const movingEl = this.tiles.get(movingValue);
    const blankEl = this.tiles.get(0);
    const gap = 7;

    const w = this.boardEl.clientWidth;
    const tileSize = (w - gap * (size + 1)) / size;

    const fromRow = Math.floor(fromIndex / size);
    const fromCol = fromIndex % size;
    const toRow = Math.floor(i0 / size);
    const toCol = i0 % size;

    const fromX = gap + fromCol * (tileSize + gap);
    const fromY = gap + fromRow * (tileSize + gap);
    const toX = gap + toCol * (tileSize + gap);
    const toY = gap + toRow * (tileSize + gap);

    this.isAnimating = true;
    let done;
    await new Promise(res => {
      done = animate({
        duration: 220,
        update: p => {
          movingEl.style.transform = `translate(${fromX + (toX - fromX) * p}px, ${fromY + (toY - fromY) * p}px)`;
        },
        complete: res,
      });
    });
    // Swap positions in DOM: move blank where moving was
    blankEl.style.transform = `translate(${fromX}px, ${fromY}px)`;
    this.isAnimating = false;
    return true;
  }

  async tryMove(toIndex, record = true) {
    if (this.isAnimating) return false;
    const size = this.size;
    const i0 = indexOfZero(this.state);
    const legal = getMoves(this.state, size);
    if (!legal.includes(toIndex)) return false;

    await this.animateMove(toIndex);
    this.state = applyMove(this.state, toIndex);

    if (record) {
      this.moves.push({ fromIndex: i0, toIndex });
      this.redoStack.length = 0;
    }

    this.updateHUD();

    if (isSolved(this.state)) {
      this.statusEl.textContent = 'Solved';
      this.stopTimer();
      this.onWin?.();
    }
    return true;
  }

  async shuffle(depth = 200) {
    // Generate a random solvable state by random legal moves
    this.size = this.getSize();
    this.state = this.createGoalState(this.size);
    this.initialState = this.state.slice();
    this.moves = [];
    this.redoStack = [];
    this.renderBoard();
    this.updateHUD();
    this.resetTimer();

    let prev = -1;
    for (let i = 0; i < depth; i++) {
      const legal = getMoves(this.state, this.size).filter(idx => idx !== prev);
      const choice = legal[Math.floor(Math.random() * legal.length)];
      // Skip animation to quickly scramble, but animate last few moves for flair
      if (i >= depth - 8) await this.animateMove(choice);
      this.state = applyMove(this.state, choice);
      prev = indexOfZero(this.state);
    }

    // Ensure solvable just in case
    if (!solvable(this.state, this.size)) {
      const i0 = indexOfZero(this.state);
      const a = i0 === 0 ? 1 : 0;
      const b = i0 === 1 ? 2 : 1;
      [this.state[a], this.state[b]] = [this.state[b], this.state[a]];
    }

    this.updateHUD();
    this.statusEl.textContent = 'In play';
  }

  async reset() {
    this.state = this.initialState.slice();
    this.moves = [];
    this.redoStack = [];
    this.renderBoard();
    this.updateHUD();
    this.stopTimer();
    this.timeEl.textContent = '0:00';
    this.statusEl.textContent = 'Ready';
  }

  async undo() {
    if (this.moves.length === 0 || this.isAnimating) return;
    const last = this.moves.pop();
    // Currently, zero is at last.toIndex; to undo, move the tile located at last.fromIndex into zero
    await this.tryMove(last.fromIndex, false);
    this.redoStack.push(last);
    this.updateHUD();
  }

  async redo() {
    if (this.redoStack.length === 0 || this.isAnimating) return;
    const step = this.redoStack.pop();
    await this.tryMove(step.toIndex, true);
  }

  attachEvents() {
    this.boardEl.addEventListener('click', async (e) => {
      const tileEl = e.target.closest('.tile');
      if (!tileEl) return;
      const val = Number(tileEl.dataset.value);
      if (val === 0) return;
      const idx = this.state.indexOf(val);
      await this.tryMove(idx);
    });

    window.addEventListener('resize', () => {
      // Reposition tiles on resize
      const size = this.size; const gap = 7;
      const w = this.boardEl.clientWidth;
      const tileSize = (w - gap * (size + 1)) / size;
      for (let i = 0; i < this.state.length; i++) {
        const v = this.state[i];
        const el = this.tiles.get(v);
        const r = Math.floor(i / size);
        const c = i % size;
        this.positionTile(el, r, c, tileSize, gap);
      }
    });

    document.addEventListener('keydown', async (e) => {
      const i0 = indexOfZero(this.state);
      const r = Math.floor(i0 / this.size);
      const c = i0 % this.size;
      let target = -1;
      if (e.key === 'ArrowUp' && r < this.size - 1) target = i0 + this.size;
      if (e.key === 'ArrowDown' && r > 0) target = i0 - this.size;
      if (e.key === 'ArrowLeft' && c < this.size - 1) target = i0 + 1;
      if (e.key === 'ArrowRight' && c > 0) target = i0 - 1;
      if (target !== -1) {
        e.preventDefault();
        await this.tryMove(target);
      }
    });
  }
}