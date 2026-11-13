package com.upgradetree.config;

import com.upgradetree.data.PlayerData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Ids of the built-in trees, upgrades and chests before 2.3, mapped to their current (English) ids.
 * Player data, requirement references, commands and placeholders that still use an old id keep
 * working: an id that does not exist is looked up under its other name.
 */
public final class LegacyIds {

    /** Old upgrade id → current upgrade id. */
    public static final Map<String, String> NODES = Map.ofEntries(
            Map.entry("baslangic", "origin"), Map.entry("keskinlik", "sharp_sword"),
            Map.entry("seri_darbe", "swift_strikes"), Map.entry("kritik", "critical_hit"),
            Map.entry("can_calma", "bloodsucker"), Map.entry("ofke", "rage"), Map.entry("kasirga", "whirlwind"),
            Map.entry("savas_ustasi", "warmaster"), Map.entry("can", "health_boost"), Map.entry("zirh", "iron_skin"),
            Map.entry("sarsilmaz", "unshakable"), Map.entry("kacinma", "shadow_step"),
            Map.entry("yenilenme", "regeneration"), Map.entry("dev_kalkan", "giant_shield"),
            Map.entry("madenci", "miner"), Map.entry("sansli_kazi", "lucky_dig"),
            Map.entry("hazine", "treasure_hunter"), Map.entry("dalgic", "diver_miner"),
            Map.entry("usta_gozu", "masters_eye"), Map.entry("bilgelik", "wisdom"), Map.entry("kedi_gozu", "cats_eye"),
            Map.entry("hafiza", "strong_memory"), Map.entry("efsane", "legend"), Map.entry("ceviklik", "agility"),
            Map.entry("basamak", "easy_step"), Map.entry("yay_bacak", "spring_legs"), Map.entry("tuy", "feather"),
            Map.entry("yunus", "dolphin"), Map.entry("tokluk", "satiety"), Map.entry("kozlenmez", "fireproof"),
            Map.entry("avci", "hunter"), Map.entry("ikinci_sans", "second_chance"),
            Map.entry("yetenek_kapisi", "ability_gate"), Map.entry("atilma", "dash"),
            Map.entry("hiz_patlamasi", "speed_burst"), Map.entry("sicrama", "leap"),
            Map.entry("sok_dalgasi", "shockwave"), Map.entry("sifa", "heal"), Map.entry("kalkan", "shield"),
            Map.entry("savasci", "master_fighter"), Map.entry("ruh", "wind_spirit"),
            Map.entry("kutsal_isik", "holy_light"), Map.entry("toplayici", "gatherer"),
            Map.entry("damar", "vein_hunter"), Map.entry("eritici", "molten_pickaxe"),
            Map.entry("hiz_tutkusu", "need_for_speed"), Map.entry("olta", "lucky_rod"),
            Map.entry("oduncu", "lumberjack"), Map.entry("orman", "forest_keeper"), Map.entry("bereket", "bounty"),
            Map.entry("bahcivan", "green_thumb"));

    /** Old tree id (file name) → current tree id. */
    public static final Map<String, String> TREES = Map.of("ana", "main", "yetenekler", "abilities", "toplayici", "gatherer");

    /** Old chest id → current chest id. */
    public static final Map<String, String> CHESTS = Map.of("demir", "iron", "elmas", "diamond", "netherit", "netherite");

    private static final Map<String, String> NODES_BACK = reverse(NODES);
    private static final Map<String, String> TREES_BACK = reverse(TREES);
    private static final Map<String, String> CHESTS_BACK = reverse(CHESTS);

    private LegacyIds() {
    }

    private static Map<String, String> reverse(Map<String, String> map) {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            out.put(e.getValue(), e.getKey());
        }
        return Map.copyOf(out);
    }

    /** The other name of an upgrade id (old → current, or current → old for a kept old file), or null. */
    public static String node(String id) {
        return other(NODES, NODES_BACK, id);
    }

    public static String tree(String id) {
        return other(TREES, TREES_BACK, id);
    }

    public static String chest(String id) {
        return other(CHESTS, CHESTS_BACK, id);
    }

    private static String other(Map<String, String> forward, Map<String, String> back, String id) {
        if (id == null) {
            return null;
        }
        String key = id.toLowerCase(java.util.Locale.ROOT);
        String v = forward.get(key);
        return v != null ? v : back.get(key);
    }

    /**
     * Player data whose upgrade and chest ids are moved to the names that exist now. An id is only
     * moved when it does not exist itself, its other name does and the player has nothing under
     * that name yet. Returns {@code s} itself when nothing changes.
     */
    public static PlayerData.Snapshot upgrade(PlayerData.Snapshot s, Predicate<String> nodeExists, Predicate<String> chestExists) {
        Map<String, Integer> levels = moveInts(s.levels(), nodeExists, LegacyIds::node);
        Map<String, Double> spent = moveDoubles(s.spent(), nodeExists);
        Map<String, Integer> chests = moveInts(s.chests(), chestExists, LegacyIds::chest);
        if (levels == null && spent == null && chests == null) {
            return s;
        }
        return new PlayerData.Snapshot(s.uuid(), s.name(), levels != null ? levels : s.levels(), spent != null ? spent : s.spent(),
                s.points(), s.prestige(), s.lastRespec(), s.lastDaily(), s.menuMode(), chests != null ? chests : s.chests());
    }

    private static Map<String, Integer> moveInts(Map<String, Integer> in, Predicate<String> exists,
                                                 java.util.function.UnaryOperator<String> other) {
        Map<String, Integer> out = null;
        for (Map.Entry<String, Integer> e : in.entrySet()) {
            String to = target(e.getKey(), exists, other, in);
            if (to != null) {
                if (out == null) {
                    out = new LinkedHashMap<>(in);
                }
                out.remove(e.getKey());
                out.put(to, e.getValue());
            }
        }
        return out;
    }

    private static Map<String, Double> moveDoubles(Map<String, Double> in, Predicate<String> exists) {
        Map<String, Double> out = null;
        for (Map.Entry<String, Double> e : in.entrySet()) {
            String to = target(e.getKey(), exists, LegacyIds::node, in);
            if (to != null) {
                if (out == null) {
                    out = new LinkedHashMap<>(in);
                }
                out.remove(e.getKey());
                out.put(to, e.getValue());
            }
        }
        return out;
    }

    private static String target(String id, Predicate<String> exists, java.util.function.UnaryOperator<String> other, Map<String, ?> have) {
        if (exists.test(id)) {
            return null;
        }
        String to = other.apply(id);
        return to != null && exists.test(to) && !have.containsKey(to) ? to : null;
    }
}
