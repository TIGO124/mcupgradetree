package com.upgradetree.lootbag;

import com.upgradetree.config.Problems;
import com.upgradetree.config.Texts;
import com.upgradetree.util.Palette;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

/** Everything read from lootbags.yml: settings, the rarities of loot bags and what they can hold. */
public final class LootBagRegistry {

    public static final String FILE = "lootbags.yml";
    private static final int DEFAULT_COLOR = 0xFFB4BCC8;
    private static final String DEFAULT_MODEL = "minecraft:bundle";
    private static final int MAX_AMOUNT = 999;
    private static final int MAX_LEVELS = 1000;
    private static final int MAX_ITEMS = 54;
    private static final List<String> DEFAULT_REASONS = List.of("SPAWNER", "SPAWNER_EGG", "DISPENSE_EGG", "SLIME_SPLIT",
            "BUILD_IRONGOLEM", "BUILD_SNOWMAN");

    public final boolean enabled;
    /** Hostile mobs drop bags this many times as often as the others. */
    public final double hostileMultiplier;
    /** A dropped bag shows its name above it. */
    public final boolean showName;
    /** Mobs spawned these ways never drop bags (farm protection). */
    public final Set<SpawnReason> ignoredSpawnReasons;
    private final List<LootBagType> types;
    private final Map<String, LootBagType> byId = new LinkedHashMap<>();
    private final List<String> issues;

    private LootBagRegistry(boolean enabled, double hostileMultiplier, boolean showName, Set<SpawnReason> ignored,
                            List<LootBagType> types, List<String> issues) {
        this.enabled = enabled;
        this.hostileMultiplier = hostileMultiplier;
        this.showName = showName;
        this.ignoredSpawnReasons = ignored.isEmpty() ? Set.of() : Set.copyOf(ignored);
        this.types = List.copyOf(types);
        for (LootBagType t : this.types) {
            byId.put(t.id, t);
        }
        this.issues = List.copyOf(issues);
    }

    public static LootBagRegistry empty() {
        return new LootBagRegistry(false, 2, true, Set.of(), List.of(), List.of());
    }

    /** Reads plugins/UpgradeTree/lootbags.yml (the server must be running: items and enchantments are checked). */
    public static LootBagRegistry load(File dataFolder, Logger log) {
        File f = new File(dataFolder, FILE);
        YamlConfiguration y = new YamlConfiguration();
        Problems.Sink sink = Problems.log(log);
        try {
            y.load(f);
        } catch (Exception ex) {
            sink.warn(FILE + " could not be read, loot bags are off: " + ex.getMessage());
            return empty();
        }
        var enchantments = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        return parse(y, sink, m -> m.isItem() && !m.isAir(), key -> {
            NamespacedKey k = NamespacedKey.fromString(key);
            return k != null && enchantments.get(k) != null;
        });
    }

