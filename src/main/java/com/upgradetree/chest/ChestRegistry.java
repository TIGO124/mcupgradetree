package com.upgradetree.chest;

import com.upgradetree.config.LegacyIds;
import com.upgradetree.config.Problems;
import com.upgradetree.config.Texts;
import com.upgradetree.util.Palette;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/** Everything read from chests.yml: settings, rarities and the chests sold in the Chests section. */
public final class ChestRegistry {

    public static final String FILE = "chests.yml";
    private static final int DEFAULT_COLOR = 0xFF8A97A8;

    public final boolean enabled;
    /** How long the roulette spins (ticks). */
    public final int spinTicks;
    /** An unopened chest in front of a player goes back to their chests after this many ticks. */
    public final int despawnTicks;
    private final Map<String, Rarity> rarities;
    private final List<ChestType> chests;
    private final Map<String, ChestType> byId = new LinkedHashMap<>();
    private final List<String> issues;

    private ChestRegistry(boolean enabled, int spinTicks, int despawnTicks, Map<String, Rarity> rarities,
                          List<ChestType> chests, List<String> issues) {
        this.enabled = enabled;
        this.spinTicks = spinTicks;
        this.despawnTicks = despawnTicks;
        this.rarities = rarities;
        List<ChestType> sorted = new ArrayList<>(chests);
        sorted.sort(Comparator.comparingInt((ChestType c) -> c.order));
        this.chests = List.copyOf(sorted);
        for (ChestType c : this.chests) {
            byId.put(c.id, c);
        }
        this.issues = List.copyOf(issues);
    }

    public static ChestRegistry empty() {
        return new ChestRegistry(false, 120, 2400, Map.of(), List.of(), List.of());
    }

    /** Reads plugins/UpgradeTree/chests.yml (the server must be running: materials are checked). */
    public static ChestRegistry load(File dataFolder, Logger log) {
        File f = new File(dataFolder, FILE);
        YamlConfiguration y = new YamlConfiguration();
        Problems.Sink sink = Problems.log(log);
        try {
            y.load(f);
        } catch (Exception ex) {
            sink.warn(FILE + " could not be read, chests are off: " + ex.getMessage());
            return empty();
        }
        return parse(y, sink, m -> m.isItem() && !m.isAir(), Material::isBlock);
    }

    /** Parses a chests file. The predicates check materials (tests pass "always true"). */
    public static ChestRegistry parse(ConfigurationSection y, Problems.Sink sink, Predicate<Material> isItem, Predicate<Material> isBlock) {
        List<String> issues = new ArrayList<>();
        Problems.Sink warn = problem -> {
            issues.add(problem);
            sink.warn(problem);
        };
        boolean enabled = y.getBoolean("enabled", true);
        int spin = (int) Math.round(clamp(y.getDouble("spin-seconds", 6), 2, 20) * 20);
        int despawn = (int) Math.round(clamp(y.getDouble("despawn-seconds", 120), 10, 3600) * 20);

        Map<String, Rarity> rarities = new LinkedHashMap<>();
        ConfigurationSection rs = y.getConfigurationSection("rarities");
        if (rs != null) {
            int rank = 0;
            for (String id : rs.getKeys(false)) {
                ConfigurationSection s = rs.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                String key = id.toLowerCase(Locale.ROOT);
                int color = Palette.parse(s.getString("color"), 0xFFB0C3D9);
                rarities.put(key, new Rarity(key, Texts.text(s.get("name"), id), color, Math.max(0, s.getDouble("weight", 1)),
                        s.getBoolean("announce", false), rank++));
            }
        }
        if (rarities.isEmpty()) {
            rarities.put("common", new Rarity("common", "<#B0C3D9>Common", 0xFFB0C3D9, 1, false, 0));
        }

        List<ChestType> chests = new ArrayList<>();
        ConfigurationSection cs = y.getConfigurationSection("chests");
        if (cs != null) {
            int order = 0;
            for (String id : cs.getKeys(false)) {
                ConfigurationSection s = cs.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                order++;
                String ctx = FILE + " chests." + id;
                if (!id.matches("[A-Za-z0-9_-]{1,32}")) {
                    warn.warn(ctx + ": a chest id may only contain letters, digits, _ and -, skipped.");
                    continue;
                }
                List<ChestReward> rewards = rewards(ctx, s.getMapList("rewards"), rarities, isItem, warn);
                if (rewards.isEmpty()) {
                    warn.warn(ctx + ": no rewards, skipped.");
                    continue;
                }
                Map<String, Double> weights = new LinkedHashMap<>();
                ConfigurationSection ws = s.getConfigurationSection("weights");
                if (ws != null) {
                    for (String r : ws.getKeys(false)) {
                        weights.put(r.toLowerCase(Locale.ROOT), Math.max(0, ws.getDouble(r)));
                    }
                }
                ChestType.Look look = new ChestType.Look(
                        material(s.getString("look.body"), Material.SPRUCE_PLANKS, isBlock, ctx + " look.body", warn),
                        material(s.getString("look.trim"), Material.DARK_OAK_PLANKS, isBlock, ctx + " look.trim", warn),
                        material(s.getString("look.latch"), Material.IRON_BLOCK, isBlock, ctx + " look.latch", warn));
                ChestType c = new ChestType(id, Texts.text(s.get("name"), id),
                        material(s.getString("icon"), Material.CHEST, isItem, ctx + " icon", warn),
                        s.getBoolean("glint", false), s.getDouble("price", 0), s.getInt("order", order),
                        Palette.parse(s.getString("color"), DEFAULT_COLOR), Texts.lines(s.get("description")), look, rewards, weights);
                if (!c.openable()) {
                    warn.warn(ctx + ": no reward can drop (all weights are 0), skipped.");
                    continue;
                }
                chests.add(c);
            }
        }
        return new ChestRegistry(enabled, spin, despawn, rarities, chests, issues);
    }

