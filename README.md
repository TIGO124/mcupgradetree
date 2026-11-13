# UpgradeTree 2.3 — Hexagon Upgrade Tree for Minecraft 26.3

A hexagon upgrade tree. Instead of a chest menu, a 3D panel opens in front of the player and only they can see it:
hexagon upgrades laid out like a honeycomb over a background picture (a blue nebula by default). Buying an upgrade
reveals the ones that follow it (progressive reveal). The look comes from ready-made styles.

## Installation
1. Server: Paper 26.3 (or a Paper fork such as Purpur or Leaf), Java 25.
2. Put `UpgradeTree-2.3.0.jar` into the server's `plugins` folder and restart.
3. Settings: `plugins/UpgradeTree/config.yml`, trees: `trees/*.yml` (every file is a tab), chests: `chests.yml`,
   messages: `messages.yml`.

### Updating from 2.2 or older
Everything is converted on the first start, nothing has to be done by hand:
- Built-in trees and `chests.yml` that were never edited are replaced by the new files (`trees/main.yml`,
  `trees/abilities.yml`, `trees/gatherer.yml`). The old files stay as `*.bak`. Edited files are kept as they are.
- Player levels and unopened chests move to the new upgrade and chest ids by themselves (for example `keskinlik` →
  `sharp_sword`, `demir` → `iron`). Old ids in commands, placeholders and the API still work.
- `config.yml` gets the new comments; every value is kept (the old file is saved as `config.yml.bak`).
- The plugin is English only: the `lang/` folder is no longer read and can be deleted. All texts are in `messages.yml`.

## Features
- **Hexagon panel:** the frame of an upgrade shows its state (can be bought, owned, finished), its fill takes the colour
  of its branch. Affordable upgrades pulse. A progress line and bar show how much of the tree is finished.
- **Styles:** `menu.style`: `NIGHT` (night glass), `CELESTIAL` (black and champagne gold), `MINIMAL` (black and white),
  `WOOD` (walnut planks). A style colours the panel, the chest menus and the messages together. Single colours can be
  overridden in `config.yml` → `theme` (`#AARRGGBB` for see-through colours).
- **Background picture:** `plugins/UpgradeTree/background.jpg` (PNG works too, `menu.background-image`). The plugin cuts
  the picture to the panel's shape and puts it into a small resource pack that players get when they join
  (`resource-pack.mode`):
  - `SELF_HOST` (default): the plugin serves the pack from its own small web server; `resource-pack.port` (8164) must be
    open to the outside. Hosting panels may need an extra port.
  - `URL`: upload `plugins/UpgradeTree/resourcepack.zip` somewhere and put its address into `resource-pack.url`.
  - `EXTERNAL`: add the contents of the pack to the server's own resource pack yourself.
  - Players who decline or cannot download the pack see the style's plain colour. `menu.background: NONE | STARS | PLANKS`
    turns the picture off.
- **Message colours:** `messages.yml` uses the colour names of the style: `<hi>` `<body>` `<dim>` `<accent>` `<value>`
  `<bad>` `<good>` `<info>` `<magic>`. Normal MiniMessage colours work too.
- **Progressive reveal:** an upgrade appears once one of the upgrades it requires is bought (`menu.progressive-reveal`).
- **Tabs:** every `trees/*.yml` file is a tree of its own. Built in: Main Tree, Abilities, Gatherer. The **Chests**
  tab comes last.
- **Chests (CS2-style cases):** chests are bought in the Chests section of the menu. A bought chest appears in front of
  the player; a right click shakes it, opens the lid and spins a strip of items over it that slows down and stops on
  the prize. Prizes are Minecraft items or XP levels with CS2-like rarity colours and odds.
- **Advanced requirements:** `requires`, `requires-any` (one is enough), `requires-level` (a certain level),
  `exclusive-with` (class choice), `permission`, `min-prestige`.
- **Effects:** attributes (health, damage, armor, speed, reach, size...), permanent potions, XP boost, double drops
  (with block lists), critical hits, lifesteal, dodge, auto smelt, vein miner, tree feller, mob loot, keeping XP or items
  on death, hunger saving, console commands.
- **Active abilities:** Dash, Leap, Heal, Shield, Speed Burst, Shockwave. Triggers: Sneak + F, sneak twice, Sneak + Jump.
- **Respec:** 75% of the spent currency comes back (configurable), with a cooldown.
- **Prestige:** once the tree is finished everything resets, permanent bonuses grow and costs go up.
- **Currency:** XP levels, Vault money or the plugin's own points. Point sources: mob and player kills (farm and alt
  account protection), playtime, mining natural ores, advancements, a daily reward.
- **Leaderboard:** `/tigo top` and a floating hologram.
- **Bedrock (Geyser/Floodgate):** these players use the chest menu automatically. Anyone can choose with
  `/tigo menu chest|panel`.
