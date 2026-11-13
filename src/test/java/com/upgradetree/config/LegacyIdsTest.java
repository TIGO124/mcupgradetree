package com.upgradetree.config;

import com.upgradetree.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LegacyIdsTest {

    private static final Set<String> CURRENT_NODES = Set.of("origin", "sharp_sword", "rage", "custom");
    private static final Set<String> CURRENT_CHESTS = Set.of("iron", "diamond");

    private static PlayerData.Snapshot data(Map<String, Integer> levels, Map<String, Double> spent, Map<String, Integer> chests) {
        return new PlayerData.Snapshot(UUID.randomUUID(), "Steve", levels, spent, 3, 1, 0, "", "", chests);
    }

    private static PlayerData.Snapshot upgrade(PlayerData.Snapshot s) {
        return LegacyIds.upgrade(s, CURRENT_NODES::contains, CURRENT_CHESTS::contains);
    }

    @Test
    void namesWorkBothWays() {
        assertEquals("sharp_sword", LegacyIds.node("keskinlik"));
        assertEquals("keskinlik", LegacyIds.node("SHARP_SWORD"));
        assertEquals("main", LegacyIds.tree("ana"));
        assertEquals("netherite", LegacyIds.chest("netherit"));
        assertNull(LegacyIds.node("custom"));
        assertNull(LegacyIds.node(null));
    }

    @Test
    void oldIdsMoveToTheCurrentOnes() {
        PlayerData.Snapshot up = upgrade(data(Map.of("baslangic", 1, "keskinlik", 5, "custom", 2),
                Map.of("keskinlik", 12.5), Map.of("demir", 2, "elmas", 1)));
        assertEquals(Map.of("origin", 1, "sharp_sword", 5, "custom", 2), up.levels());
        assertEquals(Map.of("sharp_sword", 12.5), up.spent());
        assertEquals(Map.of("iron", 2, "diamond", 1), up.chests());
        assertEquals(3, up.points());
        assertEquals(1, up.prestige());
    }

    @Test
    void nothingMovesWhenTheOldIdStillExistsOrTheNewOneIsMissing() {
        // an edited older tree file was kept: its ids exist, so the data stays as it is
        PlayerData.Snapshot kept = data(Map.of("keskinlik", 5), Map.of(), Map.of());
        assertSame(kept, LegacyIds.upgrade(kept, id -> id.equals("keskinlik"), id -> false));
        // no current upgrade to move to (the owner removed it): keep the old entry
        PlayerData.Snapshot gone = data(Map.of("zirh", 3), Map.of(), Map.of("netherit", 1));
        assertSame(gone, upgrade(gone));
        PlayerData.Snapshot current = data(Map.of("origin", 1), Map.of(), Map.of("iron", 1));
        assertSame(current, upgrade(current));
    }

    @Test
    void anExistingNewEntryIsNeverOverwritten() {
        PlayerData.Snapshot up = upgrade(data(Map.of("ofke", 1, "rage", 2), Map.of(), Map.of()));
        assertEquals(Map.of("ofke", 1, "rage", 2), up.levels());
    }

    /** Every old id points to an upgrade or chest of the bundled files. */
    @Test
    void everyNewIdIsBundled() throws Exception {
        java.util.Set<String> nodes = new java.util.HashSet<>();
        for (String tree : LegacyIds.TREES.values()) {
            nodes.addAll(BundledFilesTest.yaml("/trees/" + tree + ".yml").getConfigurationSection("nodes").getKeys(false));
        }
        for (String id : LegacyIds.NODES.values()) {
            org.junit.jupiter.api.Assertions.assertTrue(nodes.contains(id), id);
        }
        Set<String> chests = BundledFilesTest.yaml("/chests.yml").getConfigurationSection("chests").getKeys(false);
        assertEquals(Set.copyOf(LegacyIds.CHESTS.values()), chests);
    }
}
