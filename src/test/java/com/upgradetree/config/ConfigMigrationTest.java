package com.upgradetree.config;

import com.upgradetree.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigrationTest {

    @AfterEach
    void defaultPalette() {
        Text.palette(Style.DEFAULT.palette);
    }

    @Test
    void oldDefaultsGoAndCustomValuesStay() throws Exception {
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString("""
                currency:
                  symbol: "✦"
                menu:
                  stars: true
                theme:
                  background: "#0a0d1a"
                  buy: "#22C55E"
                  gold: "#FF0000"
                """);
        assertTrue(ConfigMigration.apply(c));
        assertNull(c.get("theme.background"), "an old default goes, the style decides");
        assertNull(c.get("theme.buy"));
        assertEquals("#FF0000", c.getString("theme.gold"), "a colour the owner chose stays");
        assertEquals("", c.getString("currency.symbol"));
        assertEquals("IMAGE", c.getString("menu.background"));
        assertNull(c.get("menu.stars"));
        assertFalse(ConfigMigration.apply(c), "running twice changes nothing");
    }

    @Test
    void anUntouchedThemeDisappears() throws Exception {
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString("theme:\n  background: \"#0A0D1A\"\n  gold: \"#FFD54A\"\n");
        assertTrue(ConfigMigration.apply(c));
        assertNull(c.get("theme"));
    }

    @Test
    void starsSwitchedOffBecomeAPlainPanel() throws Exception {
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString("menu:\n  stars: false\n");
        assertTrue(ConfigMigration.apply(c));
        assertEquals("NONE", c.getString("menu.background"));
    }

    /** The plugin's config always has the bundled file as defaults; those must not hide an old key. */
    @Test
    void bundledDefaultsDoNotHideTheOldStarsKey() throws Exception {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.loadFromString("menu:\n  background: IMAGE\n");
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString("menu:\n  stars: true\n");
        c.setDefaults(defaults);
        assertTrue(ConfigMigration.apply(c));
        assertEquals("IMAGE", c.getString("menu.background"));
        assertFalse(c.contains("menu.stars", true));
    }

    @Test
    void stylesAreCompleteAndFoundByName() {
        for (Style s : Style.values()) {
            assertEquals(Set.of(Style.TAGS), s.palette.keySet(), s.name());
            assertEquals(0xFF, s.theme.locked() >>> 24, s + ": node base must be opaque (hexagons overlap)");
            assertEquals(0xFF, s.theme.gold() >>> 24, s.name());
            assertEquals(0xFF, s.theme.maxed() >>> 24, s.name());
        }
        assertSame(Style.NIGHT, Style.of("night", Style.WOOD));
        assertSame(Style.MINIMAL, Style.of(" Minimal ", Style.NIGHT));
        assertSame(Style.WOOD, Style.of("nope", Style.WOOD));
        assertSame(Style.WOOD, Style.of(null, Style.WOOD));
    }

    /** Menu layers must be opaque: the client sorts see-through text backgrounds and could draw the world through a card. */
    @Test
    void menuColoursAreOpaque() {
        for (Style s : Style.values()) {
            Settings.Theme t = s.theme.opaque();
            for (int c : new int[]{t.background(), t.panelBorder(), t.header(), t.card(), t.locked(), t.line(), t.lineLocked(),
                    t.buy(), t.deny(), t.gold(), t.maxed(), t.hover()}) {
                assertEquals(0xFF, c >>> 24, s.name());
            }
            assertEquals(s.theme.gold(), t.gold(), s + ": an opaque colour stays as it is");
        }
        Settings.Theme glass = new Settings.Theme(0xFF000000, 0x80FFFFFF, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.5).opaque();
        assertEquals(0xFF808080, glass.panelBorder(), "half see-through white over black is grey");
        assertEquals(0xFF000000, glass.header(), "fully see-through is the background");
    }

    /** Every bundled message, tree and chest text only uses colour names the plugin knows. */
    @Test
    void bundledTextsUseKnownColours() throws Exception {
        for (String f : new String[]{"/messages.yml", "/trees/main.yml", "/trees/abilities.yml", "/trees/gatherer.yml",
                "/chests.yml"}) {
            YamlConfiguration y = new YamlConfiguration();
            try (InputStream in = ConfigMigrationTest.class.getResourceAsStream(f)) {
                y.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            for (String key : y.getKeys(true)) {
                Object v = y.get(key);
                for (Object line : v instanceof List<?> l ? l : List.of(v)) {
                    if (!(line instanceof String s)) {
                        continue;
                    }
                    String plain = Text.plain(s);
                    for (String tag : Style.TAGS) {
                        assertFalse(plain.contains("<" + tag + ">"), f + " " + key + ": " + s);
                    }
                    assertFalse(s.contains("<gradient") || s.contains("<color:"), f + " " + key + ": " + s);
                    assertFalse(s.contains("✦") || s.contains("★") || s.contains("✔"), f + " " + key + ": " + s);
                }
            }
        }
    }

    @Test
    void colourNamesFollowTheStyle() {
        Text.palette(Style.MINIMAL.palette);
        assertEquals(TextColor.fromHexString(Style.MINIMAL.palette.get("accent")), Text.mm("<accent>x").color());
        Text.palette(Style.CELESTIAL.palette);
        assertEquals(TextColor.fromHexString(Style.CELESTIAL.palette.get("accent")), Text.mm("<accent>x").color());
        assertEquals("Tree", Text.plain("<accent>Tree</accent>"));
    }

    @Test
    void romanNumeralsAndSymbol() {
        assertEquals("I", Text.roman(1));
        assertEquals("IV", Text.roman(4));
        assertEquals("XIV", Text.roman(14));
        assertEquals("0", Text.roman(0));
        assertEquals("", Text.symbol(""));
        assertEquals("<accent>$</accent> ", Text.symbol("$"));
    }
}
