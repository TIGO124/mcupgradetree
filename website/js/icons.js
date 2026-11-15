/* Pixel item icons for the UpgradeTree site.
   Every icon is a 12x12 bitmap drawn for this site; no game textures are used. Rows are separated by spaces.
   Palette letters: k outline, a dark, b base, c light, d highlight. Fixed letters: w/W wood, g/G leaf green. */
(function () {
  'use strict';

  const SHAPES = {
    sword: '..........kk ........kcbk .......kcbk. ......kcbk.. .....kcbk... ..k.kcbk.... ..kacbk..... ...kak...... ..kWkak..... .kWk.kk..... kWk......... kk..........',
    pickaxe: '............ ....kkkkkk.. ...kcccbbbk. ...kakkwkabk ...kkkwk.kbk ....kwk...kk ...kwk...... ..kwk....... .kwk........ kwk......... kk.......... ............',
    axe: '..kkkk...k.. .kccbbk.kwk. .kcbbbbkwk.. .kcbbbkwk... ..kkkkwk.... ....kwk..... ...kwk...... ..kwk....... .kwk........ kwk......... kk.......... ............',
    shovel: '............ ........kkk. .......kcbbk .......kcbbk ......kwkkk. .....kwk.... ....kwk..... ...kwk...... ..kwk....... .kwk........ kwk......... kk..........',
    trident: '..c..c..c... ..b..b..b... ..b..b..b... ..bbbbbbb... ...kbbbk.... .....ak..... .....ak..... .....ak..... .....ak..... .....ak..... .....ak..... .....kk.....',
    mace: '...k.kk.k... ..kbkbbkbk.. ..kbbccbbk.. .kbbcccbbbk. ..kbbbbbbk.. ..kbkaakbk.. ...k.ww.k... .....ww..... .....ww..... .....ww..... ....kaak.... .....kk.....',
    rod: '..........kk .........kck ........kcbk .......kcbk. ......kcbk.. .....kcbk... ....kcbk.... ...kcbk..... ..kcbk...... .kcbk....... kcbk........ kkk.........',
    fishingrod: '.........kk. ........kwkc .......kwk.c ......kwk..c .....kwk...c ....kwk....c ...kwk.....c ..kwk......c .kwk......ac kwk.......aa kk.......... ............',
    star: '.....kk..... .....bb..... ....kbck.... ....bccb.... .k.bcddcb.k. kbbccddccbbk kbbccddccbbk .k.bcddcb.k. ....bccb.... ....kcbk.... .....bb..... .....kk.....',
    gem: '............ ............ ...kkkkkk... ..kcdcbbak.. .kccbbbbaak. .kcbbbbbaak. ..kbbbbbak.. ...kbbbak... ....kbak.... .....kk..... ............ ............',
    ingot: '............ ............ ............ ....kkkkkkk. ...kcccccbk. ..kcbbbbbbak .kbbbbbbbak. .kaaaaaaak.. ..kkkkkkk... ............ ............ ............',
    lump: '............ ............ ....kkk..... ..kkbcbkk... .kbccbbbak.. .kbcbbaabak. kbbbaabbbak. kabbbbbbaak. .kaabbbaak.. ..kkaaakk... ....kkk..... ............',
    block: '.....kk..... ...kkcckk... .kkcccccckk. kbbkkcckkaak kbbbbkkaaaak kbbbbbaaaaak kbbbbbaaaaak kbbbbbaaaaak kbbbbbaaaaak .kkbbbaaakk. ...kkbakk... .....kk.....',
    chest: '.....kk..... ...kkcckk... .kkcccccckk. kbbkkcckkaak kbbbbkkaaaak kkkkkkkkkkkk kbbddbaaaaak kbbbbbaaaaak kbbbbbaaaaak .kkbbbaaakk. ...kkbakk... .....kk.....',
    furnace: '.....kk..... ...kkcckk... .kkcccccckk. kbbkkcckkaak kbbbbkkaaaak kbbbbbaaaaak kbkkkbaaaaak kbkdkbaaaaak kbkkkbaaaaak .kkbbbaaakk. ...kkbakk... .....kk.....',
    tnt: '.....kk..... ...kkcckk... .kkcccccckk. kbbkkcckkaak kbbbbkkaaaak kbbbbbaaaaak kddddddddddk kddddddddddk kbbbbbaaaaak .kkbbbaaakk. ...kkbakk... .....kk.....',
    beacon: '.....kk..... ...kkcckk... .kkccddcckk. kbbkkddkkaak kbbbbkkaaaak kbbbbbaaaaak kbbbbbaaaaak kbbbbbaaaaak kbbbbbaaaaak .kkbbbaaakk. ...kkbakk... .....kk.....',
    stairs: '............ ............ ......kkkkk. ......kccck. ......kbbbk. .kkkkkkbbbk. .kccccbbbbk. .kbbbbbbbbk. .kbbbbbbbak. .kaaaaaaaak. .kkkkkkkkkk. ............',
    apple: '......kkk... .....kwgggk. ..kkkkwkkk.. .kcdcbbbbak. kcdcbbbbbbak kccbbbbbbbak kcbbbbbbbbak kbbbbbbbbaak .kbbbbbbaak. .kabbbbaaak. ..kkkkkkkk.. ............',
    meat: '............ ............ ...kkkkk.... ..kbcbbbkk.. .kbccbbbbak. kbcbbbdbbak. kbbbddbbbak. kbbbbbbbaak. .kbbbbbaak.. ..kkaaakk... ............ ............',
    orb: '............ ....kkkk.... ..kkcddckk.. .kcddcbbbak. .kcdcbbbbak. kccbbbbbbbak kcbbbbbbbbak kbbbbbbbbaak .kbbbbbbaak. .kabbbbaaak. ..kkaaaakk.. ....kkkk....',
    eye: '............ ....kkkk.... ..kkcddckk.. .kcddcbbbak. .kcdcbbbbak. kccbbkkbbbak kcbbkkkkbbak kbbbbkkbbaak .kbbbbbbaak. .kabbbbaaak. ..kkaaaakk.. ....kkkk....',
    bottle: '....kkkk.... ....kwWk.... ....kddk.... ...kkcckk... ..kcdcbbbk.. .kcdbbbbbak. .kcbbbbbbak. .kbbbbbbbak. .kbbbbbbaak. ..kbbbbaak.. ...kkkkkk... ............',
    book: '............ .kkkkkkkkkkk .kabbbbbbbdk .kabccbbbbdk .kabcccbbbdk .kabbcbbbbdk .kabbbbbbbdk .kabbbbbcbdk .kabbbbbbbdk .kaaaaaaaadk .kkkkkkkkkkk ............',
    feather: '............ .........kk. ........kcck .......kccck ......kcccak .....kcccak. ....kcccak.. ...kcccak... ..kccak..... ..kak....... .kak........ .kk.........',
    elytra: '............ ..kkk..kkk.. .kccbkkbcck. .kcbbbbbbck. kcbbbkkbbbck kbbbk..kbbbk kbbak..kabbk kbbak..kabbk kbak....kabk kbak....kabk kak......kak kk........kk',
    chestplate: '............ .kkk....kkk. kcbbkkkkbbak kcbbbbbbbbak kkcbbbbbbakk .kcbbbbbbak. .kcbbbbbbak. .kcbbbbbbak. .kcbbbbbbak. .kbbbbbbaak. .kkkkkkkkkk. ............',
    shield: '............ .kkkkkkkkkk. .kaWWWWWWak. .kaWwWWwWak. .kaWwccwWak. .kaWwccwWak. .kaWwWWwWak. .kaWWWWWWak. ..kaWWWWak.. ...kaWWak... ....kaak.... .....kk.....',
    totem: '....kkkk.... ...kcccbk... ...kgbbgk... ...kbbbak... .kk.kaak.kk. kcbkbbbbkbak kbbbbbbbbbak .kk.kbbk.kk. ....kbbk.... ...kbbbak... ...kbkkak... ...kk..kk...',
    foot: '............ .......kkk.. ......kccbk. .....kcbbbk. ....kcbbbak. ...kcbbbak.. ..kcbbbak... .kcbbbak.... kcbbaak..... kbbaak...... .kkkk....... ............',
    horn: '............ ..kk........ .kcbk....... .kcbk....... .kcbbk...... ..kcbbk..... ...kcbbkk... ....kcbbbkk. .....kkbbbak .......kkaak .........kk. ............',
    melon: '............ ............ kkkkkkkkkkkk kbbbbbbbbbbk .kbbabbbabk. .kbbbbbbbbk. ..kbabbbak.. ..kcbbbbck.. ...kccddk... ....kkkk.... ............ ............',
    sapling: '............ .....kk..... ....kggk.... ..kkgGggkk.. .kgGggggGgk. .kggGggGggk. ..kkgwWgkk.. ....kwWk.... ....kwWk.... ...kkwWkk... ...kkkkkk... ............',
    wheat: '.k...k...k.. kck.kck.kck. kbk.kbk.kbk. kck.kck.kck. kbk.kbk.kbk. .ka..a..ak.. ..ka.a.ak... ...kaaak.... ....aaa..... ....aaa..... ...kaaak.... ............',
    bundle: '....kkkk.... ...kccbbk... ....kaak.... ...kkbbkk... ..kcbbbbak.. .kcbbbbbbak. .kcbbbbbbak. kcbbbbbbbbak kbbbbbbbbaak kbbbbbbbaaak .kaaaaaaaak. ..kkkkkkkk..',
    dust: '............ ............ ......k..... .....kck.... ..k...k..k.. .kck.....kck ..k..kk...k. ....kbbk.... ...kbccbk... ..kbbcbbak.. .kaabbbbaak. ..kkkkkkkk..'
  };

  const FIXED = { w: '#4b3218', W: '#8a5f2e', g: '#4f9a2c', G: '#2f6b1b' };

  // k a b c d
  const PALETTES = {
    iron: '#2f3237 #8a9098 #c4c9cf #e9ecef #ffffff',
    gold: '#5a3c0a #c38a17 #f0c43a #fbe58c #fff7d2',
    diamond: '#0c4644 #1f9c94 #4ddbcf #a3fbef #e9fffc',
    netherite: '#151214 #3a3237 #564a51 #786b73 #a3949c',
    copper: '#4a220f #9a4e2a #d0784a #f0a77a #fff1e6',
    emerald: '#0b4a1f #17923f #2fd160 #93f5ae #e4ffe9',
    prismarine: '#1f4b48 #3f8f86 #6cc1b5 #a9e5da #e3fff9',
    amethyst: '#3d1f5c #7a4fb3 #a77de0 #d4bcf5 #ffffff',
    star: '#7d8396 #b9c0d0 #d8dde8 #eff2f8 #ffffff',
    coal: '#0e0e10 #232327 #38383e #55555c #7a7a82',
    scrap: '#2a1a14 #4d3127 #6b4638 #8c6352 #b08a78',
    debris: '#1f130e #3f281f #5a3a2e #6e4b3d #8a6b5c',
    ironblock: '#4a4e55 #9aa0a8 #cfd3d8 #f0f2f4 #ffffff',
    obsidian: '#0a0712 #140d24 #22163a #3a2a5e #6a4ea0',
    tnt: '#3d0d08 #962217 #d13b2b #d0d0d0 #f4f4f4',
    furnace: '#262729 #585a5f #7f8287 #a9abae #ff9a2e',
    beacon: '#1d4c50 #4ea6a3 #84d8d3 #c9f6f2 #ffffff',
    chest: '#3a230c #704619 #a26b2c #c99446 #d9d9d9',
    ender: '#060f10 #112729 #1c3e3b #2f5f58 #3fd6a8',
    chestiron: '#2a1c10 #5e4129 #8a6440 #cdd2d8 #e9ecef',
    chestdiamond: '#1c110a #3c2516 #5b3a24 #62d6cb #f2c94c',
    chestnetherite: '#0c0b0e #1d1b21 #2f2b33 #5d555a #f2c94c',
    oak: '#3e2e17 #7d6035 #a8844a #c4a061 #e0c48a',
    beef: '#3a1c10 #6b3a22 #8f5432 #b07048 #e8d2b0',
    pork: '#4a2414 #a0573a #d68a66 #f0b494 #fbe3d3',
    bread: '#4a2e10 #9a6424 #c98f3c #e5b765 #f3d79b',
    potato: '#5a3c14 #a37632 #d1a352 #ecc87a #fff4d8',
    pearl: '#0c2f2c #155e57 #1f8a7e #4fc7b6 #c6fff3',
    heart: '#0d2a52 #1f4f9a #2f7fd1 #78b8f5 #e1f2ff',
    slime: '#2f6b1e #4f9a35 #7cc95a #ade88f #e5ffd6',
    magma: '#4a1a06 #a3380e #e2641c #f7a443 #ffe0a3',
    tear: '#6b7f86 #a9c3c9 #d3e6ea #f0fbfd #ffffff',
    egg: '#0c0712 #1f1230 #2d1a45 #4b2b6e #8a5bc4',
    wind: '#5a6f9a #9fb3dd #c9d6f2 #e6edfb #ffffff',
    xp: '#2d4a07 #6f9d12 #b6e02c #e4fb6f #fbffd2',
    spidereye: '#3a0a12 #7a1626 #b02a3e #d85a6a #f6b3bb',
    endereye: '#0d2a1a #1f5e3a #2f8f55 #6fd08f #d3ffe1',
    xpbottle: '#1f3a0c #4b8a22 #86c94a #c1ef8a #eef9f2',
    enchbook: '#20103a #4a2a78 #7a4fb3 #c6a6f0 #efe4c9',
    plainbook: '#2a1a0e #5a3a1e #8a5a2e #b07a45 #efe4c9',
    feather: '#8e9399 #c4c8cc #dfe2e5 #f6f7f8 #ffffff',
    membrane: '#56586b #8d8fa8 #b6b8cf #d9dbec #ffffff',
    elytra: '#2b2838 #5c5772 #8e88a6 #b9b3d0 #e0dcf0',
    totem: '#5a3c0c #a8781c #e8b93a #fbe28c #ffffff',
    rabbit: '#4a3420 #8a6a48 #bfa07a #e3cba8 #ffffff',
    horn: '#4b4237 #8f8473 #c4b9a5 #e4dccb #ffffff',
    melon: '#2c3d12 #3a1a1a #e0423c #6fae2f #3f7a1d',
    glistmelon: '#5a3c0a #8a5a10 #f0a63a #fbe58c #c38a17',
    wheat: '#5a4512 #9a7b2a #d9b44a #f0d77a #ffffff',
    blaze: '#6b3a05 #d0770e #f5b324 #ffe27a #fff6c9',
    sugar: '#9aa0a8 #cfd4da #eef1f4 #ffffff #ffffff',
    bone: '#9a958a #cfc9bb #e9e4d6 #fbf8ef #ffffff',
    arrow: '#3a2410 #6b4a2a #9a6e40 #c49a66 #ffffff',
    trident: '#1c4747 #3a8a83 #5fbfb2 #a8eadf #ffffff',
    mace: '#25262a #4f5157 #7c7f87 #b3b6bd #ffffff',
    shield: '#2a2a2a #8e939a #8e939a #d0d4d8 #ffffff',
    fishing: '#2a1c10 #9a9a9a #e6e6e6 #e6e6e6 #ffffff',
    leaf: '#1c3a10 #2f6b1b #4f9a2c #7cc04a #b5e58a'
  };

  // material -> "shape palette [glint]"
  const ITEMS = {
    NETHER_STAR: 'star star', IRON_SWORD: 'sword iron', GOLDEN_SWORD: 'sword gold', DIAMOND_SWORD: 'sword diamond',
    NETHERITE_SWORD: 'sword netherite', FERMENTED_SPIDER_EYE: 'eye spidereye', BLAZE_POWDER: 'dust blaze',
    TRIDENT: 'trident trident', GOLDEN_APPLE: 'apple gold', ENCHANTED_GOLDEN_APPLE: 'apple gold glint',
    IRON_CHESTPLATE: 'chestplate iron', NETHERITE_CHESTPLATE: 'chestplate netherite', DIAMOND_CHESTPLATE: 'chestplate diamond',
    SHIELD: 'shield shield', PHANTOM_MEMBRANE: 'feather membrane', GHAST_TEAR: 'orb tear', TOTEM_OF_UNDYING: 'totem totem',
    IRON_PICKAXE: 'pickaxe iron', DIAMOND_PICKAXE: 'pickaxe diamond', GOLDEN_PICKAXE: 'pickaxe gold',
    DIAMOND: 'gem diamond', EMERALD: 'gem emerald', PRISMARINE_SHARD: 'gem prismarine', AMETHYST_SHARD: 'gem amethyst',
    GOLD_INGOT: 'ingot gold', IRON_INGOT: 'ingot iron', NETHERITE_INGOT: 'ingot netherite', COPPER_INGOT: 'ingot copper',
    SPYGLASS: 'rod copper', EXPERIENCE_BOTTLE: 'bottle xpbottle', ENDER_EYE: 'eye endereye',
    ENCHANTED_BOOK: 'book enchbook glint', BOOK: 'book plainbook', DRAGON_EGG: 'orb egg', FEATHER: 'feather feather',
    OAK_STAIRS: 'stairs oak', RABBIT_FOOT: 'foot rabbit', SLIME_BALL: 'orb slime', HEART_OF_THE_SEA: 'orb heart',
    COOKED_BEEF: 'meat beef', COOKED_PORKCHOP: 'meat pork', BREAD: 'meat bread', BAKED_POTATO: 'lump potato',
    MAGMA_CREAM: 'orb magma', BONE: 'rod bone', ENDER_CHEST: 'chest ender', CHEST: 'chest chest',
    CHEST_IRON: 'chest chestiron', CHEST_DIAMOND: 'chest chestdiamond', CHEST_NETHERITE: 'chest chestnetherite',
    BLAZE_ROD: 'rod blaze', WIND_CHARGE: 'orb wind', SUGAR: 'dust sugar', GOAT_HORN: 'horn horn', TNT: 'tnt tnt',
    GLISTERING_MELON_SLICE: 'melon glistmelon', MELON_SLICE: 'melon melon', MACE: 'mace mace', ELYTRA: 'elytra elytra',
    BEACON: 'beacon beacon', BLAST_FURNACE: 'furnace furnace', GOLDEN_SHOVEL: 'shovel gold', FISHING_ROD: 'fishingrod fishing',
    IRON_AXE: 'axe iron', OAK_SAPLING: 'sapling leaf', WHEAT: 'wheat wheat', ARROW: 'rod arrow', COAL: 'lump coal',
    NETHERITE_SCRAP: 'lump scrap', ANCIENT_DEBRIS: 'block debris', DIAMOND_BLOCK: 'block diamond', GOLD_BLOCK: 'block gold',
    IRON_BLOCK: 'block ironblock', EMERALD_BLOCK: 'block emerald', OBSIDIAN: 'block obsidian', ENDER_PEARL: 'orb pearl',
    LEVELS: 'orb xp'
  };

  const BAGS = {
    COMMON: '#B4BCC8', UNCOMMON: '#5BD35B', RARE: '#4FA3FF', EPIC: '#B062FF',
    LEGENDARY: '#FFA41C', MYTHIC: '#FF4F6B', SECRET: '#9D6BFF'
  };

  function shade(hex, f) {
    const n = parseInt(hex.slice(1), 16);
    const ch = [n >> 16 & 255, n >> 8 & 255, n & 255].map(function (v) {
      const t = f < 0 ? v * (1 + f) : v + (255 - v) * f;
      return Math.round(Math.max(0, Math.min(255, t))).toString(16).padStart(2, '0');
    });
    return '#' + ch.join('');
  }

  function bagPalette(hex) {
    return [shade(hex, -0.72), shade(hex, -0.35), hex, shade(hex, 0.35), shade(hex, 0.7)].join(' ');
  }

  const cache = {};

  function draw(shape, palette) {
    const rows = SHAPES[shape].split(' ');
    const cols = palette.split(' ');
    const pal = { k: cols[0], a: cols[1], b: cols[2], c: cols[3], d: cols[4] };
    const cv = document.createElement('canvas');
    cv.width = 12;
    cv.height = 12;
    const g = cv.getContext('2d');
    rows.forEach(function (row, y) {
      for (let x = 0; x < row.length; x++) {
        const color = FIXED[row[x]] || pal[row[x]];
        if (color) {
          g.fillStyle = color;
          g.fillRect(x, y, 1, 1);
        }
      }
    });
    return cv.toDataURL('image/png');
  }

  /** Icon of a material ("DIAMOND_SWORD", "BAG_RARE", "LEVELS"): { url, glint }. Unknown materials get a plain block. */
  function icon(material) {
    const key = String(material || '').toUpperCase();
    if (cache[key]) {
      return cache[key];
    }
    let spec;
    if (key.indexOf('BAG_') === 0 && BAGS[key.slice(4)]) {
      spec = { shape: 'bundle', palette: bagPalette(BAGS[key.slice(4)]), glint: false };
    } else {
      const parts = (ITEMS[key] || 'block ironblock').split(' ');
      spec = { shape: parts[0], palette: PALETTES[parts[1]], glint: parts[2] === 'glint' };
    }
    cache[key] = { url: draw(spec.shape, spec.palette), glint: spec.glint };
    return cache[key];
  }

  /** An element showing the icon; enchanted items get a moving shine. */
  function iconEl(material, glint, cls) {
    const ic = icon(material);
    const img = document.createElement('img');
    img.src = ic.url;
    img.alt = '';
    img.className = 'px' + (cls ? ' ' + cls : '');
    if (!(glint || ic.glint)) {
      return img;
    }
    const wrap = document.createElement('span');
    wrap.className = 'ico glint' + (cls ? ' ' + cls : '');
    wrap.style.setProperty('--icon', 'url("' + ic.url + '")');
    img.className = 'px';
    wrap.appendChild(img);
    return wrap;
  }

  /** Fills every <img data-item="..."> below root. */
  function hydrate(root) {
    (root || document).querySelectorAll('img[data-item]').forEach(function (img) {
      const ic = icon(img.getAttribute('data-item'));
      img.src = ic.url;
      img.removeAttribute('data-item');
      if (ic.glint || img.hasAttribute('data-glint')) {
        const wrap = document.createElement('span');
        wrap.className = 'ico glint ' + img.className;
        wrap.style.setProperty('--icon', 'url("' + ic.url + '")');
        img.replaceWith(wrap);
        img.className = 'px';
        wrap.appendChild(img);
      }
    });
  }

  window.UT = window.UT || {};
  window.UT.icon = icon;
  window.UT.iconEl = iconEl;
  window.UT.hydrate = hydrate;
  window.UT.SHAPES = SHAPES;
})();
