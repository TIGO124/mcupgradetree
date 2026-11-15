package com.upgradetree.lootbag;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootBagRegistryTest {

    private static final String FILE = """
            hostile-multiplier: 2
            items: 2-3
            ignored-spawn-reasons: [SPAWNER, NOT_A_REASON]
            rarities:
              common:
                name: "Common Bag"
                chance: 10
                loot:
                  - { item: BREAD, amount: 2-6 }
                  - { item: COAL, amount: 4 }
                  - { item: ARROW, amount: "8 - 16" }
                  - { item: NOT_A_REAL_ITEM }
                  - { levels: 3-5, weight: 2 }
                  - { item: STONE, weight: 0 }
              rare:
                name: "Rare Bag"
                chance: 5
                items: { min: 1, max: 1 }
                glint: true
                announce: true
                model: minecraft:purple_bundle
                loot:
                  - { item: DIAMOND, amount: 1-2, enchantments: { sharpness: 5 } }
              empty:
                chance: 1
                loot: []
            """;

    private static LootBagRegistry parse(String text) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(text);
        return LootBagRegistry.parse(y, problem -> { }, m -> true, e -> true);
    }

    @Test
    void parsesRaritiesInTheirOrder() throws Exception {
        LootBagRegistry r = parse(FILE);
        assertEquals(List.of("common", "rare"), r.types().stream().map(t -> t.id).toList());
        assertEquals(Set.of(SpawnReason.SPAWNER), r.ignoredSpawnReasons);
        assertEquals(3, r.issues().size(), "the unknown item, the unknown spawn reason and the empty rarity");
        LootBagType common = r.type("COMMON");
        assertEquals(0, common.rank);
        assertEquals(5, common.loot.size());
        assertEquals(2, common.loot.get(0).min);
        assertEquals(6, common.loot.get(0).max);
        assertEquals(4, common.loot.get(1).min);
        assertEquals(4, common.loot.get(1).max);
        assertEquals(16, common.loot.get(2).max);
        assertEquals(LootEntry.Type.LEVELS, common.loot.get(3).type);
        assertEquals(2, common.minItems());
        assertEquals(3, common.maxItems());
        LootBagType rare = r.type("rare");
        assertEquals(1, rare.rank);
        assertTrue(rare.glint && rare.announce);
        assertEquals("minecraft:purple_bundle", rare.model);
        assertEquals(1, rare.minItems());
        assertEquals(1, rare.maxItems(), "a bag cannot hold more different items than it has entries");
        assertEquals(Material.DIAMOND, rare.loot.get(0).material);
        assertEquals(5, rare.loot.get(0).enchantments.get("sharpness"));
    }

    @Test
    void hostileMobsHaveTwiceTheChance() throws Exception {
        LootBagRegistry r = parse(FILE);
        assertEquals(15, r.totalChance(false), 1e-9);
        assertEquals(30, r.totalChance(true), 1e-9);
        assertSame(r.type("common"), r.roll(false, 0.05));
        assertSame(r.type("rare"), r.roll(false, 0.149));
        assertNull(r.roll(false, 0.151));
        assertSame(r.type("common"), r.roll(true, 0.15));
        assertSame(r.type("rare"), r.roll(true, 0.29));
        assertNull(r.roll(true, 0.31));

        SplittableRandom random = new SplittableRandom(7);
        int passive = 0;
        int hostile = 0;
        int rareHostile = 0;
        int runs = 200_000;
        for (int i = 0; i < runs; i++) {
            if (r.roll(false, random.nextDouble()) != null) {
                passive++;
            }
            LootBagType t = r.roll(true, random.nextDouble());
            if (t != null) {
                hostile++;
                if (t.id.equals("rare")) {
                    rareHostile++;
                }
            }
        }
        assertEquals(0.15, passive / (double) runs, 0.005);
        assertEquals(0.30, hostile / (double) runs, 0.005);
        assertEquals(1 / 3.0, rareHostile / (double) hostile, 0.01, "the rarities keep their proportions");
    }

    @Test
    void chancesOverAHundredPercentAlwaysDrop() throws Exception {
        LootBagRegistry r = parse("""
                rarities:
                  a: { chance: 60, loot: [ { item: COAL } ] }
                  b: { chance: 40, loot: [ { item: DIAMOND } ] }
                """);
        assertSame(r.type("b"), r.roll(false, 0.999));
        assertSame(r.type("b"), r.roll(true, 0.999), "200% for hostile mobs: still one bag");
        assertSame(r.type("a"), r.roll(true, 0.25));
    }

    @Test
    void aBagHoldsDifferentItemsWithinTheirAmounts() throws Exception {
        LootBagType common = parse(FILE).type("common");
        SplittableRandom random = new SplittableRandom(3);
        Set<Integer> sizes = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            List<LootBagType.Roll> rolls = common.open(random);
            sizes.add(rolls.size());
            Set<Integer> seen = new HashSet<>();
            int previous = -1;
            for (LootBagType.Roll roll : rolls) {
                LootEntry e = roll.entry();
                assertTrue(seen.add(e.index), "the same entry twice");
                assertTrue(e.weight > 0, "an entry with weight 0 was picked");
                assertTrue(roll.amount() >= e.min && roll.amount() <= e.max, roll.toString());
                assertTrue(e.index > previous, "listed in the order of the file");
                previous = e.index;
            }
        }
        assertEquals(Set.of(2, 3), sizes);
    }

    @Test
    void amountsAndRanges() {
        assertArrayEquals(new int[]{2, 6}, LootBagRegistry.range("2-6"));
        assertArrayEquals(new int[]{3, 3}, LootBagRegistry.range(3));
        assertArrayEquals(new int[]{4, 4}, LootBagRegistry.range("4"));
        assertArrayEquals(new int[]{-3, -3}, LootBagRegistry.range("-3"));
        assertNull(LootBagRegistry.range("6-2"));
        assertNull(LootBagRegistry.range("lots"));
        assertNull(LootBagRegistry.range(null));
    }

    /** The file written on a new server: seven rarities, 5-10 items each, rarer bags drop less often. */
    @Test
    void bundledFile() throws Exception {
        String text;
        try (InputStream in = LootBagRegistryTest.class.getResourceAsStream("/" + LootBagRegistry.FILE)) {
            assertNotNull(in);
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        LootBagRegistry r = parse(text);
        assertEquals(List.of(), r.issues());
        assertTrue(r.active());
        assertEquals(List.of("common", "uncommon", "rare", "epic", "legendary", "mythic", "secret"),
                r.types().stream().map(t -> t.id).toList());
        assertEquals(2.0, r.hostileMultiplier, 1e-9);
        assertEquals(2 * r.totalChance(false), r.totalChance(true), 1e-9);
        double previous = Double.MAX_VALUE;
        for (LootBagType t : r.types()) {
            assertEquals(5, t.minItems(), t.id);
            assertEquals(10, t.maxItems(), t.id);
            assertTrue(t.chance < previous, t.id + " must be rarer than the rarity before it");
            previous = t.chance;
        }
        assertFalse(r.type("epic").announce);
        assertTrue(r.type("legendary").announce && r.type("mythic").announce && r.type("secret").announce);
        assertTrue(r.ignoredSpawnReasons.contains(SpawnReason.SPAWNER));
    }
}