    private static List<ChestReward> rewards(String ctx, List<Map<?, ?>> maps, Map<String, Rarity> rarities,
                                             Predicate<Material> isItem, Problems.Sink warn) {
        List<ChestReward> out = new ArrayList<>();
        Rarity fallback = rarities.values().iterator().next();
        int i = 0;
        for (Map<?, ?> m : maps) {
            String where = ctx + " rewards[" + i++ + "]";
            String rid = str(m.get("rarity")).toLowerCase(Locale.ROOT);
            Rarity rarity = rarities.get(rid);
            if (rarity == null) {
                warn.warn(where + ": unknown rarity '" + rid + "', using " + fallback.id() + ".");
                rarity = fallback;
            }
            double weight = num(m.get("weight"), 1);
            if (m.get("levels") != null) {
                int levels = (int) Math.round(num(m.get("levels"), 0));
                if (levels <= 0) {
                    warn.warn(where + ": levels must be 1 or more, skipped.");
                    continue;
                }
                out.add(new ChestReward(out.size(), ChestReward.Type.LEVELS, rarity, Material.EXPERIENCE_BOTTLE, 1,
                        null, Map.of(), List.of(), levels, weight));
                continue;
            }
            String raw = str(m.get("item"));
            Material mat = Material.matchMaterial(raw);
            if (mat == null || !isItem.test(mat)) {
                warn.warn(where + ": unknown item '" + raw + "', skipped.");
                continue;
            }
            Map<String, Integer> ench = new LinkedHashMap<>();
            if (m.get("enchantments") instanceof Map<?, ?> em) {
                for (Map.Entry<?, ?> e : em.entrySet()) {
                    ench.put(str(e.getKey()).toLowerCase(Locale.ROOT), (int) Math.round(num(e.getValue(), 1)));
                }
            }
            int amount = (int) Math.round(num(m.get("amount"), 1));
            if (amount < 1 || amount > 99) {
                int fixed = Math.max(1, Math.min(99, amount));
                warn.warn(where + ": amount must be 1-99, using " + fixed + ".");
                amount = Math.max(1, Math.min(99, amount));
            }
            out.add(new ChestReward(out.size(), ChestReward.Type.ITEM, rarity, mat, amount,
                    Texts.text(m.get("name"), null), ench, Texts.lines(m.get("lore")), 0, weight));
        }
        return out;
    }

    private static Material material(String raw, Material def, Predicate<Material> ok, String ctx, Problems.Sink warn) {
        if (raw == null || raw.isBlank()) {
            return def;
        }
        Material m = Material.matchMaterial(raw.trim());
        if (m == null || !ok.test(m)) {
            warn.warn(ctx + ": unknown material '" + raw + "', using " + def.name() + ".");
            return def;
        }
        return m;
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

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** True when the Chests section is shown (enabled and at least one chest). */
    public boolean active() {
        return enabled && !chests.isEmpty();
    }

    public List<ChestType> chests() {
        return chests;
    }

    public ChestType chest(String id) {
        if (id == null) {
            return null;
        }
        ChestType c = byId.get(id);
        if (c != null) {
            return c;
        }
        for (ChestType x : chests) {
            if (x.id.equalsIgnoreCase(id)) {
                return x;
            }
        }
        String other = LegacyIds.chest(id); // an id from before 2.3
        return other == null ? null : byId.get(other);
    }

    /** True if a chest with exactly this id is loaded (old ids do not count). */
    public boolean has(String id) {
        return byId.containsKey(id);
    }

    public Collection<Rarity> rarities() {
        return rarities.values();
    }

    /** Largest reward count of a chest (sizes the contents grid). */
    public int maxRewards() {
        int max = 0;
        for (ChestType c : chests) {
            max = Math.max(max, c.sortedRewards().size());
        }
        return max;
    }

    /** Problems found while loading. */
    public List<String> issues() {
        return issues;
    }
}
