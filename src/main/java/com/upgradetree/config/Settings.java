package com.upgradetree.config;

import com.upgradetree.tree.EffectParser;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.util.Palette;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Values read from config.yml (with safe defaults for every key). Messages live in {@link Lang}. */
public final class Settings {

    public enum MenuMode { AUTO, DISPLAY, CHEST }

    public final String currencyType;
    /** Configured currency name; empty = the name from messages.yml. */
    private final String currencyName;
    public final String currencySymbol;

    public final double startingPoints;
    public final double pointsPerMob;
    public final double pointsPerPlayer;
    public final Map<EntityType, Double> mobPoints = new EnumMap<>(EntityType.class);
    public final Set<CreatureSpawnEvent.SpawnReason> ignoredSpawnReasons = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
    public final long playerKillCooldownMs;
    public final int playtimeMinutes;
    public final double playtimeAmount;
    public final double miningAmount;
    public final Set<Material> miningBlocks;
    public final double advancementPoints;
    public final double dailyPoints;

    public final boolean prestigeEnabled;
    public final double prestigeRequiredPercent;
    public final int prestigeMax;
    public final double prestigeCostIncrease;
    public final boolean prestigeResetPoints;
    public final List<NodeEffect> prestigeEffects;
    public final List<String> prestigeCommands;

    public final boolean respecEnabled;
    public final double respecRefund;
    public final long respecCooldownMs;

    public final MenuMode menuMode;
    public final double menuDistance;
    public final double menuScale;
    public final boolean progressiveReveal;
    public final boolean clickToBuy;
    public final double confirmCost;
    public final boolean closeOnDamage;
    public final boolean lockPlayer;
    public final double maxDistance;
    public final long combatCooldownMs;
    public final boolean hideHand;
    public final boolean glowOnHover;
    public final boolean sounds;
    public final boolean particles;
    public final boolean animations;
    /** What is drawn on the panel behind the tree. */
    public final Background background;
    /** IMAGE background: the picture in the plugin folder (sent to players in a resource pack). */
    public final String backgroundImage;
    public final boolean highQuality;

    /** The chosen look; {@link #theme} is its colours with the overrides of config.yml "theme". */
    public final Style style;
    public final Theme theme;

    public enum Background { IMAGE, PLANKS, STARS, NONE }

    /** The style used when config.yml names none. */
    public static final Style DEFAULT_STYLE = Style.DEFAULT;

    public final ResourcePackMode packMode;
    public final int packPort;
    public final String packHost;
    public final String packUrl;
    public final boolean packRequired;

    /** How the background image reaches the players. */
    public enum ResourcePackMode { SELF_HOST, URL, EXTERNAL, OFF }

    public final boolean accessItem;
    public final Material accessItemMaterial;
    public final int accessItemSlot;
    /** Configured menu item name; empty = the name from messages.yml. */
    private final String accessItemName;
    public final String entityTag;

    public final boolean notify;
    public final long notifyCooldownMs;

    public final Set<String> disabledWorlds = new HashSet<>();

    public final int leaderboardSize;
    public final String hologram;

    public final String storageType;
    public final int autosaveSeconds;
    public final String table;
    public final String mysqlHost;
    public final int mysqlPort;
    public final String mysqlDatabase;
    public final String mysqlUser;
    public final String mysqlPassword;
    public final String mysqlParams;

    public final Set<Material> doubleDropBlocks;
    public final Set<Material> veinMinerBlocks;
    public final int treeFellerMax;

    public final boolean cleanupMode;

    /**
     * Colours of the 3D menu (ARGB; header and card may be see-through). {@code locked} is the base of
     * every node, {@code tint} how much of a node's own colour shows once it is owned (0 = none).
     * Node frames: {@code line} when it can be bought, {@code gold} when owned, {@code maxed} when finished.
     */
    public record Theme(int background, int panelBorder, int header, int card, int locked, int line,
                        int lineLocked, int buy, int deny, int gold, int maxed, int hover, double tint) {
    }

