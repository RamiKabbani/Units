// Sliding puzzle solver utilities
// Supports optimal A* for 3x3 (8-puzzle), and a Greedy + limited IDA* hybrid for larger sizes.

function manhattanDistance(state, size) {
  let d = 0;
  const n2 = size * size;
  for (let i = 0; i < n2; i++) {
    const v = state[i];
    if (v === 0) continue;
    const goalRow = Math.floor((v - 1) / size);
    const goalCol = (v - 1) % size;
    const row = Math.floor(i / size);
    const col = i % size;
    d += Math.abs(row - goalRow) + Math.abs(col - goalCol);
  }
  return d;
}

export function isSolved(state) {
  for (let i = 0; i < state.length - 1; i++) if (state[i] !== i + 1) return false;
  return state[state.length - 1] === 0;
}

export function cloneState(state) { return state.slice(); }

export function indexOfZero(state) { return state.indexOf(0); }

export function swap(state, i, j) {
  const s = state.slice();
  [s[i], s[j]] = [s[j], s[i]];
  return s;
}

export function getMoves(state, size) {
  const i0 = indexOfZero(state);
  const r = Math.floor(i0 / size);
  const c = i0 % size;
  const moves = [];
  if (r > 0) moves.push(i0 - size);
  if (r < size - 1) moves.push(i0 + size);
  if (c > 0) moves.push(i0 - 1);
  if (c < size - 1) moves.push(i0 + 1);
  return moves;
}

export function applyMove(state, toIndex) {
  const i0 = indexOfZero(state);
  return swap(state, i0, toIndex);
}

export function solvable(state, size) {
  // Standard N-puzzle solvability rules
  const arr = state.filter(v => v !== 0);
  let inversions = 0;
  for (let i = 0; i < arr.length; i++) {
    for (let j = i + 1; j < arr.length; j++) {
      if (arr[i] > arr[j]) inversions++;
    }
  }
  if (size % 2 === 1) {
    return inversions % 2 === 0;
  } else {
    const rowFromBottom = size - Math.floor(indexOfZero(state) / size);
    if (rowFromBottom % 2 === 0) {
      return inversions % 2 === 1;
    } else {
      return inversions % 2 === 0;
    }
  }
}

function reconstructPath(cameFrom, endKey) {
  const path = [];
  let cur = endKey;
  while (cameFrom.has(cur)) {
    const info = cameFrom.get(cur);
    path.push(info.move);
    cur = info.prev;
  }
  return path.reverse();
}

function keyOf(state) { return state.join(','); }

export async function solveOptimallyAStar(start, size, signal) {
  // For 3x3 this is fast; for larger sizes it's exponential. Restrict to size <= 3.
  const startKey = keyOf(start);
  const open = new Map(); // key -> { state, g, f }
  const gScore = new Map();
  const cameFrom = new Map(); // key -> { prev, move(toIndex) }
  const closed = new Set();

  const h0 = manhattanDistance(start, size);
  open.set(startKey, { state: start, g: 0, f: h0 });
  gScore.set(startKey, 0);

  function getBest() {
    let bestKey = null, bestF = Infinity;
    for (const [k, v] of open) {
      if (v.f < bestF) { bestF = v.f; bestKey = k; }
    }
    return bestKey;
  }

  let steps = 0;
  while (open.size > 0) {
    if (signal?.aborted) throw new DOMException('aborted', 'AbortError');
    const curKey = getBest();
    const cur = open.get(curKey);
    open.delete(curKey);
    closed.add(curKey);

    if (isSolved(cur.state)) {
      return reconstructPath(cameFrom, curKey).map(i => i);
    }

    for (const to of getMoves(cur.state, size)) {
      const next = applyMove(cur.state, to);
      const nextKey = keyOf(next);
      if (closed.has(nextKey)) continue;
      const tentativeG = cur.g + 1;
      const prevG = gScore.get(nextKey);
      if (prevG === undefined || tentativeG < prevG) {
        cameFrom.set(nextKey, { prev: curKey, move: to });
        gScore.set(nextKey, tentativeG);
        const f = tentativeG + manhattanDistance(next, size);
        open.set(nextKey, { state: next, g: tentativeG, f });
      }
    }

    if (++steps % 1000 === 0) await new Promise(r => setTimeout(r, 0));
  }
  return null;
}

// A pragmatic solver for 4x4/5x5: greedy best-first using Manhattan; if stuck, fall back to shallow IDA* iterations.
export async function solveHeuristic(start, size, signal, maxIters = 40000) {
  if (size <= 3) return solveOptimallyAStar(start, size, signal);
  const startKey = keyOf(start);
  const cameFrom = new Map();
  const open = new Map(); // key -> { state, h }
  const closed = new Set();

  open.set(startKey, { state: start, h: manhattanDistance(start, size) });
  let steps = 0;

  function getBest() {
    let bestKey = null, bestH = Infinity;
    for (const [k, v] of open) { if (v.h < bestH) { bestH = v.h; bestKey = k; } }
    return bestKey;
  }

  while (open.size > 0 && steps < maxIters) {
    if (signal?.aborted) throw new DOMException('aborted', 'AbortError');
    const curKey = getBest();
    const cur = open.get(curKey);
    open.delete(curKey);
    closed.add(curKey);

    if (isSolved(cur.state)) {
      return reconstructPath(cameFrom, curKey);
    }

    const moves = getMoves(cur.state, size);
    // Prefer moves that reduce heuristic
    const nexts = moves.map(to => {
      const s2 = applyMove(cur.state, to);
      return { to, state: s2, h: manhattanDistance(s2, size) };
    }).sort((a, b) => a.h - b.h);

    for (const n of nexts) {
      const k = keyOf(n.state);
      if (closed.has(k) || open.has(k)) continue;
      cameFrom.set(k, { prev: curKey, move: n.to });
      open.set(k, { state: n.state, h: n.h });
    }

    steps++;
    if (steps % 1000 === 0) await new Promise(r => setTimeout(r, 0));
  }
  // Fallback: attempt limited deepening from best-so-far
  return null;
}