    /** Parses a loot bag file. The predicates check items and enchantment keys (tests pass "always true"). */
    public static LootBagRegistry parse(ConfigurationSection y, Problems.Sink sink, Predicate<Material> isItem,
                                        Predicate<String> isEnchantment) {
        List<String> issues = new ArrayList<>();
        Problems.Sink warn = problem -> {
            issues.add(problem);
            sink.warn(problem);
        };
        boolean enabled = y.getBoolean("enabled", true);
        double multiplier = Math.max(0, Math.min(100, y.getDouble("hostile-multiplier", 2)));
        int[] items = items(y, "items", new int[]{5, 10}, FILE + " items", warn);

        Set<SpawnReason> ignored = EnumSet.noneOf(SpawnReason.class);
        for (String r : y.isList("ignored-spawn-reasons") ? y.getStringList("ignored-spawn-reasons") : DEFAULT_REASONS) {
            try {
                ignored.add(SpawnReason.valueOf(r.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                warn.warn(FILE + " ignored-spawn-reasons: unknown spawn reason '" + r + "'.");
            }
        }

        List<LootBagType> types = new ArrayList<>();
        ConfigurationSection rs = y.getConfigurationSection("rarities");
        if (rs != null) {
            for (String id : rs.getKeys(false)) {
                ConfigurationSection s = rs.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                String ctx = FILE + " rarities." + id;
                String key = id.toLowerCase(Locale.ROOT);
                if (!id.matches("[A-Za-z0-9_-]{1,32}")) {
                    warn.warn(ctx + ": a rarity id may only contain letters, digits, _ and -, skipped.");
                    continue;
                }
                if (types.stream().anyMatch(t -> t.id.equals(key))) {
                    warn.warn(ctx + ": the id is used twice, skipped.");
                    continue;
                }
                List<LootEntry> loot = loot(ctx, s.getMapList("loot"), isItem, isEnchantment, warn);
                int[] range = items(s, "items", items, ctx + " items", warn);
                String model = s.getString("model", DEFAULT_MODEL).trim().toLowerCase(Locale.ROOT);
                if (NamespacedKey.fromString(model) == null) {
                    warn.warn(ctx + ": invalid model '" + model + "', using " + DEFAULT_MODEL + ".");
                    model = DEFAULT_MODEL;
                }
                double chance = s.getDouble("chance", 0);
                if (chance < 0 || chance > 100) {
                    warn.warn(ctx + ": chance must be 0-100 (percent), using " + (chance < 0 ? 0 : 100) + ".");
                    chance = chance < 0 ? 0 : 100;
                }
                LootBagType t = new LootBagType(key, Texts.text(s.get("name"), id), Palette.parse(s.getString("color"), DEFAULT_COLOR),
                        chance, model, s.getBoolean("glint", false), s.getBoolean("glow", false), s.getBoolean("announce", false),
                        types.size(), range[0], range[1], loot);
                if (t.maxItems() == 0) {
                    warn.warn(ctx + ": nothing can be inside (no loot, or every weight is 0), skipped.");
                    continue;
                }
                types.add(t);
            }
        }
        return new LootBagRegistry(enabled, multiplier, y.getBoolean("show-name", true), ignored, types, issues);
    }

    /** "items: 5-10", "items: 7" or "items: { min: 5, max: 10 }"; the default when missing or wrong. */
    private static int[] items(ConfigurationSection s, String path, int[] def, String ctx, Problems.Sink warn) {
        if (!s.contains(path)) {
            return def;
        }
        int[] r = s.isConfigurationSection(path)
                ? new int[]{s.getInt(path + ".min", def[0]), s.getInt(path + ".max", def[1])}
                : range(s.get(path));
        if (r == null || r[0] < 1 || r[1] < r[0] || r[1] > MAX_ITEMS) {
            warn.warn(ctx + ": must be 1-" + MAX_ITEMS + " items (e.g. 5-10), using " + def[0] + "-" + def[1] + ".");
            return def;
        }
        return r;
    }

    private static List<LootEntry> loot(String ctx, List<Map<?, ?>> maps, Predicate<Material> isItem,
                                        Predicate<String> isEnchantment, Problems.Sink warn) {
        List<LootEntry> out = new ArrayList<>();
        int i = 0;
        for (Map<?, ?> m : maps) {
            String where = ctx + " loot[" + i++ + "]";
            double weight = Math.max(0, num(m.get("weight"), 1));
            if (m.get("levels") != null) {
                int[] levels = range(m.get("levels"));
                if (levels == null || levels[0] < 1 || levels[1] > MAX_LEVELS) {
                    warn.warn(where + ": levels must be 1-" + MAX_LEVELS + " (e.g. 5 or 3-6), skipped.");
                    continue;
                }
                out.add(new LootEntry(out.size(), LootEntry.Type.LEVELS, Material.EXPERIENCE_BOTTLE, levels[0], levels[1],
                        weight, null, List.of(), Map.of()));
                continue;
            }
            String raw = str(m.get("item"));
            Material mat = raw.isEmpty() ? null : Material.matchMaterial(raw);
            if (mat == null || !isItem.test(mat)) {
                warn.warn(where + ": unknown item '" + raw + "', skipped.");
                continue;
            }
            int[] amount = m.get("amount") == null ? new int[]{1, 1} : range(m.get("amount"));
            if (amount == null || amount[0] < 1 || amount[1] > MAX_AMOUNT) {
                warn.warn(where + ": amount must be 1-" + MAX_AMOUNT + " (e.g. 4 or 2-6), using 1.");
                amount = new int[]{1, 1};
            }
            Map<String, Integer> ench = new LinkedHashMap<>();
            if (m.get("enchantments") instanceof Map<?, ?> em) {
                for (Map.Entry<?, ?> e : em.entrySet()) {
                    String k = str(e.getKey()).toLowerCase(Locale.ROOT);
                    if (!isEnchantment.test(k)) {
                        warn.warn(where + ": unknown enchantment '" + k + "', left out.");
                        continue;
                    }
                    ench.put(k, (int) Math.round(num(e.getValue(), 1)));
                }
            }
            out.add(new LootEntry(out.size(), LootEntry.Type.ITEM, mat, amount[0], amount[1], weight,
                    Texts.text(m.get("name"), null), Texts.lines(m.get("lore")), ench));
        }
        return out;
    }

    /** "3" or 3 → [3, 3], "2-6" → [2, 6]; null when it is neither (or the range is backwards). */
    static int[] range(Object o) {
        if (o instanceof Number n) {
            int v = (int) Math.round(n.doubleValue());
            return new int[]{v, v};
        }
        String s = str(o).replace(" ", "");
        int dash = s.indexOf('-', 1); // a leading minus belongs to the number
        try {
            if (dash < 0) {
                int v = Integer.parseInt(s);
                return new int[]{v, v};
            }
            int a = Integer.parseInt(s.substring(0, dash));
            int b = Integer.parseInt(s.substring(dash + 1));
            return b < a ? null : new int[]{a, b};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static double num(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return o == null ? def : Double.parseDouble(String.valueOf(o).trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * The bag a killed mob drops, or null for none. {@code random} is uniform in [0, 1). Every
     * rarity has its chance (percent); hostile mobs have it times {@link #hostileMultiplier}.
     * When the chances add up to more than 100% a bag always drops, in their proportions.
     */
    public LootBagType roll(boolean hostile, double random) {
        double total = totalChance(hostile);
        if (total <= 0) {
            return null;
        }
        double mult = hostile ? hostileMultiplier : 1;
        double r = random * Math.max(100, total);
        for (LootBagType t : types) {
            r -= t.chance * mult;
            if (r < 0) {
                return t;
            }
        }
        return null;
    }

    /** Percent chance that a killed mob drops any bag. */
    public double totalChance(boolean hostile) {
        double mult = hostile ? hostileMultiplier : 1;
        double total = 0;
        for (LootBagType t : types) {
            total += t.chance * mult;
        }
        return total;
    }

    /** True when bags drop and open (enabled and at least one rarity). */
    public boolean active() {
        return enabled && !types.isEmpty();
    }

    /** The rarities, most common first. */
    public List<LootBagType> types() {
        return types;
    }

    public LootBagType type(String id) {
        if (id == null) {
            return null;
        }
        LootBagType t = byId.get(id);
        if (t != null) {
            return t;
        }
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    /** Problems found while loading. */
    public List<String> issues() {
        return issues;
    }
}
