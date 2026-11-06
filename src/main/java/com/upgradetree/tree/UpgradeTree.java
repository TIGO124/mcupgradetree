package com.upgradetree.tree;

import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** The whole upgrade tree, loaded from tree.yml. */
public final class UpgradeTree {

    public static final int DEFAULT_COLOR = 0xFF22C55E;

    private static final Map<String, String> POTION_ALIASES = Map.ofEntries(
            Map.entry("increase_damage", "strength"),
            Map.entry("fast_digging", "haste"),
            Map.entry("slow_digging", "mining_fatigue"),
            Map.entry("damage_resistance", "resistance"),
            Map.entry("slow", "slowness"),
            Map.entry("jump", "jump_boost"),
            Map.entry("heal", "instant_health"),
            Map.entry("harm", "instant_damage"),
            Map.entry("confusion", "nausea"));

    public final String titleRaw;
    public final Component title;
    private final Map<String, UpgradeNode> nodes;
    private final Map<String, UpgradeNode> byLowerId;
    private final List<UpgradeNode[]> edges;
    private final Map<String, Integer> depth;

    private UpgradeTree(String titleRaw, Map<String, UpgradeNode> nodes) {
        this.titleRaw = titleRaw;
        this.title = Text.mm(titleRaw);
        this.nodes = Collections.unmodifiableMap(nodes);
        Map<String, UpgradeNode> lower = new HashMap<>();
        for (UpgradeNode n : nodes.values()) {
            lower.put(n.id.toLowerCase(Locale.ROOT), n);
        }
        this.byLowerId = lower;
        List<UpgradeNode[]> e = new ArrayList<>();
        for (UpgradeNode child : nodes.values()) {
            for (String r : child.requires) {
                UpgradeNode parent = nodes.get(r);
                if (parent != null) {
                    e.add(new UpgradeNode[]{parent, child});
                }
            }
        }
        this.edges = Collections.unmodifiableList(e);
        this.depth = computeDepth();
    }

    public Collection<UpgradeNode> nodes() {
        return nodes.values();
    }

    public UpgradeNode node(String id) {
        if (id == null) {
            return null;
        }
        UpgradeNode n = nodes.get(id);
        return n != null ? n : byLowerId.get(id.toLowerCase(Locale.ROOT));
    }

    /** Parent → child pairs (one per requirement). */
    public List<UpgradeNode[]> edges() {
        return edges;
    }

    public int depth(UpgradeNode n) {
        return depth.getOrDefault(n.id, 0);
    }

    public int totalMaxLevels() {
        int t = 0;
        for (UpgradeNode n : nodes.values()) {
            t += n.maxLevel;
        }
        return t;
    }

    private Map<String, Integer> computeDepth() {
        Map<String, Integer> d = new HashMap<>();
        Deque<UpgradeNode> queue = new ArrayDeque<>();
        for (UpgradeNode n : nodes.values()) {
            if (n.requires.isEmpty()) {
                d.put(n.id, 0);
                queue.add(n);
            }
        }
        while (!queue.isEmpty()) {
            UpgradeNode cur = queue.poll();
            int cd = d.get(cur.id);
            for (UpgradeNode[] e : edges) {
                if (e[0] == cur) {
                    int nd = cd + 1;
                    Integer old = d.get(e[1].id);
                    if (old == null || old < nd) {
                        if (nd <= nodes.size()) {
                            d.put(e[1].id, nd);
                            queue.add(e[1]);
                        }
                    }
                }
            }
        }
        for (UpgradeNode n : nodes.values()) {
            d.putIfAbsent(n.id, 0);
        }
        return d;
    }

    // ------------------------------------------------------------------ loading

    public static UpgradeTree empty() {
        return new UpgradeTree("<gradient:#FFE259:#FFA751><b>YÜKSELTME AĞACI</b></gradient>", new LinkedHashMap<>());
    }

