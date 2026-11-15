/* UpgradeTree site: navigation, code tabs, YAML colours, Minecraft-style tooltips and the splash text. */
(function () {
  'use strict';

  const UT = window.UT || {};
  if (UT.hydrate) {
    UT.hydrate(document);
  }

  // ------------------------------------------------------------------ mobile navigation
  const toggle = document.querySelector('.nav-toggle');
  const nav = document.getElementById('nav');
  if (toggle && nav) {
    toggle.addEventListener('click', function () {
      const open = nav.classList.toggle('is-open');
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
    nav.addEventListener('click', function (e) {
      if (e.target.closest('a')) {
        nav.classList.remove('is-open');
        toggle.setAttribute('aria-expanded', 'false');
      }
    });
  }

  // ------------------------------------------------------------------ splash text (a new one on every visit)
  const splash = document.querySelector('.splash');
  const SPLASHES = ['Now with loot bags!', 'No chest menus!', '52 upgrades!', 'Spins like a CS2 case!', 'Bedrock ready!',
    'Try /chest!', 'Hexagons only!', 'Edit it in YAML!'];
  if (splash) {
    splash.textContent = SPLASHES[Math.floor(Math.random() * SPLASHES.length)];
  }

  // ------------------------------------------------------------------ YAML colours
  function esc(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }
  function value(v) {
    const m = /^(.*?)(\s+#.*)?$/.exec(v);
    let body = m[1];
    const comment = m[2] || '';
    body = esc(body)
      .replace(/("[^"]*")/g, '<span class="y-s">$1</span>')
      .replace(/(^|[\s[{,:])(-?\d+(?:\.\d+)?(?:-\d+)?)(?=[\s,\]}]|$)/g, '$1<span class="y-n">$2</span>')
      .replace(/\b(true|false)\b/g, '<span class="y-b">$1</span>');
    return body + (comment ? '<span class="y-c">' + esc(comment) + '</span>' : '');
  }
  function yaml(src) {
    return src.split('\n').map(function (line) {
      if (/^\s*#/.test(line)) {
        return '<span class="y-c">' + esc(line) + '</span>';
      }
      const m = /^(\s*(?:- )?)([\w.-]+)(:)(.*)$/.exec(line);
      if (m) {
        return esc(m[1]) + '<span class="y-k">' + m[2] + '</span>' + m[3] + value(m[4]);
      }
      const li = /^(\s*- )(.*)$/.exec(line);
      return li ? esc(li[1]) + value(li[2]) : value(line);
    }).join('\n');
  }
  document.querySelectorAll('code.lang-yaml').forEach(function (code) {
    code.innerHTML = yaml(code.textContent);
  });

  // ------------------------------------------------------------------ code tabs
  document.querySelectorAll('[data-tabs]').forEach(function (box) {
    const buttons = box.querySelectorAll('[role="tab"]');
    buttons.forEach(function (b) {
      b.addEventListener('click', function () {
        buttons.forEach(function (o) {
          const on = o === b;
          o.setAttribute('aria-selected', on ? 'true' : 'false');
          o.tabIndex = on ? 0 : -1;
          document.getElementById(o.getAttribute('aria-controls')).hidden = !on;
        });
      });
      b.addEventListener('keydown', function (e) {
        const list = Array.prototype.slice.call(buttons);
        const i = list.indexOf(b);
        const next = e.key === 'ArrowRight' ? list[(i + 1) % list.length] : e.key === 'ArrowLeft' ? list[(i - 1 + list.length) % list.length] : null;
        if (next) {
          e.preventDefault();
          next.click();
          next.focus();
        }
      });
    });
  });

  // ------------------------------------------------------------------ Minecraft-style tooltips for [data-tip]
  const tip = document.createElement('div');
  tip.className = 'mc-tip';
  tip.hidden = true;
  tip.setAttribute('role', 'tooltip');
  document.body.appendChild(tip);

  function place(x, y) {
    const w = tip.offsetWidth;
    const h = tip.offsetHeight;
    let left = x + 14;
    let top = y - h - 10;
    if (left + w > window.innerWidth - 8) { left = Math.max(8, x - w - 14); }
    if (top < 8) { top = y + 18; }
    tip.style.left = left + 'px';
    tip.style.top = top + 'px';
  }
  function show(el, x, y) {
    tip.innerHTML = el.getAttribute('data-tip');
    tip.hidden = false;
    place(x, y);
  }
  document.addEventListener('pointerover', function (e) {
    const el = e.target.closest && e.target.closest('[data-tip]');
    if (el) {
      show(el, e.clientX, e.clientY);
    } else if (!tip.hidden) {
      tip.hidden = true;
    }
  });
  document.addEventListener('pointermove', function (e) {
    if (!tip.hidden) { place(e.clientX, e.clientY); }
  });
  document.addEventListener('focusin', function (e) {
    const el = e.target.closest && e.target.closest('[data-tip]');
    if (el) {
      const r = el.getBoundingClientRect();
      show(el, r.left + r.width / 2, r.top);
    } else {
      tip.hidden = true;
    }
  });
  document.addEventListener('scroll', function () { tip.hidden = true; }, { passive: true });
})();
