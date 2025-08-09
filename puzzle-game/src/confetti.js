import { easeOutCubic } from './utils.js';

export function createConfetti(canvas) {
  const ctx = canvas.getContext('2d');
  let particles = [];
  let raf = 0;
  let isRunning = false;

  function resize() {
    const dpr = Math.max(1, window.devicePixelRatio || 1);
    canvas.width = Math.floor(canvas.clientWidth * dpr);
    canvas.height = Math.floor(canvas.clientHeight * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  function spawnBurst(x, y, count = 120) {
    for (let i = 0; i < count; i++) {
      const angle = Math.random() * Math.PI * 2;
      const speed = 6 + Math.random() * 6;
      particles.push({
        x, y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        life: 0,
        ttl: 120 + Math.random() * 50,
        size: 2 + Math.random() * 4,
        color: `hsl(${Math.floor(Math.random() * 360)}, 80%, 60%)`,
        rotation: Math.random() * Math.PI,
        vr: (Math.random() - 0.5) * 0.2,
      });
    }
  }

  function draw() {
    ctx.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight);
    for (const p of particles) {
      p.life++;
      p.vy += 0.1; // gravity
      p.x += p.vx;
      p.y += p.vy;
      p.rotation += p.vr;
      const t = p.life / p.ttl;
      const alpha = 1 - easeOutCubic(t);
      if (alpha <= 0) continue;
      ctx.globalAlpha = alpha;
      ctx.save();
      ctx.translate(p.x, p.y);
      ctx.rotate(p.rotation);
      ctx.fillStyle = p.color;
      ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
      ctx.restore();
    }
    particles = particles.filter(p => p.life < p.ttl);
    if (particles.length === 0) {
      isRunning = false;
      return;
    }
    raf = requestAnimationFrame(draw);
  }

  function burstAtCenter() {
    const rect = canvas.getBoundingClientRect();
    spawnBurst(rect.width / 2, rect.height / 3, 200);
    if (!isRunning) { isRunning = true; draw(); }
  }

  window.addEventListener('resize', resize);
  resize();

  return {
    burst: (x, y) => { spawnBurst(x, y, 120); if (!isRunning) { isRunning = true; draw(); } },
    celebrate: burstAtCenter,
    stop: () => { cancelAnimationFrame(raf); isRunning = false; particles = []; ctx.clearRect(0,0,canvas.clientWidth, canvas.clientHeight); },
  };
}