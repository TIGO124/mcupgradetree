package com.upgradetree.util;

/** ARGB colours used by the in-world menu (Roblox-style dark UI). */
public final class Palette {

    public static final int PANEL_STROKE = 0xFF0B0D16;
    public static final int PANEL = 0xFF1F2438;
    public static final int HEADER = 0xFF2A3050;
    public static final int CANVAS = 0xFF161A2A;
    public static final int SIDEBAR = 0xFF1A1F33;
    public static final int PILL = 0xFF11152A;
    public static final int STROKE = 0xFF0B0D16;
    public static final int LOCKED = 0xFF2E3446;
    public static final int AVAILABLE = 0xFF56607E;
    public static final int GOLD = 0xFFFFD54A;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int LINE_LOCKED = 0xFF2A3042;
    public static final int LINE_AVAILABLE = 0xFF8C96B4;
    public static final int BUY = 0xFF22C55E;
    public static final int DENY = 0xFFE5484D;
    public static final int MAXED_BUTTON = 0xFFEAB308;
    public static final int LOCKED_BUTTON = 0xFF4B5263;
    public static final int BAR_BG = 0xFF0B0D16;

    private Palette() {
    }

    public static int lighten(int argb, double f) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int) Math.round(r + (255 - r) * f);
        g = (int) Math.round(g + (255 - g) * f);
        b = (int) Math.round(b + (255 - b) * f);
        return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int darken(int argb, double f) {
        int a = argb >>> 24;
        int r = (int) Math.round(((argb >> 16) & 0xFF) * (1 - f));
        int g = (int) Math.round(((argb >> 8) & 0xFF) * (1 - f));
        int b = (int) Math.round((argb & 0xFF) * (1 - f));
        return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /** Blends two colours: t = 0 gives a, t = 1 gives b (alpha taken from a). */
    public static int mix(int a, int b, double t) {
        double f = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
        int bl = (int) Math.round((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return (a & 0xFF000000) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(bl);
    }

    /** Same colour with a new alpha (0..255). */
    public static int alpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
    }

    /** Parses "#RRGGBB" / "RRGGBB" into an opaque ARGB int. */
    public static int parse(String hex, int def) {
        if (hex == null) {
            return def;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return def;
        }
        try {
            return 0xFF000000 | Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