    public static UpgradeTree load(File file, Logger log) throws IOException, InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.load(file);
        String title = y.getString("title", "<gradient:#FFE259:#FFA751><b>YÜKSELTME AĞACI</b></gradient>");
        Map<String, UpgradeNode> nodes = new LinkedHashMap<>();
        ConfigurationSection ns = y.getConfigurationSection("nodes");
        if (ns == null) {
            log.warning("tree.yml içinde 'nodes' bölümü yok, ağaç boş.");
            return new UpgradeTree(title, nodes);
        }
        Set<String> usedKeys = new HashSet<>();
        for (String id : ns.getKeys(false)) {
            ConfigurationSection s = ns.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            try {
                UpgradeNode node = parseNode(id, s, usedKeys, log);
                nodes.put(id, node);
            } catch (Exception ex) {
                log.warning("tree.yml: '" + id + "' düğümü okunamadı: " + ex.getMessage());
            }
        }
        // drop unknown requirements
        Map<String, String> lowerIds = new HashMap<>();
        for (String id : nodes.keySet()) {
            lowerIds.put(id.toLowerCase(Locale.ROOT), id);
        }
        Map<String, UpgradeNode> fixed = new LinkedHashMap<>();
        for (UpgradeNode n : nodes.values()) {
            List<String> req = new ArrayList<>();
            for (String r : n.requires) {
                String real = nodes.containsKey(r) ? r : lowerIds.get(r.toLowerCase(Locale.ROOT));
                if (real == null) {
                    log.warning("tree.yml: '" + n.id + "' düğümü bilinmeyen '" + r + "' düğümünü istiyor, yok sayıldı.");
                } else if (real.equals(n.id)) {
                    log.warning("tree.yml: '" + n.id + "' kendini gerektiremez.");
                } else if (!req.contains(real)) {
                    req.add(real);
                }
            }
            fixed.put(n.id, req.equals(n.requires) ? n : new UpgradeNode(n.id, n.key, n.nameRaw, n.icon, n.x, n.y,
                    n.maxLevel, n.cost, n.costIncrease, n.costMultiplier, n.costs, req, n.color, n.description,
                    n.bonus, n.bonusMultiplier, n.bonusPerLevel, n.effects));
        }
        return new UpgradeTree(title, fixed);
    }

    private static UpgradeNode parseNode(String id, ConfigurationSection s, Set<String> usedKeys, Logger log) {
        String key = sanitize(id);
        String base = key;
        int i = 2;
        while (!usedKeys.add(key)) {
            key = base + "_" + i++;
        }
        String name = s.getString("name", id);
        String iconName = s.getString("icon", "PAPER");
        Material icon = Material.matchMaterial(iconName);
        if (icon == null || !icon.isItem() || icon.isAir()) {
            log.warning("tree.yml: '" + id + "' için geçersiz ikon '" + iconName + "', PAPER kullanıldı.");
            icon = Material.PAPER;
        }
        List<Double> costs = new ArrayList<>(s.getDoubleList("costs"));
        List<String> requires = new ArrayList<>(s.getStringList("requires"));
        if (requires.isEmpty() && s.isString("requires")) {
            requires.add(s.getString("requires"));
        }
        List<String> desc = new ArrayList<>(s.getStringList("description"));
        if (desc.isEmpty() && s.isString("description")) {
            desc.add(s.getString("description"));
        }
        Double bonusPer = s.contains("bonus-per-level") ? s.getDouble("bonus-per-level") : null;

        List<NodeEffect> effects = new ArrayList<>();
        for (Map<?, ?> map : s.getMapList("effects")) {
            NodeEffect e = parseEffect(id, map, log);
            if (e != null) {
                effects.add(e);
            }
        }
        return new UpgradeNode(id, key, name, icon, s.getDouble("x", 0), s.getDouble("y", 0),
                s.getInt("max-level", 1), s.getDouble("cost", 1), s.getDouble("cost-increase", 0),
                s.getDouble("cost-multiplier", 1), costs, requires, Palette.parse(s.getString("color"), DEFAULT_COLOR),
                desc, s.getString("bonus", ""), s.getDouble("bonus-multiplier", 1), bonusPer, effects);
    }

    private static NodeEffect parseEffect(String nodeId, Map<?, ?> map, Logger log) {
        String type = str(map.get("type")).toUpperCase(Locale.ROOT);
        switch (type) {
            case "ATTRIBUTE": {
                String raw = str(map.get("attribute"));
                String k = normalizeAttribute(raw);
                Attribute attr = lookup(RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE), k);
                if (attr == null) {
                    log.warning("tree.yml: '" + nodeId + "' bilinmeyen özellik (attribute) '" + raw + "'");
                    return null;
                }
                return new NodeEffect.AttributeBonus(attr, k, num(map.get("amount"), 0), operation(str(map.get("operation"))));
            }
            case "POTION":
            case "EFFECT":
            case "POTION_EFFECT": {
                String raw = str(map.get("effect"));
                String k = raw.trim().toLowerCase(Locale.ROOT).replace("minecraft:", "").replace(' ', '_');
                k = POTION_ALIASES.getOrDefault(k, k);
                PotionEffectType t = lookup(RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT), k);
                if (t == null) {
                    log.warning("tree.yml: '" + nodeId + "' bilinmeyen iksir efekti '" + raw + "'");
                    return null;
                }
                int amp = (int) num(map.get("amplifier"), 0);
                int per = (int) num(map.get("amplifier-per-level"), 1);
                return new NodeEffect.PotionBonus(t, amp, per);
            }
            case "XP_BOOST":
            case "XP":
                return new NodeEffect.XpBoost(num(map.get("amount"), 0));
            case "DOUBLE_DROP":
            case "DOUBLE_ORE":
            case "ORE_DOUBLE":
                return new NodeEffect.DoubleDrop(num(map.get("amount"), 0));
            case "COMMAND":
            case "COMMANDS": {
                List<String> cmds = new ArrayList<>();
                Object list = map.get("commands");
                if (list instanceof List<?> l) {
                    for (Object o : l) {
                        cmds.add(String.valueOf(o));
                    }
                }
                Object single = map.get("command");
                if (single != null) {
                    cmds.add(String.valueOf(single));
                }
                return cmds.isEmpty() ? null : new NodeEffect.Commands(cmds);
            }
            default:
                log.warning("tree.yml: '" + nodeId + "' bilinmeyen efekt tipi '" + type + "'");
                return null;
        }
    }

    private static <T extends org.bukkit.Keyed> T lookup(Registry<T> registry, String key) {
        try {
            return registry.get(NamespacedKey.minecraft(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String normalizeAttribute(String s) {
        String k = s.trim().toLowerCase(Locale.ROOT);
        if (k.startsWith("minecraft:")) {
            k = k.substring("minecraft:".length());
        }
        for (String p : new String[]{"generic.", "player.", "zombie.", "horse.", "generic_", "player_"}) {
            if (k.startsWith(p)) {
                k = k.substring(p.length());
                break;
            }
        }
        return k.replace('.', '_').replace(' ', '_');
    }

    private static AttributeModifier.Operation operation(String s) {
        switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "ADD_SCALAR":
            case "ADD_MULTIPLIED_BASE":
            case "MULTIPLY_BASE":
                return AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY_SCALAR_1":
            case "ADD_MULTIPLIED_TOTAL":
            case "MULTIPLY_TOTAL":
                return AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default:
                return AttributeModifier.Operation.ADD_NUMBER;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static double num(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o != null) {
            try {
                return Double.parseDouble(String.valueOf(o).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return def;
    }

    static String sanitize(String id) {
        StringBuilder sb = new StringBuilder();
        String lower = id.toLowerCase(Locale.ROOT)
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u').replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
        for (char c : lower.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.length() == 0 ? "node" : sb.toString();
    }
}