- **Data:** YAML, SQLite or MySQL. Changes are saved in batches. YAML data is copied into an empty database by itself.
- **Access:** command, optional menu item, tagged NPC (`/tigo npc`), "new upgrade available" notifications.
- **Restrictions:** `restrictions.disabled-worlds`, a menu cooldown after combat.
- **Free movement:** the panel stays where it was opened and the player keeps walking. Clicks go to the menu while
  looking at the panel; looking away the game works as usual. Walking `menu.max-distance` blocks away closes the menu.
  `menu.lock-player: true` brings back the old locked mode.
- **Admin panel (operators only):** `/tigo admin [player]`. Player list, points and prestige editing, upgrade levels
  (left +1, right −1, Shift+left max, Shift+right 0), max all / reset all, removing bonuses, one-click settings
  (progressive reveal, click to buy, player lock, currency, menu type), hologram, reload and tree check.
  "Edit in the 3D tree" opens the player's tree on the hexagon panel: every upgrade is visible and a click changes its level.

## Usage
- `/tigo` opens and closes the menu. Every command is `/tigo <command>`.
- **Key binding:** Options › Controls › **Quick Actions** opens a small window whose **Open the tree** button opens the
  tree. Every player can pick the key. `access.quick-action: false` turns it off (needs a restart).
- Aim at a hexagon with the crosshair and **left click** to buy. The panel stays put, you can walk around it. Costs of
  10 and more need a second click (`menu.confirm-cost`).
- Switch trees with the tabs at the top. **Reset** and **Prestige** need two clicks. Looking at the panel, **Sneak** or
  the X closes it.

| Command | Description |
|---|---|
| `/tigo info [player]` | Shows the upgrades and the command list |
| `/tigo chests` | Opens the Chests section (buy and open chests) |
| `/tigo respec [confirm]` | Resets your own tree with a refund |
| `/tigo prestige [confirm]` | Prestiges |
| `/tigo top [prestige\|levels]` | Leaderboard |
| `/tigo menu <panel\|chest\|auto>` | Menu type |
| `/tigo daily` | Daily reward (POINTS) |

Operators get the `/tigo admin [player]` panel. Admin commands (`upgradetree.admin`): `reload`, `check` (tree mistakes:
cycles, unreachable upgrades, overlapping hexagons), `reset <player>`, `set <player> <upgrade> <level>`,
`points <give|take|set> <player> <amount>`, `prestige set <player> <level>`, `cleanup <player|all>`, `npc`,
`top place|remove`, `open <player>`, `chests give <player> <chest> [amount]` (unopened chests, offline players too),
`debug <preview|look|pos|entities|target> <player> [upgrade]`.

Permissions: `upgradetree.use` (everyone) and `upgradetree.admin` (operators).

## PlaceholderAPI
Ids are the keys of the files: upgrades (`sharp_sword`), trees (`main`, `abilities`, `gatherer`), chests (`iron`).
Yes/no placeholders use PlaceholderAPI's own true/false texts.

| Placeholder | Value |
|---|---|
| `%upgradetree_points%` | Upgrade points |
| `%upgradetree_balance%` / `%upgradetree_balance_formatted%` | Balance of the active currency (`25` / `25 Levels`) |
| `%upgradetree_currency%` | Currency name |
| `%upgradetree_levels%` / `%upgradetree_max_levels%` | Levels bought / levels there are |
| `%upgradetree_owned%` / `%upgradetree_maxed%` / `%upgradetree_total%` | Upgrades owned / finished / there are |
| `%upgradetree_progress%` | Percent of the reachable levels owned |
| `%upgradetree_affordable%` | Upgrades that can be bought right now |
| `%upgradetree_chests%` | Unopened chests |
| `%upgradetree_rank%` / `%upgradetree_rank_levels%` | Place on the prestige / levels leaderboard (0 = not listed) |
| `%upgradetree_respec_ready%` / `%upgradetree_respec_cooldown%` | Respec possible / time left (`m:ss`) |
| `%upgradetree_daily_ready%` | Daily reward can be claimed |
| `%upgradetree_prestige%` / `%upgradetree_prestige_roman%` | Prestige level (`3` / `III`) |
| `%upgradetree_prestige_max%` / `%upgradetree_prestige_ready%` | Highest prestige / prestige possible now |
| `%upgradetree_ability_<trigger>%` | Ability on a trigger (`sneak_swap`, `double_sneak`, `sneak_jump`) |
| `%upgradetree_ability_<trigger>_cooldown%` | Seconds until that ability is ready |
| `%upgradetree_level_<upgrade>%` / `%upgradetree_maxlevel_<upgrade>%` | Level / highest level of an upgrade |
| `%upgradetree_cost_<upgrade>%` | Cost of its next level (empty when finished) |
| `%upgradetree_state_<upgrade>%` | `locked`, `blocked`, `available`, `owned` or `maxed` |
| `%upgradetree_owned_<tree>%` / `%upgradetree_completed_<tree>%` | Upgrades owned / finished in a tree |
| `%upgradetree_total_<tree>%` / `%upgradetree_progress_<tree>%` | Upgrades in a tree / percent of its levels owned |
| `%upgradetree_chests_<chest>%` / `%upgradetree_chest_price_<chest>%` | Unopened chests of a type / its price |
| `%upgradetree_top_<n>_name\|prestige\|levels%` | Leaderboard by prestige |
| `%upgradetree_toplevels_<n>_name\|prestige\|levels%` | Leaderboard by total levels |

