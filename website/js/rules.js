/* UpgradeTree site: the tree rules and colours, shared by the panel.
   They follow the plugin: UpgradeService (requirements, states, progressive reveal), UpgradeNode (cost and bonus
   formulas), util/Palette and Settings.Theme (colour mixing) and Text.num (number format). */
(function () {
  'use strict';

  const UT = window.UT;
  const D = UT && UT.data;
  if (!D) {
    return;
  }

  const COLORS = ['hi', 'body', 'dim', 'accent', 'value', 'bad', 'good', 'info', 'magic'];
  const THEME_KEYS = ['background', 'border', 'header', 'card', 'locked', 'line', 'hidden', 'buy', 'deny', 'gold', 'maxed', 'hover'];

  const NODES = {};
  D.TREES.forEach(function (t) {
    t.nodes.forEach(function (n) {
      n.tree = t.id;
      n.links = (n.r || []).concat(n.ra || [], Object.keys(n.rl || {}));
      NODES[n.id] = n;
    });
  });
  const ALL = Object.keys(NODES).map(function (id) { return NODES[id]; });

  // the player's progress: levels by upgrade id, balance and what was spent (for the respec refund)
  const model = { lv: {}, bal: 0, spent: 0 };

  function lvl(id) { return model.lv[id] || 0; }
  function plain(s) { return s.replace(/<[^>]+>/g, ''); }

  function requirementsMet(n) {
    if ((n.r || []).some(function (id) { return lvl(id) <= 0; })) {
      return false;
    }
    if (n.ra && n.ra.length && !n.ra.some(function (id) { return lvl(id) > 0; })) {
      return false;
    }
    const rl = n.rl || {};
    return Object.keys(rl).every(function (id) { return lvl(id) >= rl[id]; });
  }
  function blockers(n) {
    return (n.ex || []).filter(function (id) { return lvl(id) > 0; }).map(function (id) { return NODES[id]; });
  }
  function state(n) {
    const l = lvl(n.id);
    if (l >= n.m) { return 'MAXED'; }
    if (l > 0) { return 'OWNED'; }
    if (blockers(n).length) { return 'BLOCKED'; }
    return requirementsMet(n) ? 'AVAILABLE' : 'LOCKED';
  }
  // progressive reveal: a node shows up once one of the nodes it mentions is owned
  function visible(n) {
    return n.av || !n.links.length || lvl(n.id) > 0 || n.links.some(function (id) { return lvl(id) > 0; });
  }
  // cost * multiplier^level + increase * level
  function cost(n) {
    const l = lvl(n.id);
    const c = n.c * Math.pow(n.cm || 1, l) + (n.ci || 0) * l;
    return Math.round(Math.max(0, c) * 100) / 100;
  }
  function bonusValue(n, l) {
    const per = n.bp != null ? n.bp : (n.pl || 0);
    return per * l * (n.bm != null ? n.bm : 1);
  }
  function canBuy(n) {
    const st = state(n);
    return (st === 'AVAILABLE' || st === 'OWNED') && model.bal + 1e-9 >= cost(n);
  }
  function num(v) {
    if (Math.abs(v - Math.round(v)) < 1e-6) { return String(Math.round(v)); }
    return v.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
  }
  function roman(n) {
    let out = '';
    [[10, 'X'], [9, 'IX'], [5, 'V'], [4, 'IV'], [1, 'I']].forEach(function (m) {
      while (n >= m[0]) { out += m[1]; n -= m[0]; }
    });
    return out;
  }
  function currency(v) { return num(v) + (Math.abs(v - 1) < 1e-9 ? ' Level' : ' Levels'); }
  function missing(n) {
    const out = [];
    (n.r || []).forEach(function (id) { if (lvl(id) <= 0) { out.push(plain(NODES[id].n)); } });
    if (n.ra && n.ra.length && !n.ra.some(function (id) { return lvl(id) > 0; })) {
      out.push(n.ra.map(function (id) { return plain(NODES[id].n); }).join(' or '));
    }
    Object.keys(n.rl || {}).forEach(function (id) {
      if (lvl(id) < n.rl[id]) { out.push(plain(NODES[id].n) + ' ' + roman(n.rl[id])); }
    });
    return out;
  }
  function unlocks(n) {
    return ALL.filter(function (m) { return !visible(m) && m.links.indexOf(n.id) >= 0; }).length;
  }

  // ------------------------------------------------------------------ colours
  function rgb(h) {
    const n = parseInt(h.replace('#', ''), 16);
    return [n >> 16 & 255, n >> 8 & 255, n & 255];
  }
  function hex(c) {
    return '#' + c.map(function (v) {
      return Math.round(Math.max(0, Math.min(255, v))).toString(16).padStart(2, '0');
    }).join('');
  }
  function mix(a, b, t) {
    const A = rgb(a);
    const B = rgb(b);
    const f = Math.max(0, Math.min(1, t));
    return hex(A.map(function (v, i) { return v * (1 - f) + B[i] * f; }));
  }
  function lighten(c, f) { return mix(c, '#ffffff', f); }
  // a see-through style colour is mixed into the panel background (Settings.Theme.opaque)
  function argb(s, bg) {
    const a = parseInt(s.slice(0, 2), 16);
    const c = '#' + s.slice(2);
    return bg && a < 255 ? mix(bg, c, a / 255) : c;
  }
  function theme(name) {
    const st = D.STYLES[name];
    const bg = argb(st.theme[0]);
    const th = { name: name, tint: st.tint, pal: {} };
    THEME_KEYS.forEach(function (k, i) { th[k] = argb(st.theme[i], bg); });
    COLORS.forEach(function (k, i) { th.pal[k] = st.palette[i]; });
    return th;
  }

  // ------------------------------------------------------------------ MiniMessage colour tags -> spans
  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function mm(text, base) {
    const start = base || 'body';
    let color = start;
    let bold = false;
    let html = '';
    text.split(/(<\/?[a-z]+>)/).forEach(function (tok) {
      const m = /^<(\/?)([a-z]+)>$/.exec(tok);
      if (m && m[2] === 'b') {
        bold = !m[1];
      } else if (m && COLORS.indexOf(m[2]) >= 0) {
        color = m[1] ? start : m[2];
      } else if (tok) {
        html += '<span class="t-' + color + (bold ? ' t-b' : '') + '">' + esc(tok) + '</span>';
      }
    });
    return html;
  }

  UT.tree = {
    NODES: NODES, ALL: ALL, model: model, THEME_KEYS: THEME_KEYS, COLORS: COLORS,
    lvl: lvl, plain: plain, state: state, visible: visible, cost: cost, bonusValue: bonusValue, canBuy: canBuy,
    blockers: blockers, missing: missing, unlocks: unlocks, num: num, roman: roman, currency: currency,
    mix: mix, lighten: lighten, theme: theme, esc: esc, mm: mm
  };
})();
