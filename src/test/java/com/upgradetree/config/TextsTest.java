package com.upgradetree.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextsTest {

    private static YamlConfiguration yaml(String text) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(text);
        return y;
    }

    @Test
    void plainTextsAndLines() throws Exception {
        YamlConfiguration y = yaml("a: Sword\nb: [one, two]\nc: one line\nd: ' '\n");
        assertEquals("Sword", Texts.text(y.get("a"), "?"));
        assertEquals("?", Texts.text(y.get("missing"), "?"));
        assertNull(Texts.text(y.get("missing"), null));
        assertEquals("?", Texts.text(y.get("b"), "?"), "a list is not a single text");
        assertEquals(List.of("one", "two"), Texts.lines(y.get("b")));
        assertEquals(List.of("one line"), Texts.lines(y.get("c")));
        assertEquals(List.of(), Texts.lines(y.get("d")));
        assertEquals(List.of(), Texts.lines(null));
    }

    /** Files of older versions held a text per language: the English one is used. */
    @Test
    void perLanguageMapsGiveTheirEnglishText() throws Exception {
        YamlConfiguration y = yaml("""
                a: { xx: Other, en: Sword }
                b: { xx: [one], en: [first, second] }
                c: { de: Schwert }
                d: { en: "" }
                """);
        assertEquals("Sword", Texts.text(y.get("a"), "?"));
        assertEquals(List.of("first", "second"), Texts.lines(y.get("b")));
        assertEquals("Schwert", Texts.text(y.get("c"), "?"), "no English text: the first one");
        assertEquals("", Texts.text(y.get("d"), "?"));
    }
}
