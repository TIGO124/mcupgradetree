/* Game data for the UpgradeTree site, copied from the plugin's built-in files
   (trees/*.yml, chests.yml, lootbags.yml, config/Style.java). Keep it in sync when the defaults change.
   Node keys: n name, i icon, m max-level, c cost, ci cost-increase, r requires, ra requires-any,
   rl requires-level, ex exclusive-with, av always-visible, col colour, d description, b bonus,
   bm bonus-multiplier, bp bonus-per-level, pl amount per level of the first effect, ab [trigger, cooldown, per level]. */
(function () {
  'use strict';

  const TREES = [
    { id: 'main', name: 'Main Tree', icon: 'NETHER_STAR', nodes: [
      { id: 'origin', n: 'Origin', i: 'NETHER_STAR', x: 0, y: 0, m: 1, c: 1, col: '#4A8580', d: ['<body>The heart of the tree.', '<body>Unlocks all six branches.'], b: '+{value} ❤', bm: 0.5, pl: 2 },
      { id: 'sharp_sword', n: 'Sharp Sword', i: 'IRON_SWORD', x: 1, y: 0, m: 5, c: 2, ci: 1, r: ['origin'], col: '#A8452F', d: ['<body>All your attacks', '<body>deal more damage.'], b: '+{value} damage', pl: 0.3 },
      { id: 'swift_strikes', n: 'Swift Strikes', i: 'GOLDEN_SWORD', x: 2, y: 0, m: 4, c: 3, ci: 1, r: ['sharp_sword'], col: '#A8452F', d: ['<body>You attack faster.'], b: '+{value}% attack speed', bm: 100, pl: 0.03 },
      { id: 'critical_hit', n: 'Critical Hit', i: 'DIAMOND_SWORD', x: 2.5, y: -1, m: 4, c: 4, ci: 2, r: ['swift_strikes'], col: '#A8452F', d: ['<body>Your hits can deal', '<body>1.4x damage.'], b: '{value}% chance', bm: 100, pl: 0.04 },
      { id: 'bloodsucker', n: 'Bloodsucker', i: 'FERMENTED_SPIDER_EYE', x: 2.5, y: 1, m: 3, c: 5, ci: 2, r: ['swift_strikes'], col: '#A8452F', d: ['<body>Part of the damage', '<body>you deal heals you.'], b: '{value}% lifesteal', bm: 100, pl: 0.025 },
      { id: 'rage', n: 'Rage', i: 'BLAZE_POWDER', x: 3, y: 0, m: 2, c: 6, ci: 4, r: ['swift_strikes'], col: '#A8452F', d: ['<body>Your rage powers', '<body>all of your attacks.'], b: '+{value} damage', pl: 0.25 },
      { id: 'whirlwind', n: 'Whirlwind', i: 'NETHERITE_SWORD', x: 4, y: 0, m: 3, c: 6, ci: 2, r: ['rage'], col: '#A8452F', d: ['<body>Your sweeping attacks', '<body>get stronger.'], b: '+{value}% sweeping', bm: 100, pl: 0.15 },
      { id: 'warmaster', n: '<b>Warmaster</b>', i: 'TRIDENT', x: 5, y: 0, m: 1, c: 20, r: ['whirlwind'], rl: { critical_hit: 2 }, col: '#923826', d: ['<body>Master of the combat branch.', '<value>+0.5 damage <body>· <value>+5% attack speed'] },
      { id: 'health_boost', n: 'Health Boost', i: 'GOLDEN_APPLE', x: -1, y: 0, m: 5, c: 2, ci: 1, r: ['origin'], col: '#4F7591', d: ['<body>Your maximum health grows.'], b: '+{value} ❤', bm: 0.5, pl: 1 },
      { id: 'iron_skin', n: 'Iron Skin', i: 'IRON_CHESTPLATE', x: -2, y: 0, m: 5, c: 3, ci: 1, r: ['health_boost'], col: '#4F7591', d: ['<body>Gain extra armor', '<body>even without wearing any.'], b: '+{value} armor', pl: 0.6 },
      { id: 'unshakable', n: 'Unshakable', i: 'SHIELD', x: -2.5, y: -1, m: 3, c: 3, ci: 2, r: ['iron_skin'], col: '#4F7591', d: ['<body>Gain resistance', '<body>against knockback.'], b: '{value}% resistance', bm: 100, pl: 0.05 },
      { id: 'shadow_step', n: 'Shadow Step', i: 'PHANTOM_MEMBRANE', x: -2.5, y: 1, m: 3, c: 5, ci: 2, r: ['iron_skin'], col: '#4F7591', d: ['<body>A chance to fully', '<body>dodge attacks.'], b: '{value}% dodge', bm: 100, pl: 0.03 },
      { id: 'regeneration', n: 'Regeneration', i: 'GHAST_TEAR', x: -3, y: 0, m: 1, c: 25, r: ['iron_skin'], col: '#4F7591', d: ['<body>Your health slowly', '<body>regenerates all the time.'] },
      { id: 'giant_shield', n: 'Giant Shield', i: 'NETHERITE_CHESTPLATE', x: -4, y: 0, m: 3, c: 6, ci: 2, r: ['regeneration'], col: '#4F7591', d: ['<body>You withstand heavy', '<body>blows better.'], b: '+{value} armor toughness', pl: 0.5 },
      { id: 'titan', n: '<b>Titan</b>', i: 'TOTEM_OF_UNDYING', x: -5, y: 0, m: 1, c: 20, r: ['giant_shield'], rl: { unshakable: 3 }, col: '#44657F', d: ['<body>A little bigger and', '<body>a lot tougher.', '<value>+1 ❤ <body>· <value>10% size'] },
      { id: 'miner', n: 'Miner', i: 'IRON_PICKAXE', x: 0.5, y: -1, m: 5, c: 2, ci: 1, r: ['origin'], col: '#B98433', d: ['<body>You break blocks', '<body>faster.'], b: '+{value}% mining speed', bm: 100, pl: 0.06 },
      { id: 'lucky_dig', n: 'Lucky Dig', i: 'DIAMOND', x: 1, y: -2, m: 4, c: 4, ci: 2, r: ['miner'], col: '#B98433', d: ['<body>A chance for double', '<body>drops from ores.'], b: '{value}% chance', bm: 100, pl: 0.03 },
      { id: 'treasure_hunter', n: 'Treasure Hunter', i: 'GOLD_INGOT', x: 2, y: -2, m: 3, c: 4, ci: 2, r: ['lucky_dig'], col: '#B98433', d: ['<body>More luck: better chest', '<body>and fishing loot.'], b: '+{value} luck', pl: 0.5 },
      { id: 'diver_miner', n: 'Diver Miner', i: 'PRISMARINE_SHARD', x: 1.5, y: -3, m: 3, c: 5, ci: 2, r: ['lucky_dig'], col: '#B98433', d: ['<body>You mine much faster', '<body>underwater.'], b: '+{value}% underwater speed', bm: 100, pl: 0.4 },
      { id: 'masters_eye', n: "Master's Eye", i: 'SPYGLASS', x: 0, y: -2, m: 3, c: 4, ci: 2, ra: ['miner', 'wisdom'], col: '#6F5A9A', d: ['<body>You reach blocks', '<body>from farther away.'], b: '+{value} block reach', pl: 0.5 },
      { id: 'wisdom', n: 'Wisdom', i: 'EXPERIENCE_BOTTLE', x: -0.5, y: -1, m: 5, c: 2, ci: 1, r: ['origin'], col: '#7A5A99', d: ['<body>You gain more', '<body>experience (XP).'], b: '+{value}% XP', bm: 100, pl: 0.06 },
      { id: 'cats_eye', n: "Cat's Eye", i: 'ENDER_EYE', x: -1, y: -2, m: 1, c: 6, r: ['wisdom'], col: '#7A5A99', d: ['<body>You see clearly', '<body>even in the dark.'] },
      { id: 'strong_memory', n: 'Strong Memory', i: 'ENCHANTED_BOOK', x: -1.5, y: -3, m: 4, c: 5, ci: 2, r: ['cats_eye'], col: '#7A5A99', d: ['<body>You keep part of', '<body>your XP when you die.'], b: '{value}% XP kept', bm: 100, pl: 0.15 },
      { id: 'legend', n: '<b>Legend</b>', i: 'DRAGON_EGG', x: 0, y: -4, m: 1, c: 30, r: ['diver_miner', 'strong_memory'], col: '#7E4479', d: ['<body>The peak of the wisdom', '<body>and mining branches.', '<value>+1 ❤ <body>· <value>+0.5 luck <body>· <value>+5% XP'] },
      { id: 'agility', n: 'Agility', i: 'FEATHER', x: 0.5, y: 1, m: 5, c: 2, ci: 1, r: ['origin'], col: '#6B8A3A', d: ['<body>You run faster.'], b: '+{value}% speed', bm: 100, pl: 0.02 },
      { id: 'easy_step', n: 'Easy Step', i: 'OAK_STAIRS', x: 0, y: 2, m: 1, c: 6, r: ['agility'], col: '#6B8A3A', d: ['<body>You step up full blocks', '<body>without jumping.'] },
      { id: 'spring_legs', n: 'Spring Legs', i: 'RABBIT_FOOT', x: 1, y: 2, m: 3, c: 3, ci: 2, r: ['agility'], col: '#6B8A3A', d: ['<body>You jump higher.'], b: '+{value} jump', pl: 0.04 },
      { id: 'feather', n: 'Light as a Feather', i: 'SLIME_BALL', x: 1.5, y: 3, m: 3, c: 4, ci: 2, r: ['spring_legs'], col: '#6B8A3A', d: ['<body>You take less fall damage.'], b: '-{value}% fall damage', bm: -100, pl: -0.15 },
      { id: 'dolphin', n: 'Dolphin', i: 'HEART_OF_THE_SEA', x: 2, y: 2, m: 3, c: 4, ci: 2, r: ['spring_legs'], col: '#6B8A3A', d: ['<body>You stay underwater longer', '<body>and swim faster.'], b: '+{value} breath', pl: 1 },
      { id: 'satiety', n: 'Satiety', i: 'COOKED_BEEF', x: -0.5, y: 1, m: 4, c: 2, ci: 1, r: ['origin'], col: '#94485E', d: ['<body>Your hunger drops', '<body>more slowly.'], b: '{value}% saved', bm: 100, pl: 0.08 },
      { id: 'fireproof', n: 'Fireproof', i: 'MAGMA_CREAM', x: -1, y: 2, m: 3, c: 3, ci: 2, r: ['satiety'], col: '#94485E', d: ['<body>Fire burns you for', '<body>a shorter time.'], b: '-{value}% burning', bm: -100, pl: -0.25 },
      { id: 'hunter', n: 'Hunter', i: 'BONE', x: -2, y: 2, m: 3, c: 4, ci: 2, r: ['fireproof'], col: '#94485E', d: ['<body>A chance for double', '<body>loot from mobs.'], b: '{value}% chance', bm: 100, pl: 0.07 },
      { id: 'second_chance', n: 'Second Chance', i: 'ENDER_CHEST', x: -1.5, y: 3, m: 3, c: 10, ci: 5, r: ['fireproof'], col: '#94485E', d: ['<body>A chance to keep your', '<body>items when you die.'], b: '{value}% chance', bm: 100, pl: 0.05 }
    ] },
    { id: 'abilities', name: 'Abilities', icon: 'BLAZE_POWDER', nodes: [
      { id: 'ability_gate', n: 'Ability Gate', i: 'BLAZE_ROD', x: 0, y: 0, m: 1, c: 3, r: ['origin'], av: true, col: '#B8692F', d: ['<body>Unlocks the', '<body>active abilities.'] },
      { id: 'dash', n: 'Dash', i: 'WIND_CHARGE', x: 1, y: 0, m: 3, c: 5, ci: 3, r: ['ability_gate'], ex: ['speed_burst'], col: '#B8692F', d: ['<hi>Shift + F</hi><body>: dash in the', '<body>direction you look.'], b: 'Power {value}', bp: 1, ab: ['SNEAK_SWAP', 14, -2] },
      { id: 'speed_burst', n: 'Speed Burst', i: 'SUGAR', x: 0.5, y: -1, m: 3, c: 5, ci: 3, r: ['ability_gate'], ex: ['dash'], col: '#B8692F', d: ['<hi>Shift + F</hi><body>: run fast', '<body>for a short time.', '<body>Lv.1 Speed I, Lv.2+ Speed II'], ab: ['SNEAK_SWAP', 35, -5] },
      { id: 'leap', n: 'Leap', i: 'GOAT_HORN', x: -1, y: 0, m: 3, c: 5, ci: 3, r: ['ability_gate'], ex: ['shockwave'], col: '#8E5287', d: ['<hi>Shift + Jump</hi><body>: launch into', '<body>the air, no fall damage.'], b: 'Power {value}', bp: 1, ab: ['SNEAK_JUMP', 15, -3] },
      { id: 'shockwave', n: 'Shockwave', i: 'TNT', x: -0.5, y: -1, m: 3, c: 6, ci: 3, r: ['ability_gate'], ex: ['leap'], col: '#8E5287', d: ['<hi>Shift + Jump</hi><body>: knock back', '<body>nearby monsters.'], b: 'Power {value}', bp: 1, ab: ['SNEAK_JUMP', 25, -4] },
      { id: 'heal', n: 'Heal', i: 'GLISTERING_MELON_SLICE', x: 0.5, y: 1, m: 3, c: 6, ci: 3, r: ['ability_gate'], ex: ['shield'], col: '#4C8466', d: ['<hi>Double Shift</hi><body>: heal', '<body>right away.', '<body>Lv.1 2 ❤, Lv.2 2.5 ❤, Lv.3 3 ❤'], ab: ['DOUBLE_SNEAK', 45, -6] },
      { id: 'shield', n: 'Shield', i: 'SHIELD', x: -0.5, y: 1, m: 3, c: 6, ci: 3, r: ['ability_gate'], ex: ['heal'], col: '#4C8466', d: ['<hi>Double Shift</hi><body>: extra health', '<body>and resistance for a while.', '<body>Lv.1 +2 ❤, Lv.2+ +4 ❤ shield'], ab: ['DOUBLE_SNEAK', 40, -5] },
      { id: 'master_fighter', n: 'Master Fighter', i: 'MACE', x: 2, y: 0, m: 3, c: 8, ci: 4, ra: ['dash', 'speed_burst'], col: '#A85A28', d: ['<body>You land critical hits', '<body>more often while moving.'], b: '{value}% crit', bm: 100, pl: 0.03 },
      { id: 'wind_spirit', n: 'Wind Spirit', i: 'ELYTRA', x: -2, y: 0, m: 3, c: 8, ci: 4, ra: ['leap', 'shockwave'], col: '#7E4479', d: ['<body>Your falls are softer,', '<body>you land more safely.'], b: '+{value} safe fall', pl: 1 },
      { id: 'holy_light', n: '<b>Holy Light</b>', i: 'BEACON', x: 0, y: 2, m: 1, c: 20, ra: ['heal', 'shield'], col: '#3F7158', d: ['<body>Permanent regeneration.'] }
    ] },
    { id: 'gatherer', name: 'Gatherer', icon: 'GOLDEN_PICKAXE', nodes: [
      { id: 'gatherer', n: 'Gatherer', i: 'GOLDEN_PICKAXE', x: 0, y: 0, m: 3, c: 3, ci: 2, r: ['miner'], av: true, col: '#A0702A', d: ['<body>Your pickaxes work', '<body>more efficiently.'], b: '+{value} efficiency', pl: 1 },
      { id: 'vein_hunter', n: 'Vein Hunter', i: 'DIAMOND_PICKAXE', x: 1, y: 0, m: 4, c: 5, ci: 3, r: ['gatherer'], col: '#B98433', d: ['<hi>Sneak</hi> <body>and break an ore,', '<body>the whole vein breaks too.'], b: '+{value} blocks', pl: 4 },
      { id: 'molten_pickaxe', n: 'Molten Pickaxe', i: 'BLAST_FURNACE', x: 2, y: 0, m: 4, c: 5, ci: 2, r: ['vein_hunter'], col: '#B98433', d: ['<body>Ores melt into ingots', '<body>as you break them.'], b: '{value}% chance', bm: 100, pl: 0.25 },
      { id: 'need_for_speed', n: 'Need for Speed', i: 'GOLDEN_SHOVEL', x: 0.5, y: -1, m: 2, c: 8, ci: 8, r: ['gatherer'], col: '#B98433', d: ['<body>Permanent <value>Haste I</value> and', '<body>faster mining.'], b: '+{value}% mining speed', bm: 100, pl: 0.05 },
      { id: 'lucky_rod', n: 'Lucky Rod', i: 'FISHING_ROD', x: -0.5, y: -1, m: 3, c: 3, ci: 2, r: ['gatherer'], col: '#4F829A', d: ['<body>Better loot', '<body>when fishing.'], b: '+{value} luck', bp: 0.5 },
      { id: 'lumberjack', n: 'Lumberjack', i: 'IRON_AXE', x: -1, y: 0, m: 4, c: 4, ci: 2, r: ['gatherer'], col: '#66802B', d: ['<hi>Sneak</hi> <body>and break a log,', '<body>the whole tree falls.'], b: '+{value} logs', pl: 16 },
      { id: 'forest_keeper', n: 'Forest Keeper', i: 'OAK_SAPLING', x: -2, y: 0, m: 3, c: 4, ci: 2, r: ['lumberjack'], col: '#66802B', d: ['<body>A chance for double', '<body>wood from logs.'], b: '{value}% chance', bm: 100, pl: 0.1 },
      { id: 'bounty', n: 'Bounty', i: 'WHEAT', x: 0.5, y: 1, m: 4, c: 3, ci: 2, r: ['gatherer'], col: '#557232', d: ['<body>A chance for double', '<body>crops from ripe plants.'], b: '{value}% chance', bm: 100, pl: 0.08 },
      { id: 'green_thumb', n: 'Green Thumb', i: 'MELON_SLICE', x: -0.5, y: 1, m: 3, c: 3, ci: 2, r: ['gatherer'], col: '#557232', d: ['<body>Double drops from melons,', '<body>pumpkins and sugar cane.'], b: '{value}% chance', bm: 100, pl: 0.08 }
    ] }
  ];

  const TRIGGERS = { SNEAK_SWAP: 'Sneak + F', DOUBLE_SNEAK: 'Double Sneak', SNEAK_JUMP: 'Sneak + Jump' };

  // Style.java: background, panel-border, header, card, locked, line, hidden-line, buy, deny, gold, maxed, hover (ARGB),
  // then tint and the message colours hi, body, dim, accent, value, bad, good, info, magic.
  const STYLES = {
    NIGHT: { theme: ['FF0B1018', 'FF2B3B55', 'B80A0F18', 'B80A0F18', 'FF151D2A', 'FF7F9CC4', '8C2C3A50', 'FF3D6B9E', 'FF9B3A45', 'FFD3E3F5', 'FFE3C27A', 'FFFFFFFF'], tint: 0.5,
      palette: ['#EEF3FA', '#A9B6CA', '#647189', '#9CC3F0', '#DDE7F3', '#E07A7A', '#8DCFA6', '#9CC3F0', '#B8A9E6'] },
    CELESTIAL: { theme: ['FF0C0D12', 'FF5A4A2E', 'BF0B0B10', 'BF0B0B10', 'FF17161B', 'FF8C7A55', '8C3A352C', 'FF7E6230', 'FF8E3A36', 'FFE2C27A', 'FFF4E4B8', 'FFFFF6E0'], tint: 0.3,
      palette: ['#F3EEE2', '#C5BFB1', '#7B766B', '#E2C27A', '#F1DFA8', '#DE806F', '#A8CB8C', '#AFC4DB', '#C6AEDB'] },
    MINIMAL: { theme: ['FF0A0A0C', '59FFFFFF', '99000000', '99000000', 'FF1A1B1F', 'FF9A9CA3', '40FFFFFF', 'FF3A3C42', 'FF7A2E2E', 'FFE6E6E6', 'FF9AD3A0', 'FFFFFFFF'], tint: 0,
      palette: ['#FFFFFF', '#BDBEC2', '#6E7076', '#FFFFFF', '#E6E6E6', '#E27C7C', '#9AD3A0', '#BDBEC2', '#C9C2DE'] },
    WOOD: { theme: ['FF21170F', 'FF6E5233', 'FF2F2218', 'FF2A1E15', 'FF3A2D22', 'FFA88B5E', 'FF33261B', 'FF5E7B35', 'FF8E3026', 'FFD2A24C', 'FF84A060', 'FFEFE3C6'], tint: 1,
      palette: ['#EFE3C6', '#C4AE88', '#8C7559', '#D2A24C', '#E2C27A', '#C8553D', '#9DB565', '#9FB6C8', '#B79AD0'] }
  };

  // chests.yml: rarity weights are the CS2 odds.
  const CHEST_RARITIES = [
    { id: 'common', name: 'Common', color: '#9AA4B2', weight: 79.92 },
    { id: 'rare', name: 'Rare', color: '#5F8FC4', weight: 15.98 },
    { id: 'epic', name: 'Epic', color: '#9A6BC4', weight: 3.2 },
    { id: 'legendary', name: 'Legendary', color: '#D27A33', weight: 0.64, announce: true },
    { id: 'mythic', name: 'Mythic', color: '#E0B54A', weight: 0.26, announce: true }
  ];

  // [item, amount, rarity, custom name, enchantments]; LEVELS = XP levels
  const CHESTS = [
    { id: 'iron', name: 'Iron Chest', icon: 'CHEST_IRON', color: '#8E96A3', price: 5, rewards: [
      ['IRON_INGOT', 8, 'common'], ['BREAD', 16, 'common'], ['ARROW', 32, 'common'], ['COAL', 24, 'common'], ['LEVELS', 3, 'common'],
      ['GOLD_INGOT', 8, 'rare'], ['IRON_PICKAXE', 1, 'rare', 'Swift Iron Pickaxe', 'Efficiency III, Unbreaking II'], ['LEVELS', 8, 'rare'],
      ['DIAMOND', 3, 'epic'], ['GOLDEN_APPLE', 2, 'epic'], ['DIAMOND', 8, 'legendary'], ['LEVELS', 25, 'legendary'],
      ['ENCHANTED_GOLDEN_APPLE', 1, 'mythic'], ['NETHERITE_SCRAP', 1, 'mythic']] },
    { id: 'diamond', name: 'Diamond Chest', icon: 'CHEST_DIAMOND', color: '#5E9A94', price: 15, rewards: [
      ['DIAMOND', 1, 'common'], ['EMERALD', 8, 'common'], ['ENDER_PEARL', 4, 'common'], ['GOLD_INGOT', 12, 'common'], ['LEVELS', 10, 'common'],
      ['DIAMOND', 4, 'rare'], ['GOLDEN_APPLE', 3, 'rare'], ['DIAMOND_PICKAXE', 1, 'rare', 'Diamond Miner', 'Efficiency IV, Unbreaking III'], ['LEVELS', 20, 'rare'],
      ['DIAMOND_SWORD', 1, 'epic', 'Sharp Diamond Sword', 'Sharpness V, Unbreaking III'], ['TOTEM_OF_UNDYING', 1, 'epic'], ['LEVELS', 35, 'epic'],
      ['NETHERITE_SCRAP', 4, 'legendary'], ['DIAMOND_BLOCK', 4, 'legendary'], ['NETHERITE_INGOT', 2, 'mythic'], ['ENCHANTED_GOLDEN_APPLE', 2, 'mythic']] },
    { id: 'netherite', name: 'Netherite Chest', icon: 'CHEST_NETHERITE', color: '#8E7FB8', price: 35, rewards: [
      ['DIAMOND', 5, 'common'], ['ANCIENT_DEBRIS', 2, 'common'], ['EXPERIENCE_BOTTLE', 32, 'common'], ['LEVELS', 25, 'common'],
      ['NETHERITE_SCRAP', 4, 'rare'], ['TOTEM_OF_UNDYING', 1, 'rare'], ['LEVELS', 45, 'rare'],
      ['NETHERITE_INGOT', 1, 'epic'], ['DIAMOND_PICKAXE', 1, 'epic', 'Deep Digger', 'Efficiency V, Fortune III, Unbreaking III, Mending'], ['LEVELS', 70, 'epic'],
      ['NETHERITE_SWORD', 1, 'legendary', 'Midnight', 'Sharpness V, Unbreaking III, Mending'], ['ELYTRA', 1, 'legendary'],
      ['NETHER_STAR', 1, 'mythic'], ['NETHERITE_INGOT', 4, 'mythic'], ['LEVELS', 150, 'mythic']] }
  ];

  // lootbags.yml. Entry: "ITEM amount [wWeight] [*Enchantments][|Custom name]"; chance is percent per animal (monsters x2).
  const LOOT = [
    { id: 'common', name: 'Common Loot Bag', color: '#B4BCC8', chance: 1.5, loot: ['BREAD 3-8', 'COOKED_BEEF 2-6', 'BAKED_POTATO 3-8', 'APPLE 2-5', 'COAL 4-12', 'TORCH 8-24', 'ARROW 8-24', 'OAK_LOG 4-12', 'COBBLESTONE 16-32', 'STRING 2-6', 'LEATHER 2-5', 'FEATHER 3-8', 'BONE_MEAL 4-10', 'IRON_NUGGET 4-12'] },
    { id: 'uncommon', name: 'Uncommon Loot Bag', color: '#5BD35B', chance: 0.6, loot: ['IRON_INGOT 2-6', 'COPPER_INGOT 6-16', 'GOLD_NUGGET 6-18', 'COOKED_PORKCHOP 4-8', 'GOLDEN_CARROT 2-6', 'REDSTONE 6-16', 'LAPIS_LAZULI 4-12', 'GUNPOWDER 2-6', 'SLIME_BALL 2-5', 'EXPERIENCE_BOTTLE 2-5', 'BOOK 2-5', 'LEAD 1-2', 'IRON_PICKAXE 1 w0.6', 'IRON_SWORD 1 w0.6'] },
    { id: 'rare', name: 'Rare Loot Bag', color: '#4FA3FF', chance: 0.25, loot: ['IRON_INGOT 6-14', 'GOLD_INGOT 4-10', 'DIAMOND 1-2 w0.6', 'EMERALD 3-8', 'ENDER_PEARL 2-4', 'EXPERIENCE_BOTTLE 4-10', 'GOLDEN_APPLE 1 w0.6', 'OBSIDIAN 4-8', 'BLAZE_ROD 1-3', 'QUARTZ 8-16', 'AMETHYST_SHARD 4-10', 'NAME_TAG 1 w0.7', 'SADDLE 1 w0.7', 'BOW 1 w0.6 *Power II', 'LEVELS 3-6'] },
    { id: 'epic', name: 'Epic Loot Bag', color: '#B062FF', chance: 0.08, loot: ['DIAMOND 2-5', 'GOLD_BLOCK 1-2', 'EMERALD 8-16', 'GOLDEN_APPLE 1-3', 'ENDER_PEARL 4-8', 'EXPERIENCE_BOTTLE 8-16', 'IRON_BLOCK 2-4', 'BLAZE_ROD 3-6', 'GHAST_TEAR 1-3', 'ENDER_EYE 2-4', 'NAUTILUS_SHELL 1-2', 'NETHERITE_SCRAP 1 w0.4', 'ENCHANTED_BOOK 1 w0.5 *Unbreaking III', 'DIAMOND_SWORD 1 w0.6 *Sharpness III, Unbreaking II', 'DIAMOND_PICKAXE 1 w0.6 *Efficiency III, Unbreaking II', 'LEVELS 6-10'] },
    { id: 'legendary', name: 'Legendary Loot Bag', color: '#FFA41C', chance: 0.025, announce: true, loot: ['DIAMOND 6-12', 'DIAMOND_BLOCK 1 w0.6', 'NETHERITE_SCRAP 1-3', 'ANCIENT_DEBRIS 2-4', 'GOLDEN_APPLE 3-6', 'EMERALD_BLOCK 2-4', 'EXPERIENCE_BOTTLE 16-32', 'TOTEM_OF_UNDYING 1 w0.7', 'SHULKER_SHELL 2 w0.7', 'TRIDENT 1 w0.4', 'HEART_OF_THE_SEA 1 w0.4', 'WITHER_SKELETON_SKULL 1 w0.5', 'ENCHANTED_BOOK 1 w0.6 *Mending', 'DIAMOND_CHESTPLATE 1 w0.6 *Protection IV, Unbreaking III', 'DIAMOND_SWORD 1 w0.6 *Sharpness V, Unbreaking III, Looting II', 'LEVELS 10-15'] },
    { id: 'mythic', name: 'Mythic Loot Bag', color: '#FF4F6B', chance: 0.006, announce: true, loot: ['NETHERITE_INGOT 1-2', 'ENCHANTED_GOLDEN_APPLE 1', 'TOTEM_OF_UNDYING 1-2', 'DIAMOND_BLOCK 2-4', 'ANCIENT_DEBRIS 4-8', 'NETHERITE_UPGRADE_SMITHING_TEMPLATE 1-2', 'SHULKER_SHELL 4-6', 'EXPERIENCE_BOTTLE 32-64', 'ELYTRA 1 w0.4', 'NETHER_STAR 1 w0.3', 'ENCHANTED_BOOK 1 w0.8 *Mending', 'TRIDENT 1 w0.6 *Loyalty III, Impaling III', 'DIAMOND_PICKAXE 1 w0.6 *Efficiency V, Unbreaking III, Fortune III, Mending|Mythic Pickaxe', 'DIAMOND_SWORD 1 w0.6 *Sharpness V, Unbreaking III, Looting III, Mending|Mythic Blade', 'LEVELS 15-25'] },
    { id: 'secret', name: 'Secret Loot Bag', color: '#9D6BFF', chance: 0.0012, announce: true, loot: ['NETHERITE_INGOT 3-5', 'ENCHANTED_GOLDEN_APPLE 2-4', 'TOTEM_OF_UNDYING 2-3', 'ELYTRA 1', 'NETHER_STAR 1-2', 'BEACON 1', 'HEAVY_CORE 1', 'DRAGON_HEAD 1', 'DIAMOND_BLOCK 6-12', 'SHULKER_BOX 1-2', 'EXPERIENCE_BOTTLE 64', 'ENCHANTED_BOOK 1 *Mending', 'NETHERITE_SWORD 1 *Sharpness V, Unbreaking III, Looting III, Fire Aspect II, Mending|Secret Blade', 'NETHERITE_CHESTPLATE 1 *Protection IV, Unbreaking III, Mending|Secret Plate', 'LEVELS 30'] }
  ];

  window.UT = window.UT || {};
  window.UT.data = { TREES: TREES, TRIGGERS: TRIGGERS, STYLES: STYLES, CHEST_RARITIES: CHEST_RARITIES, CHESTS: CHESTS, LOOT: LOOT };
})();
