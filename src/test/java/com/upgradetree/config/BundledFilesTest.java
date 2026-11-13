package com.upgradetree.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The files the plugin writes on a new server: English only, one text per key. */
class BundledFilesTest {

    static final String[] FILES = {"/config.yml", "/messages.yml", "/chests.yml", "/trees/main.yml",
            "/trees/abilities.yml", "/trees/gatherer.yml"};
    /** Letters of other alphabets that must not show up in the bundled English texts. */
    private static final Pattern FOREIGN = Pattern.compile("[\u00e7\u011f\u0131\u00f6\u015f\u00fc\u00c7\u011e\u0130\u00d6\u015e\u00dc\u00e4\u00df\u00e9\u00e8\u00ea\u00e0\u00e1\u00ed\u00f3\u00fa\u00f1]");

    static String text(String resource) throws Exception {
        try (InputStream in = BundledFilesTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static YamlConfiguration yaml(String resource) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(text(resource));
        return y;
    }

    @Test
    void everyFileIsEnglish() throws Exception {
        for (String f : FILES) {
            String[] lines = text(f).split("\n");
            for (int i = 0; i < lines.length; i++) {
                assertFalse(FOREIGN.matcher(lines[i]).find(), f + ":" + (i + 1) + " " + lines[i]);
            }
        }
    }

    /** Texts are plain strings or lists, never maps of languages. */
    @Test
    void noTextIsWrittenPerLanguage() throws Exception {
        for (String f : FILES) {
            YamlConfiguration y = yaml(f);
            for (String key : y.getKeys(true)) {
                assertFalse(key.endsWith(".en") || key.endsWith(".tr") || key.equals("en") || key.equals("tr"), f + " " + key);
            }
            for (String key : y.getKeys(true)) {
                if (y.get(key) instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            assertFalse(m.get("name") instanceof Map<?, ?>, f + " " + key + " " + m);
                        }
                    }
                }
            }
        }
    }

    @Test
    void messagesHaveNoLanguageCommands() throws Exception {
        YamlConfiguration m = yaml("/messages.yml");
        assertEquals(Lang.VERSION, m.getInt("messages-version"));
        for (String key : m.getKeys(false)) {
            assertFalse(key.contains("language"), key);
            for (String line : m.getStringList(key)) {
                assertFalse(line.contains("language"), key + ": " + line);
            }
        }
        assertTrue(m.contains("quick-open"));
    }

    @Test
    void configHasItsVersionAndNoLanguage() throws Exception {
        YamlConfiguration c = yaml("/config.yml");
        assertTrue(c.contains("config-version"));
        assertFalse(c.contains("language"));
        assertFalse(c.contains("messages"));
    }

    @Test
    void treesHaveTitlesTabsAndNames() throws Exception {
        for (String tree : new String[]{"main", "abilities", "gatherer"}) {
            YamlConfiguration y = yaml("/trees/" + tree + ".yml");
            assertTrue(y.isString("title"), tree);
            assertTrue(y.isString("tab.name"), tree);
            ConfigurationSection nodes = y.getConfigurationSection("nodes");
            assertNotNull(nodes, tree);
            for (String id : nodes.getKeys(false)) {
                assertTrue(id.matches("[a-z0-9_]+"), tree + ": " + id);
                assertTrue(nodes.isString(id + ".name"), tree + "." + id);
                assertTrue(nodes.isList(id + ".description"), tree + "." + id);
            }
        }
    }
}
