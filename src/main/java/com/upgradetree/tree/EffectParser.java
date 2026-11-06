package com.upgradetree.tree;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Turns the {@code effects:} entries of tree files (and prestige) into {@link NodeEffect}s. */
public final class EffectParser {

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

    private EffectParser() {
    }

    public static List<NodeEffect> parseAll(String context, List<Map<?, ?>> maps, Logger log) {
        List<NodeEffect> out = new ArrayList<>();
        for (Map<?, ?> map : maps) {
            NodeEffect e = parse(context, map, log);
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    public static NodeEffect parse(String context, Map<?, ?> map, Logger log) {
        String type = str(map.get("type")).trim().toUpperCase(Locale.ROOT).replace('-', '_');
        double amount = num(map.get("amount"), 0);
        switch (type) {
            case "ATTRIBUTE" -> {
                String raw = str(map.get("attribute"));
                String k = normalizeAttribute(raw);
                Attribute attr = lookup(RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE), k);
                if (attr == null) {
                    log.warning(context + ": bilinmeyen özellik (attribute) '" + raw + "'");
                    return null;
                }
                return new NodeEffect.AttributeBonus(attr, k, amount, operation(str(map.get("operation"))));
            }
            case "POTION", "EFFECT", "POTION_EFFECT" -> {
                String raw = str(map.get("effect"));
                String k = raw.trim().toLowerCase(Locale.ROOT).replace("minecraft:", "").replace(' ', '_');
                k = POTION_ALIASES.getOrDefault(k, k);
                PotionEffectType t = lookup(RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT), k);
                if (t == null) {
                    log.warning(context + ": bilinmeyen iksir efekti '" + raw + "'");
                    return null;
                }
                return new NodeEffect.PotionBonus(t, (int) num(map.get("amplifier"), 0), (int) num(map.get("amplifier-per-level"), 1));
            }
            case "XP_BOOST", "XP" -> {
                return new NodeEffect.XpBoost(amount);
            }
            case "DOUBLE_DROP", "DOUBLE_ORE", "ORE_DOUBLE" -> {
                return new NodeEffect.DoubleDrop(amount, blocks(context, map.get("blocks"), log));
            }
            case "COMMAND", "COMMANDS" -> {
                List<String> cmds = new ArrayList<>();
                if (map.get("commands") instanceof List<?> l) {
                    for (Object o : l) {
                        cmds.add(String.valueOf(o));
                    }
                }
                if (map.get("command") != null) {
                    cmds.add(String.valueOf(map.get("command")));
                }
                return cmds.isEmpty() ? null : new NodeEffect.Commands(cmds);
            }
            case "LIFESTEAL", "LIFE_STEAL" -> {
                return new NodeEffect.Lifesteal(amount);
            }
            case "CRIT", "CRIT_CHANCE", "CRITICAL" -> {
                return new NodeEffect.Crit(amount, num(map.get("multiplier"), 1.5));
            }
            case "DODGE" -> {
                return new NodeEffect.Dodge(amount);
            }
            case "AUTO_SMELT", "SMELT" -> {
                return new NodeEffect.AutoSmelt(amount);
            }
            case "VEIN_MINER", "VEINMINER" -> {
                return new NodeEffect.VeinMiner(amount, blocks(context, map.get("blocks"), log));
            }
            case "TREE_FELLER", "TIMBER" -> {
                return new NodeEffect.TreeFeller(amount);
            }
            case "MOB_DROPS", "DOUBLE_MOB_DROP", "MOB_DROP" -> {
                return new NodeEffect.MobDrops(amount);
            }
            case "KEEP_XP" -> {
                return new NodeEffect.KeepXp(amount);
            }
            case "KEEP_INVENTORY" -> {
                return new NodeEffect.KeepInventory(amount);
            }
            case "HUNGER_SAVE", "HUNGER" -> {
                return new NodeEffect.HungerSave(amount);
            }
            case "ABILITY" -> {
                return ability(context, map, log);
            }
            default -> {
                log.warning(context + ": bilinmeyen efekt tipi '" + type + "'");
                return null;
            }
        }
    }

    private static NodeEffect ability(String context, Map<?, ?> map, Logger log) {
        NodeEffect.AbilityType type;
        NodeEffect.Trigger trigger;
        try {
            type = NodeEffect.AbilityType.valueOf(str(map.get("ability")).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warning(context + ": bilinmeyen yetenek '" + str(map.get("ability")) + "' (DASH, LEAP, HEAL, SHIELD, SPEED, NOVA)");
            return null;
        }
        String trig = str(map.get("trigger")).trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            trigger = trig.isEmpty() ? NodeEffect.Trigger.SNEAK_SWAP : NodeEffect.Trigger.valueOf(trig);
        } catch (IllegalArgumentException e) {
            log.warning(context + ": bilinmeyen tetik '" + trig + "' (SNEAK_SWAP, DOUBLE_SNEAK, SNEAK_JUMP)");
            trigger = NodeEffect.Trigger.SNEAK_SWAP;
        }
        return new NodeEffect.Ability(type, trigger, num(map.get("cooldown"), 20), num(map.get("cooldown-per-level"), 0),
                num(map.get("power"), 1), num(map.get("power-per-level"), 0));
    }

    /** Block list: material names and block tags such as "#logs" or "#minecraft:crops". */
    public static Set<Material> blocks(String context, Object raw, Logger log) {
        Set<Material> set = EnumSet.noneOf(Material.class);
        if (!(raw instanceof List<?> list)) {
            return set;
        }
        for (Object o : list) {
            String s = String.valueOf(o).trim();
            if (s.startsWith("#")) {
                String k = s.substring(1).toLowerCase(Locale.ROOT).replace("minecraft:", "");
                Tag<Material> tag = null;
                try {
                    tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(k), Material.class);
                } catch (Exception ignored) {
                    // unknown tag
                }
                if (tag == null) {
                    log.warning(context + ": bilinmeyen blok etiketi '" + s + "'");
                } else {
                    set.addAll(tag.getValues());
                }
                continue;
            }
            Material m = Material.matchMaterial(s);
            if (m == null || !m.isBlock()) {
                log.warning(context + ": bilinmeyen blok '" + s + "'");
            } else {
                set.add(m);
            }
        }
        return set;
    }

    private static <T extends Keyed> T lookup(Registry<T> registry, String key) {
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
        return switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "ADD_SCALAR", "ADD_MULTIPLIED_BASE", "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY_SCALAR_1", "ADD_MULTIPLIED_TOTAL", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> AttributeModifier.Operation.ADD_NUMBER;
        };
    }

    static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    static double num(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o != null) {
            try {
                return Double.parseDouble(String.valueOf(o).trim().replace(',', '.'));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return def;
    }
}
