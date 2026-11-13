package com.upgradetree.config;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Map;

/**
 * Ready-made looks of the menu (config.yml "menu.style"). A style sets the menu colours
 * ({@link Settings.Theme}; single colours can still be overridden under "theme") and the colours
 * behind the message colour names ({@code <hi>}, {@code <body>}, {@code <accent>}...).
 * Header and card colours are see-through so a background image shows behind them.
 */
public enum Style {

    /** Dark blue glass, ice-blue lines, warm gold for finished upgrades. */
    NIGHT(new Settings.Theme(0xFF0B1018, 0xFF2B3B55, 0xB80A0F18, 0xB80A0F18, 0xFF151D2A, 0xFF7F9CC4, 0x8C2C3A50,
            0xFF3D6B9E, 0xFF9B3A45, 0xFFD3E3F5, 0xFFE3C27A, 0xFFFFFFFF, 0.5),
            palette("#EEF3FA", "#A9B6CA", "#647189", "#9CC3F0", "#DDE7F3", "#E07A7A", "#8DCFA6", "#9CC3F0", "#B8A9E6"),
            Material.BLUE_STAINED_GLASS_PANE),

    /** Near black with champagne gold: a quiet, celestial look. */
    CELESTIAL(new Settings.Theme(0xFF0C0D12, 0xFF5A4A2E, 0xBF0B0B10, 0xBF0B0B10, 0xFF17161B, 0xFF8C7A55, 0x8C3A352C,
            0xFF7E6230, 0xFF8E3A36, 0xFFE2C27A, 0xFFF4E4B8, 0xFFFFF6E0, 0.3),
            palette("#F3EEE2", "#C5BFB1", "#7B766B", "#E2C27A", "#F1DFA8", "#DE806F", "#A8CB8C", "#AFC4DB", "#C6AEDB"),
            Material.BLACK_STAINED_GLASS_PANE),

    /** Black and white: thin frames, no colour except where it means something. */
    MINIMAL(new Settings.Theme(0xFF0A0A0C, 0x59FFFFFF, 0x99000000, 0x99000000, 0xFF1A1B1F, 0xFF9A9CA3, 0x40FFFFFF,
            0xFF3A3C42, 0xFF7A2E2E, 0xFFE6E6E6, 0xFF9AD3A0, 0xFFFFFFFF, 0.0),
            palette("#FFFFFF", "#BDBEC2", "#6E7076", "#FFFFFF", "#E6E6E6", "#E27C7C", "#9AD3A0", "#BDBEC2", "#C9C2DE"),
            Material.GRAY_STAINED_GLASS_PANE),

    /** Walnut planks, brass frame and parchment text. */
    WOOD(new Settings.Theme(0xFF21170F, 0xFF6E5233, 0xFF2F2218, 0xFF2A1E15, 0xFF3A2D22, 0xFFA88B5E, 0xFF33261B,
            0xFF5E7B35, 0xFF8E3026, 0xFFD2A24C, 0xFF84A060, 0xFFEFE3C6, 1.0),
            palette("#EFE3C6", "#C4AE88", "#8C7559", "#D2A24C", "#E2C27A", "#C8553D", "#9DB565", "#9FB6C8", "#B79AD0"),
            Material.BROWN_STAINED_GLASS_PANE);

    /** The style used when config.yml names none. */
    public static final Style DEFAULT = NIGHT;

    /** Message colour names, in the order of {@link #palette(String...)}. */
    public static final String[] TAGS = Names.ALL.clone();

    /** Holder: enum constants are built before the enum's own static fields. */
    private static final class Names {
        static final String[] ALL = {"hi", "body", "dim", "accent", "value", "bad", "good", "info", "magic"};
    }

    public final Settings.Theme theme;
    /** Message colour name → #RRGGBB. */
    public final Map<String, String> palette;
    /** Filler of the chest menus. */
    public final Material glass;

    Style(Settings.Theme theme, Map<String, String> palette, Material glass) {
        this.theme = theme;
        this.palette = palette;
        this.glass = glass;
    }

    private static Map<String, String> palette(String... hex) {
        String[] t = Names.ALL;
        return Map.of(t[0], hex[0], t[1], hex[1], t[2], hex[2], t[3], hex[3], t[4], hex[4],
                t[5], hex[5], t[6], hex[6], t[7], hex[7], t[8], hex[8]);
    }

    /** The style with this name (case-insensitive), or {@code def}. */
    public static Style of(String name, Style def) {
        if (name == null) {
            return def;
        }
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "NIGHT" -> NIGHT;
            case "CELESTIAL", "GOLD" -> CELESTIAL;
            case "MINIMAL" -> MINIMAL;
            case "WOOD", "RPG" -> WOOD;
            default -> def;
        };
    }
}