Note: PlaceholderAPI 2.12.2 does not start on Paper 26.3 (version parsing error). A PlaceholderAPI version that
supports 26.x is needed.

## Chests
- **Buying:** `/tigo chests` (or the **Chests** tab of the menu). Look at a chest card and press **BUY**. The cost is
  taken in the currency of `config.yml` (XP levels, Vault or points). The grid below shows what the chest contains, in
  rarity colours; the card on the right lists the odds of every rarity, and aiming at a prize shows its own chance.
- **Opening:** the menu closes and the chest appears in front of you (only you see it). **Right click**: the chest shakes,
  the lid opens, the inside glows and a CS2-style strip of items spins above it. The prize under the golden line is
  yours (dropped on the ground when the inventory is full). Legendary and Mythic prizes are announced in chat.
- **An unopened chest** disappears after a while (or when you walk away) but stays in your chests: the **OPEN ×N**
  button of the Chests section calls it back. A chest is never lost when the server stops during an opening.
- **Bedrock:** display entities are not shown there, so the strip spins in a chest menu (inventory).
- **File:** `chests.yml` — rarities (colour, weight, announcement), chests (name, icon, cost, description, blocks of the
  chest), prizes (`item` + `amount` + optional `name`, `lore`, `enchantments`, or `levels`). Odds: first the rarity by
  its weight, then a prize of that rarity (the default weights are the CS2 odds: 79.92% / 15.98% / 3.2% / 0.64% / 0.26%).

```yaml
chests:
  diamond:
    name: "Diamond Chest"
    icon: CHEST
    price: 15
    look: { body: DARK_OAK_PLANKS, trim: DIAMOND_BLOCK, latch: GOLD_BLOCK }
    rewards:
      - { item: DIAMOND, amount: 4, rarity: rare }
      - { levels: 20, rarity: rare }
      - item: DIAMOND_SWORD
        name: "Sharp Diamond Sword"
        enchantments: { sharpness: 5, unbreaking: 3 }
        rarity: epic
```

## Tree files
An example and every setting are explained at the top of `trees/main.yml`. Positions are on a hexagon grid: side
neighbours at `x±1` (same `y`), diagonal neighbours at `x±0.5` and `y±1`. On even rows `x` is a whole number, on odd
rows it ends in `.5`.

```yaml
nodes:
  sharp_sword:
    name: "Sharp Sword"
    icon: IRON_SWORD
    x: 1
    y: 0
    max-level: 5
    cost: 2
    cost-increase: 1
    requires: [origin]
    color: "#A8452F"
    description:
      - "<body>All your attacks"
      - "<body>deal more damage."
    bonus: "+{value} damage"
    effects:
      - type: ATTRIBUTE
        attribute: attack_damage
        amount: 0.3
```

## For developers
```java
int lvl = UpgradeTreeAPI.level(player, "sharp_sword");
UpgradeTreeAPI.addPoints(player, 5);
```
Events (`com.upgradetree.api.event`): `UpgradePurchaseEvent` (can be cancelled, the cost can be changed),
`UpgradeLevelChangeEvent`, `UpgradeResetEvent`, `PrestigeEvent`, `AbilityUseEvent`.

## Building and testing
```bash
./gradlew build        # jar: build/libs/UpgradeTree-2.3.0.jar (runs the unit tests too)
./gradlew runServer    # downloads Paper 26.3 and starts a local test server with the plugin
```
GitHub Actions builds every push. A `v*` tag puts the jar on the Releases page.

## Notes
- In survival mode the game registers at most about 2 left clicks per second (a Minecraft rule).
- If the panel does not fit on a 4:3 screen: `config.yml` → `menu.scale: 0.9`.
- On busy servers `menu.quality: LOW` uses fewer entities.
- Before removing the plugin set `cleanup-mode: true` or run `/tigo cleanup all`. Otherwise the health and damage
  bonuses it gave stay on the players.
