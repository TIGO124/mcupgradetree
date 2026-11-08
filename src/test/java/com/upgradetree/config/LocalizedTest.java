package com.upgradetree.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizedTest {

    private static final String[] TREES = {"ana", "yetenekler", "toplayici"};
    /** Words in a text (tags, placeholders and symbols removed). */
    private static final Pattern WORD = Pattern.compile("\\p{L}{2,}");

    private static YamlConfiguration yaml(String resource) throws Exception {
        try (InputStream in = LocalizedTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return y;
        }
    }

    @Test
    void readerLanguageThenServerLanguageThenFirst() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString("a: { tr: Kılıç, en: Sword }\nb: Titan\nc: { de: Schwert }\nd: [x, y]\ne: { tr: [bir], en: [one, two] }\n");
        Localized<String> a = Localized.text(y.get("a"), "?");
        assertEquals("Sword", a.get("en", "tr"));
        assertEquals("Kılıç", a.get("tr", "tr"));
        assertEquals("Kılıç", a.get("de", "tr"), "missing language → server language");
        assertEquals("Sword", a.get("de", "en"));
        assertEquals("Kılıç", a.get("de", "fr"), "nothing matches → first written");
        assertEquals("Kılıç", a.base());
        assertEquals("Titan", Localized.text(y.get("b"), "?").get("en", "tr"));
        assertEquals("Schwert", Localized.text(y.get("c"), "?").get("tr", "tr"));
        assertEquals("?", Localized.text(y.get("missing"), "?").get("en", "tr"));
        assertNull(Localized.text(y.get("missing"), null).get("en", "tr"));
        assertEquals(List.of("x", "y"), Localized.lines(y.get("d")).get("en", "tr"));
        assertEquals(List.of("one", "two"), Localized.lines(y.get("e")).get("en", "tr"));
        assertEquals(List.of("bir"), Localized.lines(y.get("e")).get("tr", "en"));
        assertEquals(List.of(), Localized.lines(null).get("en", "tr"));
        assertEquals(Set.of("tr", "en"), a.languages());
    }

    @Test
    void currencyAndItemNamesFollowTheLanguageUnlessCustomised() {
        Set<String> defaults = Set.of("seviye", "levels");
        assertTrue(Settings.defaultName(Localized.of("Seviye"), defaults).isEmpty());
        assertTrue(Settings.defaultName(Localized.of(" "), defaults).isEmpty());
        assertTrue(Settings.defaultName(Localized.empty(), defaults).isEmpty());
        assertEquals("Elmas", Settings.defaultName(Localized.of("Elmas"), defaults).base());
        Localized<String> map = Localized.of(Map.of("tr", "Seviye", "en", "Levels"));
        assertEquals("Levels", Settings.defaultName(map, defaults).get("en", "tr"), "an explicit map is always kept");
    }

    /** Every player-visible text of the bundled trees has an English version (or is language neutral). */
    @Test
    void bundledTreesAreWrittenInEveryLanguage() throws Exception {
        for (String t : TREES) {
            YamlConfiguration y = yaml("/trees/" + t + ".yml");
            assertTranslated(t + " title", y.get("title"));
            assertTranslated(t + " tab", y.get("tab.name"));
            ConfigurationSection nodes = y.getConfigurationSection("nodes");
            assertNotNull(nodes, t);
            for (String id : nodes.getKeys(false)) {
                ConfigurationSection n = nodes.getConfigurationSection(id);
                assertTranslated(t + "." + id + " name", n.get("name"));
                assertTranslated(t + "." + id + " description", n.get("description"));
                assertTranslated(t + "." + id + " bonus", n.get("bonus"));
            }
        }
    }

    @Test
    void bundledChestsAreWrittenInEveryLanguage() throws Exception {
        YamlConfiguration y = yaml("/chests.yml");
        ConfigurationSection rarities = y.getConfigurationSection("rarities");
        for (String id : rarities.getKeys(false)) {
            assertTranslated("rarity " + id, rarities.get(id + ".name"));
        }
        ConfigurationSection chests = y.getConfigurationSection("chests");
        for (String id : chests.getKeys(false)) {
            assertTranslated("chest " + id + " name", chests.get(id + ".name"));
            assertTranslated("chest " + id + " description", chests.get(id + ".description"));
            for (Map<?, ?> reward : chests.getMapList(id + ".rewards")) {
                assertTranslated("chest " + id + " reward " + reward.get("item"), reward.get("name"));
            }
        }
    }

    /** A text that has words must be a {tr, en} map; neutral texts ("Titan", "+{value} ❤") may stay plain. */
    private static void assertTranslated(String what, Object raw) {
        if (raw == null) {
            return;
        }
        Localized<?> l = raw instanceof List<?> || (raw instanceof ConfigurationSection s && s.get("tr") instanceof List<?>)
                ? Localized.lines(raw) : Localized.text(raw, null);
        if (!l.languages().isEmpty()) {
            assertTrue(l.languages().containsAll(Set.of("tr", "en")), what + " has tr and en");
            return;
        }
        String text = String.valueOf(l.base()).replaceAll("<[^>]*>", "").replace("{value}", "");
        boolean neutral = !WORD.matcher(text).find() || Set.of("Titan").contains(text.trim());
        assertTrue(neutral, what + " is only written once: " + l.base());
    }

    /** The untouched Turkish defaults of the previous version are recognised and upgraded. */
    @Test
    void oldTurkishDefaultsAreRecognised() throws Exception {
        String[] files = {"/trees/ana.yml", "/trees/yetenekler.yml", "/trees/toplayici.yml", "/chests.yml"};
        for (String f : files) {
            Object bundled = DefaultFiles.plain(yaml(f));
            Object old = DefaultFiles.plain(yaml("/old-defaults" + f));
            assertTrue(DefaultFiles.hasLanguages(bundled), f);
            assertFalse(DefaultFiles.hasLanguages(old), f);
            assertFalse(DefaultFiles.hasLanguages(DefaultFiles.reduce(bundled)), f);
            assertEquals(old, DefaultFiles.reduce(bundled), f + " is the old default with English added");
        }
    }

    @Test
    void anEditedOldFileIsNotReplaced() throws Exception {
        YamlConfiguration y = yaml("/old-defaults/trees/ana.yml");
        y.set("nodes.keskinlik.cost", 99);
        assertFalse(DefaultFiles.reduce(DefaultFiles.plain(yaml("/trees/ana.yml"))).equals(DefaultFiles.plain(y)));
    }
}
