package com.upgradetree;

import com.upgradetree.data.DataCodec;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.ProgressService;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogicTest {

    @Test
    void costFormula() {
        // cost + increase * level
        assertEquals(2, UpgradeNode.formula(2, 1, 1, List.of(), 0));
        assertEquals(5, UpgradeNode.formula(2, 1, 1, List.of(), 3));
        // cost * multiplier ^ level
        assertEquals(40, UpgradeNode.formula(10, 0, 2, List.of(), 2));
        // explicit list, last value repeats, never negative
        assertEquals(3, UpgradeNode.formula(0, 0, 1, List.of(1.0, 3.0), 1));
        assertEquals(3, UpgradeNode.formula(0, 0, 1, List.of(1.0, 3.0), 9));
        assertEquals(0, UpgradeNode.formula(-5, 0, 1, List.of(), 0));
        // rounded to 2 decimals
        assertEquals(1.33, UpgradeNode.formula(1.3333, 0, 1, List.of(), 0));
    }

    @Test
    void refundIsAShareOfTheSpending() {
        assertEquals(8, ProgressService.refundOf(10, 0.8));
        assertEquals(0, ProgressService.refundOf(10, 0));
        assertEquals(10, ProgressService.refundOf(10, 1.5));
        assertEquals(2.66, ProgressService.refundOf(3.33, 0.8));
        assertEquals(0, ProgressService.refundOf(-4, 0.8));
    }

    @Test
    void playerDataSurvivesASaveRoundTrip() throws Exception {
        UUID id = UUID.randomUUID();
        PlayerData d = new PlayerData(id, "Tigo");
        d.setLevel("keskinlik", 3);
        d.addSpent("keskinlik", 9);
        d.points(12.5);
        d.prestige(2);
        d.lastDaily("2026-11-06");
        d.menuMode("CHEST");
        PlayerData.Snapshot back = DataCodec.decode(id, "?", DataCodec.encode(d.snapshot()));
        assertEquals("Tigo", back.name());
        assertEquals(Map.of("keskinlik", 3), back.levels());
        assertEquals(9.0, back.spent().get("keskinlik"));
        assertEquals(12.5, back.points());
        assertEquals(2, back.prestige());
        assertEquals("2026-11-06", back.lastDaily());
        assertEquals("CHEST", back.menuMode());
        assertEquals(3, back.totalLevels());
    }

    @Test
    void resettingALevelForgetsItsSpending() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "x");
        d.setLevel("a", 2);
        d.addSpent("a", 5);
        d.setLevel("a", 0);
        assertEquals(0, d.level("a"));
        assertEquals(0, d.spent("a"));
        assertTrue(d.isDirty());
    }

    @Test
    void textHelpers() {
        assertEquals("5", Text.num(5.0));
        assertEquals("2.5", Text.num(2.5));
        assertEquals("0.33", Text.num(1.0 / 3));
        assertEquals("1:05", Text.duration(65_000));
        assertEquals("1:00:00", Text.duration(3_600_000));
        assertEquals("<reset><red>Hi", Text.legacyToMini("&cHi"));
        assertEquals("<reset><#FF8800>x", Text.legacyToMini("&#FF8800x"));
        List<String> lines = Text.wrap("<gray>bir iki üç dört beş altı yedi sekiz", 60);
        assertTrue(lines.size() > 1, lines.toString());
        for (String line : lines) {
            assertTrue(Text.width(Text.plain(line), false) <= 64, line);
        }
    }

    @Test
    void paletteMix() {
        assertEquals(0xFF000000, Palette.mix(0xFF000000, 0xFFFFFFFF, 0));
        assertEquals(0xFFFFFFFF, Palette.mix(0xFF000000, 0xFFFFFFFF, 1));
        assertEquals(0xFF808080, Palette.mix(0xFF000000, 0xFFFFFFFF, 0.5));
        assertEquals(0x80123456, Palette.alpha(0xFF123456, 0x80));
    }
}
