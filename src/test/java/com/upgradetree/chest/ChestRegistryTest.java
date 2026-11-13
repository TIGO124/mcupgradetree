package com.upgradetree.chest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestRegistryTest {

    private static final String FILE = """
            spin-seconds: 5
            rarities:
              common: { name: "Common", color: "#4B69FF", weight: 80 }
              rare: { name: "Rare", color: "#8847FF", weight: 16 }
              legendary: { name: "Legendary", color: "#EB4B4B", weight: 4, announce: true }
            chests:
              iron:
                name: "Iron"
                price: 5
                rewards:
                  - { item: IRON_INGOT, amount: 8, rarity: common }
                  - { item: BREAD, amount: 16, rarity: common, weight: 3 }
                  - { levels: 10, rarity: rare }
                  - { item: DIAMOND, amount: 2, rarity: legendary }
              pair:
                name: { xx: "Other", en: "Pair" }
                weights: { legendary: 0 }
                rewards:
                  - { item: COAL, rarity: common }
                  - { item: DIAMOND, rarity: legendary }
              broken:
                rewards:
                  - { item: NOT_A_REAL_ITEM, rarity: common }
            """;

    private static ChestRegistry load() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(FILE);
        return ChestRegistry.parse(y, problem -> { }, m -> true, m -> true);
    }

    @Test
    void parsesChestsRaritiesAndSettings() throws Exception {
        ChestRegistry r = load();
        assertEquals(100, r.spinTicks);
        assertEquals(List.of("iron", "pair"), r.chests().stream().map(c -> c.id).toList());
        assertNotNull(r.chest("IRON"));
        assertNull(r.chest("broken"));
        assertFalse(r.issues().isEmpty(), "the unknown item and the empty chest are reported");
        assertEquals("Pair", r.chest("pair").name(), "a per-language name of an older file shows its English text");
        ChestType iron = r.chest("iron");
        assertEquals(5, iron.price);
        assertEquals(ChestReward.Type.LEVELS, iron.rewards.get(2).type);
        assertEquals(10, iron.rewards.get(2).levels);
        assertEquals(8, iron.rewards.get(0).amount);
    }

    @Test
    void oddsWorkLikeCs2Cases() throws Exception {
        ChestType iron = load().chest("iron");
        double total = 0;
        for (double c : iron.chances()) {
            total += c;
        }
        assertEquals(1.0, total, 1e-9);
        // rarity first (80/16/4), then the rewards of that rarity by weight (1:3)
        assertEquals(0.80 * 0.25, iron.chance(iron.rewards.get(0)), 1e-9);
        assertEquals(0.80 * 0.75, iron.chance(iron.rewards.get(1)), 1e-9);
        assertEquals(0.16, iron.chance(iron.rewards.get(2)), 1e-9);
        assertEquals(0.04, iron.chance(iron.rewards.get(3)), 1e-9);
        assertEquals(List.of("common", "rare", "legendary"), iron.rarities().stream().map(Rarity::id).toList());
    }

    @Test
    void aZeroWeightRarityNeverDrops() throws Exception {
        ChestType pair = load().chest("pair");
        assertEquals(1.0, pair.chance(pair.rewards.get(0)), 1e-9);
        assertEquals(0.0, pair.chance(pair.rewards.get(1)), 1e-9);
        assertEquals(1, pair.sortedRewards().size());
        assertEquals("COAL", pair.pick(0.999).material.name());
    }

    @Test
    void pickCoversTheWholeRange() throws Exception {
        ChestType iron = load().chest("iron");
        assertEquals(0, iron.pick(0.0).index);
        assertEquals(0, iron.pick(0.199).index);
        assertEquals(1, iron.pick(0.2).index);
        assertEquals(2, iron.pick(0.85).index);
        assertEquals(3, iron.pick(0.97).index);
        assertEquals(3, iron.pick(0.9999999).index);
    }

    @Test
    void rollsFollowTheOdds() throws Exception {
        ChestType iron = load().chest("iron");
        SplittableRandom rnd = new SplittableRandom(42);
        int[] hits = new int[iron.rewards.size()];
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            hits[iron.roll(rnd).index]++;
        }
        for (ChestReward r : iron.rewards) {
            assertEquals(iron.chance(r), hits[r.index] / (double) n, 0.005, "reward " + r.index);
        }
    }

    @Test
    void bundledChestsFileIsValid() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        try (var in = getClass().getResourceAsStream("/chests.yml")) {
            assertNotNull(in);
            y.loadFromString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        ChestRegistry r = ChestRegistry.parse(y, problem -> { }, m -> true, m -> true);
        assertEquals(List.of(), r.issues());
        assertEquals(List.of("iron", "diamond", "netherite"), r.chests().stream().map(c -> c.id).toList());
        for (ChestType c : r.chests()) {
            double total = 0;
            for (double v : c.chances()) {
                total += v;
            }
            assertEquals(1.0, total, 1e-9, c.id);
            assertEquals(5, c.rarities().size(), c.id + " has every rarity");
        }
        assertTrue(r.active());
        ChestReward sword = r.chest("diamond").rewards.stream().filter(x -> x.material.name().equals("DIAMOND_SWORD"))
                .findFirst().orElseThrow();
        assertEquals(java.util.Map.of("sharpness", 5, "unbreaking", 3), sword.enchantments);
        assertEquals("Sharp Diamond Sword", sword.customName);
        assertEquals("diamond", r.chest("elmas").id, "the id of an older version still finds the chest");
        assertTrue(r.has("diamond"));
        assertFalse(r.has("elmas"));
    }

    @Test
    void sortedRewardsGoFromCommonToRare() throws Exception {
        ChestType iron = load().chest("iron");
        List<ChestReward> sorted = iron.sortedRewards();
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(sorted.get(i - 1).rarity.rank() <= sorted.get(i).rarity.rank());
        }
        assertEquals("<color:#EB4B4B>", sorted.get(sorted.size() - 1).rarity.tag());
    }

    /** A prize larger than the item's stack size is given as several legal stacks. */
    @Test
    void prizesAreSplitIntoLegalStacks() {
        assertEquals(List.of(16, 16, 8), ChestManager.stacks(40, 16));
        assertEquals(List.of(1, 1), ChestManager.stacks(2, 1));
        assertEquals(List.of(64), ChestManager.stacks(64, 64));
        assertEquals(List.of(8), ChestManager.stacks(8, 64));
        assertEquals(List.of(1, 1, 1), ChestManager.stacks(3, 0));
        assertEquals(List.of(), ChestManager.stacks(0, 64));
    }
}
