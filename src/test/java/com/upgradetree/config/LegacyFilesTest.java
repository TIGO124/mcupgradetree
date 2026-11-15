package com.upgradetree.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyFilesTest {

    private static String print(String yaml) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(yaml);
        return LegacyFiles.fingerprint(LegacyFiles.plain(y));
    }

    @Test
    void fingerprintsIgnoreCommentsAndLayoutButNotValues() throws Exception {
        String a = print("# a comment\nnodes:\n  x: { cost: 2, name: \"Hi\" }\n");
        String b = print("nodes:\n  x:\n    cost: 2   # other comment\n    name: Hi\n");
        assertEquals(a, b);
        assertNotEquals(a, print("nodes:\n  x: { cost: 3, name: \"Hi\" }\n"));
        assertNotEquals(a, print("nodes:\n  x: { cost: \"2\", name: \"Hi\" }\n"), "a number and a text differ");
        assertEquals(20, a.length());
    }

    /** A current file is never mistaken for an old default (it would be replaced at every start). */
    @Test
    void currentFilesAreNotOldDefaults() throws Exception {
        for (Map.Entry<String, Set<String>> e : LegacyFiles.OLD_DEFAULTS.entrySet()) {
            String current = LegacyFiles.fingerprint(LegacyFiles.plain(BundledFilesTest.yaml("/" + e.getKey())));
            assertFalse(e.getValue().contains(current), e.getKey());
            assertFalse(e.getValue().isEmpty(), e.getKey() + " knows its older versions");
        }
        for (String current : LegacyFiles.RENAMED.values()) {
            assertTrue(LegacyFiles.OLD_DEFAULTS.containsKey(current), current);
        }
    }

    @Test
    void anOldConfigKeepsItsValuesAndGetsTheNewLayout() throws Exception {
        YamlConfiguration old = new YamlConfiguration();
        old.loadFromString("""
                # an old comment
                language: tr
                currency:
                  type: POINTS
                  name: "Seviye"
                  symbol: "✦"
                points:
                  per-mob-kill: 1.5
                  mob-points:
                    ZOMBIE: 4
                menu:
                  distance: 2.2
                  stars: false
                  hide-held-item: false
                theme:
                  gold: "#FF0000"
                access:
                  item:
                    name: { xx: "Other", en: "<b>Tree" }
                custom-key: 7
                messages:
                  prefix: "x"
                """);
        YamlConfiguration fresh = BundledFilesTest.yaml("/config.yml");
        LegacyFiles.merge(old, fresh);

        assertFalse(fresh.contains("language"));
        assertFalse(fresh.contains("messages"));
        assertEquals(2, fresh.getInt("config-version"), "the version of the new file stays");
        assertEquals("POINTS", fresh.getString("currency.type"));
        assertEquals("", fresh.getString("currency.name"), "an old built-in name becomes the messages.yml name");
        assertEquals("", fresh.getString("currency.symbol"));
        assertEquals(1.5, fresh.getDouble("points.per-mob-kill"));
        assertEquals(Set.of("ZOMBIE"), fresh.getConfigurationSection("points.mob-points").getKeys(false),
                "a map the owner wrote is taken as a whole");
        assertEquals(600, fresh.getInt("points.player-kill-cooldown"), "keys the old file did not have keep their default");
        assertEquals(2.2, fresh.getDouble("menu.distance"));
        assertEquals("NONE", fresh.getString("menu.background"));
        assertFalse(fresh.contains("menu.stars"));
        assertFalse(fresh.getBoolean("menu.hide-hand"));
        assertEquals("#FF0000", fresh.getString("theme.gold"));
        assertEquals("<b>Tree", fresh.getString("access.item.name"));
        assertEquals(7, fresh.getInt("custom-key"), "unknown keys are kept");
        assertTrue(fresh.getComments("menu.distance").stream().anyMatch(c -> c.contains("Distance")), "new comments");
        assertTrue(fresh.getComments("theme").size() > 0, "a replaced section keeps its comments");
        assertEquals(List.of(), fresh.getComments("language"));
    }

    /** After an update the config.yml of the version before gets the new settings, its values stay. */
    @Test
    void aConfigOfAnOlderVersionGetsTheNewSettings() throws Exception {
        YamlConfiguration bundled = BundledFilesTest.yaml("/config.yml");
        YamlConfiguration old = new YamlConfiguration();
        old.loadFromString("""
                config-version: 1
                currency:
                  type: VAULT
                menu:
                  style: WOOD
                  distance: 1.9
                theme:
                  card: "#112233"
                restrictions:
                  disabled-worlds: [arena]
                """);
        assertTrue(LegacyFiles.outdated(old, bundled));
        assertTrue(LegacyFiles.outdated(new YamlConfiguration(), bundled), "2.2 and older have no version");
        assertFalse(LegacyFiles.outdated(bundled, bundled));
        YamlConfiguration newer = new YamlConfiguration();
        newer.set("config-version", bundled.getInt("config-version") + 1);
        assertFalse(LegacyFiles.outdated(newer, bundled), "a file of a newer version is left alone");

        LegacyFiles.merge(old, bundled);
        assertEquals(2, bundled.getInt("config-version"));
        assertEquals("VAULT", bundled.getString("currency.type"));
        assertEquals("WOOD", bundled.getString("menu.style"));
        assertEquals(1.9, bundled.getDouble("menu.distance"));
        assertEquals("#112233", bundled.getString("theme.card"));
        assertEquals(List.of("arena"), bundled.getStringList("restrictions.disabled-worlds"));
        assertEquals("TIGO124/mcupgradetree", bundled.getString("update.repository"), "the new section is there");
        assertTrue(bundled.getComments("update").size() > 0);
    }
}
