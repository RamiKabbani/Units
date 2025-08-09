# Sliding Puzzle — Smooth Animations + Solver

A polished web-based sliding puzzle (3×3, 4×4, 5×5) with:
- Smooth, GPU-accelerated tile animations
- Keyboard controls (arrow keys)
- Undo/Redo
- Shuffle and Reset
- Timer and move counter
- Win confetti celebration
- Solver: Optimal A* for 3×3; greedy heuristic for larger sizes

## Run locally
Option 1: Open `index.html` directly in a modern browser.

Option 2: Serve with a static server:

```
bash run.sh
```

Then open `http://localhost:5173`.

## Files
- `index.html` — entry page
- `src/styles.css` — modern UI styles
- `src/utils.js` — helpers for animations and time formatting
- `src/confetti.js` — lightweight confetti canvas
- `src/solver.js` — puzzle logic and solver algorithms
- `src/game.js` — game state, rendering, animations
- `src/main.js` — UI wiring and solver integration

## Notes
- The heuristic solver for 4×4/5×5 is not guaranteed optimal and may not find a solution within limits for very difficult scrambles. The 3×3 mode is solved optimally.
- Use the Size selector to switch board sizes.