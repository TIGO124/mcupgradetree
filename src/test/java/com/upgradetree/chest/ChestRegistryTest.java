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
              yaygin: { name: "Yaygın", color: "#4B69FF", weight: 80 }
              nadir: { name: "Nadir", color: "#8847FF", weight: 16 }
              efsanevi: { name: "Efsanevi", color: "#EB4B4B", weight: 4, announce: true }
            chests:
              demir:
                name: "Demir"
                price: 5
                rewards:
                  - { item: IRON_INGOT, amount: 8, rarity: yaygin }
                  - { item: BREAD, amount: 16, rarity: yaygin, weight: 3 }
                  - { levels: 10, rarity: nadir }
                  - { item: DIAMOND, amount: 2, rarity: efsanevi }
              ikili:
                name: "İkili"
                weights: { efsanevi: 0 }
                rewards:
                  - { item: COAL, rarity: yaygin }
                  - { item: DIAMOND, rarity: efsanevi }
              bozuk:
                rewards:
                  - { item: NOT_A_REAL_ITEM, rarity: yaygin }
            """;

    private static ChestRegistry load() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(FILE);
        return ChestRegistry.parse(y, (tr, en) -> { }, m -> true, m -> true);
    }

    @Test
    void parsesChestsRaritiesAndSettings() throws Exception {
        ChestRegistry r = load();
        assertEquals(100, r.spinTicks);
        assertEquals(List.of("demir", "ikili"), r.chests().stream().map(c -> c.id).toList());
        assertNotNull(r.chest("DEMIR"));
        assertNull(r.chest("bozuk"));
        assertFalse(r.issues().isEmpty(), "the unknown item and the empty chest are reported");
        ChestType demir = r.chest("demir");
        assertEquals(5, demir.price);
        assertEquals(ChestReward.Type.LEVELS, demir.rewards.get(2).type);
        assertEquals(10, demir.rewards.get(2).levels);
        assertEquals(8, demir.rewards.get(0).amount);
    }

    @Test
    void oddsWorkLikeCs2Cases() throws Exception {
        ChestType demir = load().chest("demir");
        double total = 0;
        for (double c : demir.chances()) {
            total += c;
        }
        assertEquals(1.0, total, 1e-9);
        // rarity first (80/16/4), then the rewards of that rarity by weight (1:3)
        assertEquals(0.80 * 0.25, demir.chance(demir.rewards.get(0)), 1e-9);
        assertEquals(0.80 * 0.75, demir.chance(demir.rewards.get(1)), 1e-9);
        assertEquals(0.16, demir.chance(demir.rewards.get(2)), 1e-9);
        assertEquals(0.04, demir.chance(demir.rewards.get(3)), 1e-9);
        assertEquals(List.of("yaygin", "nadir", "efsanevi"), demir.rarities().stream().map(Rarity::id).toList());
    }

    @Test
    void aZeroWeightRarityNeverDrops() throws Exception {
        ChestType ikili = load().chest("ikili");
        assertEquals(1.0, ikili.chance(ikili.rewards.get(0)), 1e-9);
        assertEquals(0.0, ikili.chance(ikili.rewards.get(1)), 1e-9);
        assertEquals(1, ikili.sortedRewards().size());
        assertEquals("COAL", ikili.pick(0.999).material.name());
    }

    @Test
    void pickCoversTheWholeRange() throws Exception {
        ChestType demir = load().chest("demir");
        assertEquals(0, demir.pick(0.0).index);
        assertEquals(0, demir.pick(0.199).index);
        assertEquals(1, demir.pick(0.2).index);
        assertEquals(2, demir.pick(0.85).index);
        assertEquals(3, demir.pick(0.97).index);
        assertEquals(3, demir.pick(0.9999999).index);
    }

    @Test
    void rollsFollowTheOdds() throws Exception {
        ChestType demir = load().chest("demir");
        SplittableRandom rnd = new SplittableRandom(42);
        int[] hits = new int[demir.rewards.size()];
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            hits[demir.roll(rnd).index]++;
        }
        for (ChestReward r : demir.rewards) {
            assertEquals(demir.chance(r), hits[r.index] / (double) n, 0.005, "reward " + r.index);
        }
    }

    @Test
    void bundledChestsFileIsValid() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        try (var in = getClass().getResourceAsStream("/chests.yml")) {
            assertNotNull(in);
            y.loadFromString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        ChestRegistry r = ChestRegistry.parse(y, (tr, en) -> { }, m -> true, m -> true);
        assertEquals(List.of(), r.issues());
        assertEquals(List.of("demir", "elmas", "netherit"), r.chests().stream().map(c -> c.id).toList());
        for (ChestType c : r.chests()) {
            double total = 0;
            for (double v : c.chances()) {
                total += v;
            }
            assertEquals(1.0, total, 1e-9, c.id);
            assertEquals(5, c.rarities().size(), c.id + " has every rarity");
        }
        assertTrue(r.active());
        ChestReward sword = r.chest("elmas").rewards.stream().filter(x -> x.material.name().equals("DIAMOND_SWORD"))
                .findFirst().orElseThrow();
        assertEquals(java.util.Map.of("sharpness", 5, "unbreaking", 3), sword.enchantments);
        assertEquals("<#F472B6>Sharp Diamond Sword", sword.names.get("en", "tr"));
    }

    @Test
    void sortedRewardsGoFromCommonToRare() throws Exception {
        ChestType demir = load().chest("demir");
        List<ChestReward> sorted = demir.sortedRewards();
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(sorted.get(i - 1).rarity.rank() <= sorted.get(i).rarity.rank());
        }
        assertEquals("<color:#EB4B4B>", sorted.get(sorted.size() - 1).rarity.tag());
    }
}
