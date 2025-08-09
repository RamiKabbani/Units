export function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

export function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export const easeOutCubic = t => 1 - Math.pow(1 - t, 3);
export const easeInOutCubic = t => t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;

export function animate({ duration = 300, easing = easeOutCubic, update, complete }) {
  const start = performance.now();
  let raf = 0;
  function frame(now) {
    const t = clamp((now - start) / duration, 0, 1);
    const p = easing(t);
    update(p);
    if (t < 1) raf = requestAnimationFrame(frame);
    else if (complete) complete();
  }
  raf = requestAnimationFrame(frame);
  return () => cancelAnimationFrame(raf);
}

export function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

export function shuffleArray(array) {
  const a = array.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

export function createEl(tag, className, attrs = {}) {
  const el = document.createElement(tag);
  if (className) el.className = className;
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'text') el.textContent = v;
    else if (k === 'html') el.innerHTML = v;
    else el.setAttribute(k, v);
  }
  return el;
}