    public Settings(FileConfiguration c, Logger logger) {
        Problems.Sink log = Problems.log(logger);

        String type = c.getString("currency.type", "XP_LEVELS").trim().toUpperCase(Locale.ROOT);
        currencyType = switch (type) {
            case "VAULT", "MONEY", "ECONOMY" -> "VAULT";
            case "POINTS", "POINT" -> "POINTS";
            default -> "XP_LEVELS";
        };
        currencyName = Texts.text(c.get("currency.name"), "").trim();
        currencySymbol = c.getString("currency.symbol", "").trim();

        startingPoints = c.getDouble("points.starting-points", 0);
        pointsPerMob = c.getDouble("points.per-mob-kill", 0.3);
        pointsPerPlayer = c.getDouble("points.per-player-kill", 3);
        ConfigurationSection mp = c.getConfigurationSection("points.mob-points");
        if (mp != null) {
            for (String k : mp.getKeys(false)) {
                try {
                    mobPoints.put(EntityType.valueOf(k.trim().toUpperCase(Locale.ROOT)), mp.getDouble(k));
                } catch (IllegalArgumentException e) {
                    log.warn("config.yml: unknown mob '" + k + "' (points.mob-points)");
                }
            }
        }
        List<String> reasons = c.isList("points.ignored-spawn-reasons") ? c.getStringList("points.ignored-spawn-reasons")
                : List.of("SPAWNER", "SPAWNER_EGG", "SLIME_SPLIT", "BUILD_IRONGOLEM", "BUILD_SNOWMAN", "TRIAL_SPAWNER");
        for (String r : reasons) {
            try {
                ignoredSpawnReasons.add(CreatureSpawnEvent.SpawnReason.valueOf(r.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                log.warn("config.yml: unknown spawn reason '" + r + "' (points.ignored-spawn-reasons)");
            }
        }
        playerKillCooldownMs = Math.max(0, c.getLong("points.player-kill-cooldown", 600)) * 1000L;
        playtimeMinutes = Math.max(0, c.getInt("points.playtime.every-minutes", 10));
        playtimeAmount = c.getDouble("points.playtime.amount", 1);
        miningAmount = c.getDouble("points.mining.amount", 0.2);
        miningBlocks = blocks(c, "points.mining.blocks", log, defaultOres(log));
        advancementPoints = c.getDouble("points.advancement", 2);
        dailyPoints = c.getDouble("points.daily", 3);

        prestigeEnabled = c.getBoolean("prestige.enabled", true);
        prestigeRequiredPercent = clamp(c.getDouble("prestige.required-percent", 100), 1, 100);
        prestigeMax = Math.max(0, c.getInt("prestige.max", 10));
        prestigeCostIncrease = Math.max(0, c.getDouble("prestige.cost-increase", 0.1));
        prestigeResetPoints = c.getBoolean("prestige.reset-points", false);
        prestigeEffects = EffectParser.parseAll("config.yml prestige", c.getMapList("prestige.effects"), log);
        prestigeCommands = c.getStringList("prestige.commands");

        respecEnabled = c.getBoolean("respec.enabled", true);
        respecRefund = clamp(c.getDouble("respec.refund-percent", 75), 0, 100) / 100.0;
        respecCooldownMs = Math.max(0, c.getLong("respec.cooldown", 21600)) * 1000L;

        MenuMode mode;
        try {
            mode = MenuMode.valueOf(c.getString("menu.mode", "AUTO").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            mode = MenuMode.AUTO;
        }
        menuMode = mode;
        menuDistance = clamp(c.getDouble("menu.distance", 1.6), 0.5, 4.0);
        menuScale = clamp(c.getDouble("menu.scale", 1.0), 0.4, 2.5);
        progressiveReveal = c.getBoolean("menu.progressive-reveal", true);
        clickToBuy = c.getBoolean("menu.click-to-buy", true);
        confirmCost = Math.max(0, c.getDouble("menu.confirm-cost", 10));
        closeOnDamage = c.getBoolean("menu.close-on-damage", true);
        lockPlayer = c.getBoolean("menu.lock-player", false);
        maxDistance = clamp(c.getDouble("menu.max-distance", 8.0), 2.0, 32.0);
        combatCooldownMs = Math.max(0, c.getLong("menu.combat-cooldown", 8)) * 1000L;
        hideHand = c.getBoolean("menu.hide-hand", c.getBoolean("menu.hide-held-item", true));
        glowOnHover = c.getBoolean("menu.glow-on-hover", true);
        sounds = c.getBoolean("menu.sounds", true);
        particles = c.getBoolean("menu.particles", true);
        animations = c.getBoolean("menu.animations", true);
        highQuality = !"LOW".equalsIgnoreCase(c.getString("menu.quality", "HIGH"));

        style = Style.of(c.getString("menu.style"), DEFAULT_STYLE);
        Theme s = style.theme;
        theme = new Theme(
                color(c, "theme.background", s.background()),
                color(c, "theme.panel-border", s.panelBorder()),
                color(c, "theme.header", s.header()),
                color(c, "theme.card", s.card()),
                color(c, "theme.locked", s.locked()),
                color(c, "theme.line", s.line()),
                color(c, "theme.hidden-line", s.lineLocked()),
                color(c, "theme.buy", s.buy()),
                color(c, "theme.deny", s.deny()),
                color(c, "theme.gold", s.gold()),
                color(c, "theme.maxed", s.maxed()),
                color(c, "theme.hover", s.hover()),
                clamp(c.getDouble("theme.tint", s.tint()), 0, 1));
        String bg = c.getString("menu.background", "");
        background = switch (bg.trim().toUpperCase(Locale.ROOT)) {
            case "STARS" -> Background.STARS;
            case "NONE", "PLAIN", "FLAT" -> Background.NONE;
            case "PLANKS", "WOOD" -> Background.PLANKS;
            case "IMAGE", "PICTURE" -> Background.IMAGE;
            // older configs only had "stars": switching them off meant a plain panel
            default -> c.getBoolean("menu.stars", true) ? Background.IMAGE : Background.NONE;
        };
        backgroundImage = c.getString("menu.background-image", "background.jpg").trim();
        packMode = switch (c.getString("resource-pack.mode", "SELF_HOST").trim().toUpperCase(Locale.ROOT)) {
            case "URL" -> ResourcePackMode.URL;
            case "EXTERNAL", "MERGED" -> ResourcePackMode.EXTERNAL;
            case "OFF", "NONE", "FALSE" -> ResourcePackMode.OFF;
            default -> ResourcePackMode.SELF_HOST;
        };
        packPort = Math.max(1, Math.min(65535, c.getInt("resource-pack.port", 8164)));
        packHost = c.getString("resource-pack.host", "").trim();
        packUrl = c.getString("resource-pack.url", "").trim();
        packRequired = c.getBoolean("resource-pack.required", false);

        accessItem = c.getBoolean("access.item.enabled", false);
        Material m = Material.matchMaterial(c.getString("access.item.material", "NETHER_STAR"));
        accessItemMaterial = m == null || !m.isItem() || m.isAir() ? Material.NETHER_STAR : m;
        accessItemSlot = Math.max(0, Math.min(8, c.getInt("access.item.slot", 8)));
        accessItemName = Texts.text(c.get("access.item.name"), "").trim();
        entityTag = c.getString("access.entity-tag", "upgradetree_npc");

        notify = c.getBoolean("notify.enabled", true);
        notifyCooldownMs = Math.max(5, c.getLong("notify.cooldown", 120)) * 1000L;

        for (String w : c.getStringList("restrictions.disabled-worlds")) {
            disabledWorlds.add(w.trim().toLowerCase(Locale.ROOT));
        }

        leaderboardSize = Math.max(3, Math.min(20, c.getInt("leaderboard.size", 10)));
        hologram = c.getString("leaderboard.hologram", "");

        storageType = c.getString("storage.type", "YAML").trim().toUpperCase(Locale.ROOT);
        autosaveSeconds = Math.max(10, c.getInt("storage.autosave-seconds", 60));
        table = c.getString("storage.table", "upgradetree_players");
        mysqlHost = c.getString("storage.mysql.host", "localhost");
        mysqlPort = c.getInt("storage.mysql.port", 3306);
        mysqlDatabase = c.getString("storage.mysql.database", "minecraft");
        mysqlUser = c.getString("storage.mysql.user", "root");
        mysqlPassword = c.getString("storage.mysql.password", "");
        mysqlParams = c.getString("storage.mysql.params", "useSSL=false&autoReconnect=true");

        doubleDropBlocks = blocks(c, "effects.double-drop-blocks", log, defaultOres(log));
        veinMinerBlocks = blocks(c, "effects.vein-miner-blocks", log, defaultOres(log));
        treeFellerMax = Math.max(1, Math.min(512, c.getInt("effects.tree-feller-max", 96)));

        cleanupMode = c.getBoolean("cleanup-mode", false);
    }

    /** Currency name, e.g. "Levels" (the name of messages.yml unless config.yml sets one). */
    public String currencyName(Lang lang) {
        if (!currencyName.isEmpty()) {
            return currencyName;
        }
        String key = switch (currencyType) {
            case "POINTS" -> "currency-name-points";
            case "VAULT" -> "currency-name-money";
            default -> "currency-name-levels";
        };
        return lang == null ? currencyType : lang.raw(key);
    }

    /** Name of the menu item (the name of messages.yml unless config.yml sets one). */
    public String accessItemName(Lang lang) {
        return !accessItemName.isEmpty() || lang == null ? accessItemName : lang.raw("access-item-name");
    }

    /** True when config.yml sets its own currency name. */
    public boolean customCurrencyName() {
        return !currencyName.isEmpty();
    }

    public boolean worldDisabled(String world) {
        return !disabledWorlds.isEmpty() && disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }

    private static Set<Material> blocks(FileConfiguration c, String path, Problems.Sink log, Set<Material> def) {
        if (!c.isList(path)) {
            return def;
        }
        return EffectParser.blocks("config.yml " + path, c.getList(path), log);
    }

    private static Set<Material> defaultOres(Problems.Sink log) {
        return EffectParser.blocks("config.yml", List.of("#coal_ores", "#iron_ores", "#copper_ores", "#gold_ores",
                "#redstone_ores", "#lapis_ores", "#diamond_ores", "#emerald_ores", "NETHER_GOLD_ORE", "NETHER_QUARTZ_ORE"), log);
    }

    private static int color(FileConfiguration c, String path, int def) {
        return Palette.parse(c.getString(path), def);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
