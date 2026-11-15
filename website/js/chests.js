/* UpgradeTree site: the chest strip and the loot bags.
   Odds follow the plugin: a chest first picks a rarity by its weight, then one prize of that rarity (chests.yml);
   a loot bag holds 5-10 different entries picked by weight, never the same entry twice (lootbags.yml). */
(function () {
  'use strict';

  const UT = window.UT;
  const D = UT && UT.data;
  if (!D || !UT.iconEl) {
    return;
  }
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const PLAYER = 'Kaan';
  const MOBS = ['Zombie', 'Skeleton', 'Creeper', 'Spider', 'Enderman', 'Witch', 'Drowned', 'Pillager'];
  const RAR = {};
  D.CHEST_RARITIES.forEach(function (r) { RAR[r.id] = r; });

  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function title(material) {
    return material.toLowerCase().split('_').map(function (w, i) {
      return i > 0 && (w === 'of' || w === 'the') ? w : w.charAt(0).toUpperCase() + w.slice(1);
    }).join(' ');
  }
  function pct(v) {
    return (v >= 1 ? v.toFixed(2) : v.toPrecision(2)).replace(/\.?0+$/, '') + '%';
  }
  function chatLine(box, html) {
    const hint = box.querySelector('.chat-hint');
    if (hint) { hint.remove(); }
    const p = document.createElement('p');
    p.innerHTML = html;
    box.appendChild(p);
    while (box.children.length > 7) { box.removeChild(box.firstChild); }
  }
  const PREFIX = '<span class="c-dim">[</span><span class="c-accent">Tree</span><span class="c-dim">]</span> ';

  // ------------------------------------------------------------------ chests
  const strip = document.getElementById('strip');
  const track = document.getElementById('strip-track');
  const openBtn = document.getElementById('case-open');
  const slots = document.getElementById('case-slots');
  const big = document.getElementById('case-big');
  const caseChat = document.getElementById('case-chat');
  let chest = D.CHESTS[0];
  let busy = false;

  function rewardName(rw) {
    return rw[0] === 'LEVELS' ? '+' + rw[1] + ' Levels' : (rw[1] > 1 ? rw[1] + '× ' : '') + (rw[3] || title(rw[0]));
  }
  function present(c) {
    return D.CHEST_RARITIES.filter(function (r) { return c.rewards.some(function (rw) { return rw[2] === r.id; }); });
  }
  function chanceOf(c, rw) {
    const rs = present(c);
    const total = rs.reduce(function (s, r) { return s + r.weight; }, 0);
    const same = c.rewards.filter(function (x) { return x[2] === rw[2]; }).length;
    return RAR[rw[2]].weight / total / same * 100;
  }
  function roll(c) {
    const rs = present(c);
    let x = Math.random() * rs.reduce(function (s, r) { return s + r.weight; }, 0);
    let pick = rs[rs.length - 1];
    for (let i = 0; i < rs.length; i++) {
      x -= rs[i].weight;
      if (x <= 0) { pick = rs[i]; break; }
    }
    const pool = c.rewards.filter(function (rw) { return rw[2] === pick.id; });
    return pool[Math.floor(Math.random() * pool.length)];
  }
  function tip(c, rw) {
    const r = RAR[rw[2]];
    return '<b style="color:' + r.color + '">' + esc(rw[0] === 'LEVELS' ? '+' + rw[1] + ' Levels' : rw[3] || title(rw[0])) + '</b>' +
      (rw[4] ? '<span class="tt-ench">' + esc(rw[4]) + '</span>' : '') +
      (rw[0] !== 'LEVELS' && rw[1] > 1 ? '<span class="tt-gray">Amount: ' + rw[1] + '</span>' : '') +
      '<span class="tt-gray">' + r.name + ' · Chance: ' + pct(chanceOf(c, rw)) + '</span>';
  }
  function itemEl(tag, c, rw) {
    const el = document.createElement(tag);
    el.className = 'mc-slot';
    el.style.setProperty('--r', RAR[rw[2]].color);
    el.appendChild(UT.iconEl(rw[0], !!rw[4]));
    if (rw[1] > 1) {
      const n = document.createElement('span');
      n.className = 'mc-count';
      n.textContent = rw[1];
      el.appendChild(n);
    }
    el.setAttribute('data-tip', tip(c, rw));
    el.setAttribute('tabindex', '0');
    el.setAttribute('aria-label', rewardName(rw) + ', ' + RAR[rw[2]].name);
    return el;
  }

  function fillStrip(items) {
    track.style.transition = 'none';
    track.style.transform = 'translateX(0)';
    track.textContent = '';
    items.forEach(function (rw) {
      const el = itemEl('div', chest, rw);
      el.removeAttribute('tabindex');
      track.appendChild(el);
    });
  }

  function select(id) {
    if (busy) { return; }
    chest = D.CHESTS.filter(function (c) { return c.id === id; })[0];
    document.querySelectorAll('[data-chest]').forEach(function (b) {
      const on = b.getAttribute('data-chest') === id;
      b.classList.toggle('is-on', on);
      b.setAttribute('aria-checked', on ? 'true' : 'false');
    });
    big.replaceChildren(UT.iconEl(chest.icon, false, 'case-big-img'));
    big.style.setProperty('--c', chest.color);
    openBtn.textContent = 'Open the ' + chest.name;
    slots.textContent = '';
    chest.rewards.forEach(function (rw) {
      const li = itemEl('li', chest, rw);
      slots.appendChild(li);
    });
    const idle = [];
    for (let i = 0; i < 24; i++) { idle.push(chest.rewards[i % chest.rewards.length]); }
    fillStrip(idle);
  }

  function open() {
    if (busy) { return; }
    busy = true;
    openBtn.disabled = true;
    const win = roll(chest);
    const N = 60;
    const at = N - 8;
    const items = [];
    for (let i = 0; i < N; i++) { items.push(i === at ? win : roll(chest)); }
    fillStrip(items);
    const first = track.children[0];
    const gap = parseFloat(window.getComputedStyle(track).columnGap) || 0;
    const step = first.offsetWidth + gap;
    const target = at * step + first.offsetWidth / 2 - strip.clientWidth / 2 + (Math.random() - 0.5) * first.offsetWidth * 0.7;
    big.classList.remove('is-open');
    big.classList.add('is-shaking');
    window.setTimeout(function () {
      big.classList.remove('is-shaking');
      big.classList.add('is-open');
      void track.offsetWidth;
      track.style.transition = reduced ? 'none' : 'transform 6s cubic-bezier(0.06, 0.7, 0.12, 1)';
      track.style.transform = 'translateX(' + (-target).toFixed(1) + 'px)';
      window.setTimeout(function () { finish(win, track.children[at]); }, reduced ? 30 : 6150);
    }, reduced ? 0 : 700);
  }

  function finish(win, el) {
    busy = false;
    openBtn.disabled = false;
    big.classList.remove('is-open');
    if (el) { el.classList.add('is-won'); }
    const r = RAR[win[2]];
    const prize = '<span style="color:' + r.color + '">' + esc(rewardName(win)) + '</span>';
    const name = '<span style="color:' + chest.color + '">' + esc(chest.name) + '</span>';
    chatLine(caseChat, PREFIX + name + ' <span class="c-body">opened:</span> ' + prize + ' <span class="c-dim">(</span><span style="color:' + r.color + '">' + r.name + '</span><span class="c-dim">)</span>');
    if (r.announce) {
      chatLine(caseChat, PREFIX + '<span class="c-hi">' + PLAYER + '</span> <span class="c-body">opened a</span> ' + name +
        ' <span class="c-body">and found:</span> ' + prize + ' <span class="c-dim">(</span><span style="color:' + r.color + '">' + r.name + '</span><span class="c-dim">)</span>');
    }
  }

  if (track && openBtn) {
    document.querySelectorAll('[data-chest]').forEach(function (b) {
      b.addEventListener('click', function () { select(b.getAttribute('data-chest')); });
    });
    openBtn.addEventListener('click', open);
    select('iron');
  }

  // ------------------------------------------------------------------ loot bags
  const lootChat = document.getElementById('loot-chat');

  function parse(entry) {
    let s = entry;
    let name = null;
    let ench = null;
    if (s.indexOf('|') >= 0) { name = s.slice(s.indexOf('|') + 1); s = s.slice(0, s.indexOf('|')); }
    if (s.indexOf('*') >= 0) { ench = s.slice(s.indexOf('*') + 1).trim(); s = s.slice(0, s.indexOf('*')).trim(); }
    const parts = s.split(' ');
    const amount = (parts[1] || '1').split('-').map(Number);
    let weight = 1;
    parts.slice(2).forEach(function (p) { if (p.charAt(0) === 'w') { weight = parseFloat(p.slice(1)); } });
    return { item: parts[0], min: amount[0], max: amount[1] || amount[0], weight: weight, ench: ench, name: name };
  }

  function bagName(bag) {
    const cls = bag.id === 'secret' ? 'bag-secret' : bag.id === 'mythic' ? 'bag-bold' : '';
    return '<span class="' + cls + '" style="color:' + bag.color + '">' + esc(bag.name) + '</span>';
  }

  function openBag(bag) {
    const pool = bag.loot.map(parse);
    const count = Math.min(pool.length, 5 + Math.floor(Math.random() * 6));
    const got = [];
    for (let i = 0; i < count; i++) {
      let x = Math.random() * pool.reduce(function (s, e) { return s + e.weight; }, 0);
      let k = 0;
      while (k < pool.length - 1 && (x -= pool[k].weight) > 0) { k++; }
      got.push(pool.splice(k, 1)[0]);
    }
    const parts = got.map(function (e) {
      const n = e.min + Math.floor(Math.random() * (e.max - e.min + 1));
      if (e.item === 'LEVELS') { return '<span class="c-hi">+' + n + ' Levels</span>'; }
      const label = (n > 1 ? n + '× ' : '') + (e.name || title(e.item));
      return e.ench ? '<span class="c-aqua" data-tip="' + esc('<b class="tt-aqua">' + esc(e.name || title(e.item)) + '</b><span class="tt-ench">' + esc(e.ench) + '</span>') + '">' + esc(label) + '</span>'
        : '<span class="c-white">' + esc(label) + '</span>';
    });
    chatLine(lootChat, bagName(bag) + ' <span class="c-body">opened:</span> ' + parts.join('<span class="c-dim">, </span>'));
    if (bag.announce) {
      chatLine(lootChat, bagName(bag) + ' <span class="c-body">found by</span> <span class="c-hi">' + PLAYER + '</span><span class="c-body">!</span> <span class="c-dim">(' + MOBS[Math.floor(Math.random() * MOBS.length)] + ')</span>');
    }
  }

  if (lootChat) {
    document.querySelectorAll('[data-bag]').forEach(function (b) {
      b.addEventListener('click', function () {
        const bag = D.LOOT.filter(function (x) { return x.id === b.getAttribute('data-bag'); })[0];
        const row = b.closest('tr');
        if (row && !reduced) {
          row.classList.remove('is-opening');
          void row.offsetWidth;
          row.classList.add('is-opening');
        }
        openBag(bag);
      });
    });
  }
})();
