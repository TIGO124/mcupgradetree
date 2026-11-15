/* UpgradeTree site: the hexagon panel demo (drawing, the info card and input).
   Drawing follows menu/NodeLayer.java, the card menu/InfoCard.java; the rules live in rules.js. */
(function () {
  'use strict';

  const UT = window.UT;
  const T = UT && UT.tree;
  const panel = document.getElementById('panel');
  if (!panel || !T || !UT.icon) {
    return;
  }
  const D = UT.data;
  const M = T.model;
  const NS = 'http://www.w3.org/2000/svg';
  const R = 40;
  const STEP_X = Math.sqrt(3) * R * 1.07;
  const STEP_Y = 1.5 * R * 1.07;
  const CONFIRM_COST = 10;   // menu.confirm-cost
  const REFUND = 0.75;       // respec.refund-percent
  const PREFIX = '<dim>[</dim><accent>Tree</accent><dim>]</dim> ';
  // the demo opens with a few upgrades bought, so the first view already shows a tree
  const SAMPLE = { origin: 1, sharp_sword: 2, swift_strikes: 1, health_boost: 2, miner: 1, wisdom: 1 };
  const START_BALANCE = 30;
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const mm = T.mm;
  const esc = T.esc;

  const svg = document.getElementById('p-svg');
  const card = document.getElementById('p-card');
  const chat = document.getElementById('p-chat');
  const resetBtn = document.getElementById('p-reset');
  const tabs = Array.prototype.slice.call(panel.querySelectorAll('[data-tab]'));

  const S = { tab: 'main', aim: null, confirm: null, respec: 0, touch: false, theme: null };
  let views = {};
  let lines = [];
  let gLines;
  let gNodes;
  let gFx;

  function hexPoints(r) {
    const pts = [];
    for (let i = 0; i < 6; i++) {
      const a = Math.PI / 180 * (60 * i - 90);
      pts.push((r * Math.cos(a)).toFixed(2) + ',' + (r * Math.sin(a)).toFixed(2));
    }
    return pts.join(' ');
  }
  const P_HALO = hexPoints(R * 1.17);
  const P_BORDER = hexPoints(R);
  const P_FILL = hexPoints(R * 0.85);

  function pos(n) { return [n.x * STEP_X, n.y * STEP_Y]; }
  function treeOf(id) { return D.TREES.filter(function (t) { return t.id === id; })[0]; }
  function bounds(tree) {
    const b = [Infinity, Infinity, -Infinity, -Infinity];
    tree.nodes.forEach(function (n) {
      const p = pos(n);
      b[0] = Math.min(b[0], p[0]);
      b[1] = Math.min(b[1], p[1]);
      b[2] = Math.max(b[2], p[0]);
      b[3] = Math.max(b[3], p[1]);
    });
    return b;
  }
  // every tab is at least as large as the main tree, so hexagons keep one size
  const MAIN = bounds(D.TREES[0]);
  function viewBox(tree) {
    const b = bounds(tree);
    const w = Math.max(b[2] - b[0], MAIN[2] - MAIN[0]) + R * 2.7;
    const h = Math.max(b[3] - b[1], MAIN[3] - MAIN[1]) + R * 2.7;
    const cx = (b[0] + b[2]) / 2;
    const cy = (b[1] + b[3]) / 2;
    return [cx - w / 2, cy - h / 2, w, h].map(function (v) { return v.toFixed(1); }).join(' ');
  }
  function node(tag, attrs, parent) {
    const e = document.createElementNS(NS, tag);
    Object.keys(attrs).forEach(function (k) { e.setAttribute(k, attrs[k]); });
    if (parent) { parent.appendChild(e); }
    return e;
  }
  function place(v, scale) {
    v.g.style.transform = 'translate(' + v.p[0].toFixed(1) + 'px,' + v.p[1].toFixed(1) + 'px) scale(' + scale + ')';
  }

  // ------------------------------------------------------------------ style
  function applyStyle(name) {
    const th = T.theme(name);
    S.theme = th;
    const p = panel.style;
    T.THEME_KEYS.forEach(function (k) { p.setProperty('--p-' + k, th[k]); });
    T.COLORS.forEach(function (k) { p.setProperty('--t-' + k, th.pal[k]); });
    p.setProperty('--p-raised', T.lighten(th.locked, 0.07));
    panel.setAttribute('data-style', name);
    refresh();
  }

  // ------------------------------------------------------------------ drawing
  function build() {
    const tree = treeOf(S.tab);
    svg.textContent = '';
    svg.setAttribute('viewBox', viewBox(tree));
    node('polygon', { points: P_FILL }, node('clipPath', { id: 'hx-clip' }, node('defs', {}, svg)));
    gLines = node('g', { class: 'lines' }, svg);
    gNodes = node('g', { class: 'nodes' }, svg);
    gFx = node('g', { class: 'fx' }, svg);
    views = {};
    tree.nodes.forEach(function (n) { if (T.visible(n)) { addView(n, null); } });
    buildLines();
    refresh();
    // on phones the tree is wider than the screen and scrolls sideways: start in the middle
    const view = document.getElementById('p-view');
    view.scrollLeft = (view.scrollWidth - view.clientWidth) / 2;
  }

  function addView(n, from) {
    const g = node('g', { class: 'hx', tabindex: '0', role: 'button', 'data-id': n.id }, gNodes);
    const inner = node('g', { class: 'hx-in' }, g);
    const v = { n: n, g: g, inner: inner, p: pos(n) };
    v.halo = node('polygon', { class: 'hx-halo', points: P_HALO }, inner);
    v.border = node('polygon', { class: 'hx-border', points: P_BORDER }, inner);
    v.pulse = node('polygon', { class: 'hx-pulse', points: P_BORDER }, inner);
    v.fill = node('polygon', { class: 'hx-fill', points: P_FILL }, inner);
    node('rect', { class: 'hx-shine', x: -R, y: -R, width: 2 * R, height: R * 0.62, 'clip-path': 'url(#hx-clip)' }, inner);
    v.img = node('image', { class: 'hx-icon', href: UT.icon(n.i).url }, inner);
    v.badge = node('text', { class: 'hx-badge', x: 0, y: (R * 0.66).toFixed(1), 'text-anchor': 'middle' }, inner);
    views[n.id] = v;
    if (from && !reduced) {
      const f = pos(from);
      g.style.opacity = '0';
      g.style.transform = 'translate(' + f[0].toFixed(1) + 'px,' + f[1].toFixed(1) + 'px) scale(0.2)';
      window.requestAnimationFrame(function () {
        window.requestAnimationFrame(function () {
          g.style.opacity = '1';
          place(v, 1);
        });
      });
    } else {
      place(v, 1);
    }
  }

  function buildLines() {
    gLines.textContent = '';
    lines = [];
    treeOf(S.tab).nodes.forEach(function (n) {
      if (!views[n.id]) { return; }
      n.links.forEach(function (id) {
        const from = T.NODES[id];
        if (!from || from.tree !== n.tree || !views[id]) { return; }
        const a = pos(from);
        const b = pos(n);
        lines.push({ from: id, to: n.id, el: node('line', { class: 'ln', x1: a[0], y1: a[1], x2: b[0], y2: b[1] }, gLines) });
      });
    });
  }

  function paint(v) {
    const th = S.theme;
    const n = v.n;
    const st = T.state(n);
    const base = th.locked;
    const aimed = S.aim === n.id;
    const fill = st === 'LOCKED' ? base : st === 'BLOCKED' ? T.mix(base, th.deny, 0.3)
      : st === 'AVAILABLE' ? T.mix(base, n.col, th.tint * 0.4) : T.mix(base, n.col, th.tint);
    const border = st === 'LOCKED' ? T.lighten(base, 0.12) : st === 'BLOCKED' ? T.mix(base, th.deny, 0.55)
      : st === 'AVAILABLE' ? th.line : st === 'OWNED' ? th.gold : th.maxed;
    v.fill.setAttribute('fill', fill);
    v.border.setAttribute('fill', aimed ? th.hover : border);
    v.pulse.setAttribute('fill', T.lighten(border, 0.55));
    v.halo.setAttribute('fill', st === 'MAXED' ? T.mix(th.maxed, base, 0.6) : 'none');
    v.g.classList.toggle('can-buy', T.canBuy(n) && !aimed);
    const small = st === 'LOCKED' || st === 'BLOCKED';
    const badge = n.m > 1;
    const size = Math.sqrt(3) * R * (small ? 0.46 : 0.56);
    const cy = badge ? -R * 0.12 : 0;
    v.img.setAttribute('x', (-size / 2).toFixed(1));
    v.img.setAttribute('y', (cy - size / 2).toFixed(1));
    v.img.setAttribute('width', size.toFixed(1));
    v.img.setAttribute('height', size.toFixed(1));
    v.img.setAttribute('opacity', small ? '0.7' : '1');
    v.badge.textContent = badge ? (st === 'MAXED' ? 'MAX' : T.lvl(n.id) + '/' + n.m) : '';
    v.badge.setAttribute('fill', st === 'MAXED' ? th.pal.accent : st === 'LOCKED' ? th.pal.dim : th.pal.hi);
    const label = { LOCKED: 'locked', BLOCKED: 'blocked', AVAILABLE: 'can be bought', OWNED: 'owned', MAXED: 'finished' }[st];
    v.g.setAttribute('aria-label', T.plain(n.n) + ', level ' + T.lvl(n.id) + ' of ' + n.m + ', ' + label);
  }

  function refresh() {
    if (!S.theme || !gNodes) { return; }
    const th = S.theme;
    Object.keys(views).forEach(function (id) { paint(views[id]); });
    lines.forEach(function (l) {
      const parent = T.lvl(l.from) > 0;
      const child = T.lvl(l.to) > 0;
      l.el.setAttribute('stroke', parent && child ? T.mix(th.gold, th.locked, 0.3) : parent ? th.line : th.hidden);
    });
    const tree = treeOf(S.tab).nodes;
    const done = tree.filter(function (n) { return T.lvl(n.id) >= n.m; }).length;
    const owned = tree.reduce(function (s, n) { return s + T.lvl(n.id); }, 0);
    const max = tree.reduce(function (s, n) { return s + n.m; }, 0);
    document.getElementById('p-prog').innerHTML = done === tree.length
      ? mm('<accent><b>' + done + '/' + tree.length + '</b></accent> <good>Tree completed')
      : mm('<hi><b>' + done + '/' + tree.length + '</b></hi> completed');
    document.getElementById('p-bar').style.width = (owned / max * 100).toFixed(1) + '%';
    document.getElementById('p-bal').textContent = T.num(M.bal);
    document.getElementById('p-curname').textContent = Math.abs(M.bal - 1) < 1e-9 ? 'Level' : 'Levels';
    renderCard();
  }

  // ------------------------------------------------------------------ the card
  function bigHex(icon, border, fill) {
    return '<svg class="c-hex" viewBox="-50 -50 100 100" aria-hidden="true"><polygon points="' + hexPoints(47) +
      '" fill="' + border + '"/><polygon points="' + hexPoints(40) + '" fill="' + fill + '"/><image href="' +
      UT.icon(icon).url + '" x="-23" y="-23" width="46" height="46"/></svg>';
  }

  function welcome() {
    const th = S.theme;
    const done = T.ALL.filter(function (n) { return T.lvl(n.id) >= n.m; }).length;
    const ready = T.ALL.filter(function (n) { return T.visible(n) && T.canBuy(n); }).length;
    card.innerHTML = bigHex('NETHER_STAR', th.line, T.mix(th.locked, '#4A8580', th.tint)) +
      '<h3 class="c-name">' + mm('<hi><b>Upgrade Tree') + '</h3><div class="c-desc">' +
      ['Aim at an upgrade,', 'its details show up here.', 'Unlocking one reveals', 'the next ones!'].map(function (l) {
        return '<p>' + mm(l) + '</p>';
      }).join('') + '</div><div class="c-box"><p>' + mm('Completed <hi>' + done + '</hi><dim>/</dim><hi>' + T.ALL.length) +
      '</p><p>' + (ready ? mm('<info><b>' + ready + '</b></info> upgrades ready to buy') : mm('Nothing affordable yet')) + '</p></div>';
  }

  function stats(n, st, l) {
    const th = S.theme;
    let box = '';
    if (n.b && st !== 'LOCKED' && st !== 'BLOCKED') {
      const color = th.tint <= 0 ? th.pal.value : T.lighten(n.col, 0.3);
      box += '<p class="c-bonus"><span style="color:' + color + '">' + esc(n.b.replace('{value}', T.num(T.bonusValue(n, l)))) + '</span>';
      if (l < n.m) {
        box += ' <span class="t-dim">→</span> <span style="color:' + color + '">' + esc(n.b.replace('{value}', T.num(T.bonusValue(n, l + 1)))) + '</span>';
      }
      box += '</p>';
    }
    if (st === 'MAXED') {
      return { box: box + '<p>' + mm('<accent>Completed') + '</p>', label: 'MAXED', color: T.mix(th.maxed, th.locked, 0.55) };
    }
    if (st === 'LOCKED') {
      return { box: box + '<p>' + mm('<bad>Requires: <hi>' + T.missing(n).join('<body>, <hi>')) + '</p>', label: 'LOCKED', color: T.lighten(th.locked, 0.1) };
    }
    if (st === 'BLOCKED') {
      const names = T.blockers(n).map(function (b) { return T.plain(b.n); }).join('<body>, <hi>');
      return { box: box + '<p>' + mm('<bad>Not with: <hi>' + names) + '</p>', label: 'BLOCKED', color: T.mix(th.deny, th.locked, 0.5) };
    }
    const c = T.cost(n);
    const poor = M.bal + 1e-9 < c;
    return {
      box: box + '<p>' + mm(c <= 0 ? 'Cost: <good>Free' : 'Cost: ' + (poor ? '<bad>' : '<value>') + T.currency(c)) + '</p>',
      label: S.confirm === n.id ? 'CLICK AGAIN' : 'BUY',
      color: poor ? T.mix(th.buy, th.locked, 0.55) : th.buy
    };
  }

  function renderCard() {
    const n = S.aim && T.NODES[S.aim];
    if (!S.theme) { return; }
    if (!n || n.tree !== S.tab) { welcome(); return; }
    const th = S.theme;
    const st = T.state(n);
    const l = T.lvl(n.id);
    const border = st === 'MAXED' ? th.maxed : st === 'OWNED' ? th.gold : st === 'AVAILABLE' ? th.line : T.lighten(th.locked, 0.12);
    let h = bigHex(n.i, border, st === 'LOCKED' ? th.locked : T.mix(th.locked, n.col, th.tint));
    h += '<h3 class="c-name">' + mm(n.n, 'hi') + '</h3>';
    h += '<p class="c-level">' + mm('Level <hi>' + l + '</hi><dim>/</dim><hi>' + n.m) + '</p>';
    h += '<span class="c-lvbar"><i style="width:' + (l / n.m * 100).toFixed(1) + '%"></i></span>';
    h += '<div class="c-desc">' + n.d.map(function (line) { return '<p>' + mm(line) + '</p>'; }).join('') + '</div>';
    if (n.ab) {
      const cd = n.ab[1] + n.ab[2] * (Math.max(1, l) - 1);
      h += '<p class="c-extra">' + mm('<info>' + D.TRIGGERS[n.ab[0]] + '</info> <dim>·</dim> ' + cd + 's cooldown') + '</p>';
    } else if (st === 'AVAILABLE' && T.unlocks(n) > 0) {
      const k = T.unlocks(n);
      h += '<p class="c-extra">' + mm('<info>Unlocks ' + k + ' new upgrade' + (k > 1 ? 's' : '')) + '</p>';
    }
    const s = stats(n, st, l);
    h += '<div class="c-box">' + s.box + '</div>';
    h += '<button class="c-buy" type="button" style="--b:' + s.color + '">' + s.label + '</button>';
    card.innerHTML = h;
    card.querySelector('.c-buy').addEventListener('click', function () { buy(n.id); });
  }

  // ------------------------------------------------------------------ actions
  function say(msg) {
    const li = document.createElement('li');
    li.innerHTML = mm(PREFIX + msg);
    chat.appendChild(li);
    while (chat.children.length > 4) { chat.removeChild(chat.firstChild); }
    window.setTimeout(function () { if (li.parentNode) { li.parentNode.removeChild(li); } }, 7000);
  }

  function aim(id) {
    if (S.aim === id) { return; }
    if (S.confirm && S.confirm !== id) { S.confirm = null; }
    S.aim = id;
    Object.keys(views).forEach(function (k) {
      place(views[k], k === id ? 1.1 : 1);
      paint(views[k]);
    });
    renderCard();
  }

  function sparks(n, big) {
    const v = views[n.id];
    if (!v || reduced) { return; }
    v.inner.classList.remove('pop');
    void v.inner.getBoundingClientRect();
    v.inner.classList.add('pop');
    const th = S.theme;
    for (let i = 0; i < (big ? 16 : 10); i++) {
      const a = Math.random() * Math.PI * 2;
      const d = R * (0.9 + Math.random() * 0.9);
      const s = node('rect', { class: 'spark', x: (v.p[0] - 3).toFixed(1), y: (v.p[1] - 3).toFixed(1), width: 6, height: 6, fill: i % 3 ? th.maxed : th.hover }, gFx);
      s.style.setProperty('--dx', (Math.cos(a) * d).toFixed(1) + 'px');
      s.style.setProperty('--dy', (Math.sin(a) * d).toFixed(1) + 'px');
      window.setTimeout(function () { s.remove(); }, 750);
    }
  }

  function learn(n) {
    const c = T.cost(n);
    M.bal = Math.round((M.bal - c) * 100) / 100;
    M.spent += c;
    M.lv[n.id] = T.lvl(n.id) + 1;
  }

  function buy(id) {
    const n = T.NODES[id];
    const st = T.state(n);
    if (st === 'MAXED') { say('<value>This upgrade is already at the maximum level.'); return; }
    if (st === 'LOCKED') { say('<bad>Locked! Unlock these first: <hi>' + T.missing(n).join('<body>, <hi>')); return; }
    if (st === 'BLOCKED') {
      say("<bad>This upgrade can't be combined with: <hi>" + T.blockers(n).map(function (b) { return T.plain(b.n); }).join('<body>, <hi>'));
      return;
    }
    const c = T.cost(n);
    if (M.bal + 1e-9 < c) {
      say('<bad>Not enough Levels! <body>Needed: <hi>' + T.num(c) + '</hi> · You have: <hi>' + T.num(M.bal));
      return;
    }
    if (c >= CONFIRM_COST && S.confirm !== id) {
      S.confirm = id;
      renderCard();
      say('<value>Click again to confirm: <hi>' + T.plain(n.n) + '</hi> <body>(' + T.currency(c) + ')');
      return;
    }
    S.confirm = null;
    learn(n);
    const done = T.lvl(id) >= n.m;
    say('<hi>' + T.plain(n.n) + '</hi> <accent>' + T.roman(T.lvl(id)) + '</accent> ' + (done ? '<good>mastered.' : 'learned.'));
    let added = 0;
    treeOf(S.tab).nodes.forEach(function (m) {
      if (!views[m.id] && T.visible(m)) {
        addView(m, n.tree === S.tab ? n : null);
        added++;
      }
    });
    if (added) { buildLines(); }
    sparks(n, done);
    refresh();
  }

  function respec() {
    if (M.spent <= 0) { say('<value>You have nothing to reset.'); return; }
    const refund = Math.floor(M.spent * REFUND);
    if (!S.respec) {
      S.respec = window.setTimeout(function () { S.respec = 0; resetBtn.textContent = 'Reset'; }, 5000);
      resetBtn.textContent = 'Click again';
      say('<value>Click again to reset. <body>Refund: <hi>' + T.currency(refund));
      return;
    }
    window.clearTimeout(S.respec);
    S.respec = 0;
    resetBtn.textContent = 'Reset';
    M.lv = {};
    M.bal += refund;
    M.spent = 0;
    S.aim = null;
    S.confirm = null;
    say('<good>Your upgrades were reset. <body>Refund: <hi>' + T.currency(refund));
    build();
  }

  // ------------------------------------------------------------------ input
  svg.addEventListener('pointerdown', function (e) { S.touch = e.pointerType === 'touch'; });
  svg.addEventListener('pointerover', function (e) {
    const g = e.pointerType !== 'touch' && e.target.closest && e.target.closest('.hx');
    if (g) { aim(g.getAttribute('data-id')); }
  });
  svg.addEventListener('click', function (e) {
    const g = e.target.closest && e.target.closest('.hx');
    if (!g) { return; }
    const id = g.getAttribute('data-id');
    if (S.touch && S.aim !== id) { aim(id); return; }
    aim(id);
    buy(id);
  });
  svg.addEventListener('focusin', function (e) {
    const g = e.target.closest && e.target.closest('.hx');
    if (g) { aim(g.getAttribute('data-id')); }
  });
  svg.addEventListener('keydown', function (e) {
    const g = e.target.closest && e.target.closest('.hx');
    if (g && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      buy(g.getAttribute('data-id'));
    }
  });

  tabs.forEach(function (t) {
    t.addEventListener('click', function () {
      S.tab = t.getAttribute('data-tab');
      S.aim = null;
      S.confirm = null;
      tabs.forEach(function (b) {
        b.classList.toggle('is-on', b === t);
        b.setAttribute('aria-selected', b === t ? 'true' : 'false');
      });
      build();
    });
  });

  resetBtn.addEventListener('click', respec);
  document.getElementById('p-close').addEventListener('click', function () {
    panel.classList.add('is-closed');
    document.getElementById('p-closed').hidden = false;
    document.getElementById('p-open').focus();
  });
  document.getElementById('p-open').addEventListener('click', function () {
    panel.classList.remove('is-closed');
    document.getElementById('p-closed').hidden = true;
  });
  document.getElementById('p-xp').addEventListener('click', function () {
    M.bal += 10;
    say('<good>+10 Levels <dim>(demo)');
    refresh();
  });

  function pressGroup(attr, value) {
    document.querySelectorAll('[' + attr + ']').forEach(function (b) {
      b.setAttribute('aria-pressed', b.getAttribute(attr) === value ? 'true' : 'false');
    });
  }
  document.querySelectorAll('[data-set-style]').forEach(function (b) {
    b.addEventListener('click', function () {
      applyStyle(b.getAttribute('data-set-style'));
      pressGroup('data-set-style', b.getAttribute('data-set-style'));
    });
  });
  document.querySelectorAll('[data-set-bg]').forEach(function (b) {
    b.addEventListener('click', function () {
      panel.setAttribute('data-bg', b.getAttribute('data-set-bg'));
      pressGroup('data-set-bg', b.getAttribute('data-set-bg'));
    });
  });

  Object.keys(SAMPLE).forEach(function (id) {
    for (let i = 0; i < SAMPLE[id]; i++) { learn(T.NODES[id]); }
  });
  M.bal = START_BALANCE;
  build();
  applyStyle('NIGHT');
})();
