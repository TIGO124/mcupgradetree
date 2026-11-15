/* Procedural block textures for the page bands (drawn at 16 px per block, scaled up with pixelated rendering).
   They are generated here instead of shipped as images; nothing comes from the game. Each one becomes a CSS
   variable such as --tex-dirt. The page has plain colours underneath, so it reads fine before this runs. */
(function () {
  'use strict';

  // small seeded random generator: the same texture on every visit
  function rng(seed) {
    let a = seed >>> 0;
    return function () {
      a = (a + 0x6D2B79F5) >>> 0;
      let t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function sheet(w, h) {
    const c = document.createElement('canvas');
    c.width = w;
    c.height = h;
    return c;
  }

  function noise(g, x0, y0, size, pal, rnd) {
    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        g.fillStyle = pal[Math.floor(rnd() * rnd() * pal.length)];
        g.fillRect(x0 + x, y0 + y, 1, 1);
      }
    }
  }

  // ore: a few 2x2 specks with a highlight and a shadow pixel
  function ore(g, x0, y0, col, rnd) {
    const n = 3 + Math.floor(rnd() * 2);
    for (let i = 0; i < n; i++) {
      const x = x0 + 2 + Math.floor(rnd() * 11);
      const y = y0 + 2 + Math.floor(rnd() * 11);
      g.fillStyle = col[1];
      g.fillRect(x, y, 2, 2);
      g.fillStyle = col[0];
      g.fillRect(x, y, 1, 1);
      g.fillStyle = col[2];
      g.fillRect(x + 1, y + 1, 1, 1);
    }
  }

  function tile(pal, seed) {
    const c = sheet(16, 16);
    noise(c.getContext('2d'), 0, 0, 16, pal, rng(seed));
    return c;
  }

  // 8x6 blocks of stone or deepslate; about one block in six holds an ore
  function oreSheet(pal, ores, seed) {
    const c = sheet(128, 96);
    const g = c.getContext('2d');
    const rnd = rng(seed);
    for (let i = 0; i < 48; i++) {
      const x = (i % 8) * 16;
      const y = Math.floor(i / 8) * 16;
      noise(g, x, y, 16, pal, rnd);
      if (rnd() < 0.17) {
        ore(g, x, y, ores[Math.floor(rnd() * ores.length)], rnd);
      }
    }
    return c;
  }

  function grassEdge() {
    const c = sheet(16, 16);
    const g = c.getContext('2d');
    const rnd = rng(7);
    noise(g, 0, 0, 16, ['#6b4a30', '#5b3d27', '#79553a', '#4a3220', '#86603f'], rnd);
    const green = ['#5c9a35', '#6fb03f', '#4f8a2b', '#7cc04a'];
    for (let x = 0; x < 16; x++) {
      const depth = 3 + (rnd() < 0.55 ? 1 : 0) + (rnd() < 0.25 ? 1 : 0);
      for (let y = 0; y < depth; y++) {
        g.fillStyle = green[Math.floor(rnd() * green.length)];
        g.fillRect(x, y, 1, 1);
      }
    }
    return c;
  }

  function planks() {
    const c = sheet(16, 16);
    const g = c.getContext('2d');
    const rnd = rng(31);
    for (let row = 0; row < 4; row++) {
      noise(g, 0, row * 4, 4, ['#7a5a33', '#6c4f2c', '#856238'], rnd);
      noise(g, 4, row * 4, 4, ['#7a5a33', '#6c4f2c', '#856238'], rnd);
      noise(g, 8, row * 4, 4, ['#7a5a33', '#6c4f2c', '#856238'], rnd);
      noise(g, 12, row * 4, 4, ['#7a5a33', '#6c4f2c', '#856238'], rnd);
      g.fillStyle = '#3f2c17';
      g.fillRect(0, row * 4 + 3, 16, 1);
      g.fillRect(row % 2 ? 4 : 11, row * 4, 1, 3);
    }
    return c;
  }

  function stars(size, count, seed) {
    const c = sheet(size, size);
    const g = c.getContext('2d');
    const rnd = rng(seed);
    for (let i = 0; i < count; i++) {
      const x = Math.floor(rnd() * size);
      const y = Math.floor(rnd() * size);
      g.fillStyle = 'rgba(230, 238, 255, ' + (0.25 + rnd() * 0.75).toFixed(2) + ')';
      g.fillRect(x, y, 1, 1);
      if (rnd() < 0.12) {
        g.fillStyle = 'rgba(230, 238, 255, 0.35)';
        g.fillRect(x - 1, y, 3, 1);
        g.fillRect(x, y - 1, 1, 3);
      }
    }
    return c;
  }

  function moon() {
    const c = tile(['#eef2f8', '#e2e8f1', '#d6dde9', '#f8fafc'], 11);
    const g = c.getContext('2d');
    g.fillStyle = '#c3ccda';
    [[3, 3, 3, 2], [9, 5, 2, 2], [5, 10, 2, 3], [11, 11, 3, 2], [2, 7, 1, 1]].forEach(function (r) {
      g.fillRect(r[0], r[1], r[2], r[3]);
    });
    g.fillStyle = '#aeb8c8';
    [[4, 4, 1, 1], [10, 6, 1, 1], [6, 12, 1, 1], [12, 12, 1, 1]].forEach(function (r) {
      g.fillRect(r[0], r[1], r[2], r[3]);
    });
    return c;
  }

  const STONE = ['#6f7175', '#7c7e82', '#626468', '#88898d', '#57595c'];
  const DEEP = ['#3c3c42', '#47474e', '#313137', '#52525a', '#28282d'];
  const ORE = {
    diamond: ['#c9fff7', '#4ddbcf', '#1f8c86'],
    gold: ['#fff2a8', '#f3c63a', '#a8761a'],
    iron: ['#f2dcc8', '#d8af93', '#8f6a52'],
    redstone: ['#ff8a8a', '#e01c1c', '#7a0a0a'],
    coal: ['#5a5a5e', '#1c1c1f', '#0b0b0c'],
    lapis: ['#8fb1ff', '#2752c7', '#14306f'],
    emerald: ['#a6ffbf', '#2fd160', '#127a35']
  };

  const out = {
    stars: stars(128, 60, 3),
    'panel-stars': stars(96, 40, 5),
    moon: moon(),
    grass: grassEdge(),
    dirt: tile(['#5b3d27', '#6b4a30', '#4a3220', '#79553a', '#3d2a1b'], 13),
    stone: tile(STONE, 17),
    ore: oreSheet(STONE, [ORE.coal, ORE.coal, ORE.iron, ORE.iron, ORE.gold, ORE.diamond, ORE.emerald], 19),
    deep: tile(DEEP, 23),
    'deep-ore': oreSheet(DEEP, [ORE.redstone, ORE.redstone, ORE.lapis, ORE.diamond, ORE.gold], 29),
    bedrock: tile(['#1d1d1f', '#2c2c2f', '#4a4a4e', '#121214', '#6a6a6e'], 37),
    'logo-stone': tile(['#eef1f5', '#dfe3e9', '#cfd4dc', '#f8f9fb', '#bfc5ce'], 41),
    'logo-leaf': tile(['#8fd14f', '#79c043', '#64a836', '#a6e066', '#4f8f2a'], 43),
    planks: planks()
  };

  const root = document.documentElement.style;
  Object.keys(out).forEach(function (name) {
    root.setProperty('--tex-' + name, 'url("' + out[name].toDataURL('image/png') + '")');
  });
})();
