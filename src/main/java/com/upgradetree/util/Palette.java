package com.upgradetree.util;

/** ARGB colour helpers for the in-world menu (the colours themselves come from the theme). */
public final class Palette {

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

    /** Parses "#RRGGBB" (opaque) or "#AARRGGBB" (with alpha) into an ARGB int. */
    public static int parse(String hex, int def) {
        if (hex == null) {
            return def;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            if (s.length() == 6) {
                return 0xFF000000 | Integer.parseInt(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException e) {
            return def;
        }
        return def;
    }

    /** True when the colour is see-through. */
    public static boolean translucent(int argb) {
        return (argb >>> 24) < 0xFF;
    }

    public static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }

    /** Brightness 0..1 (linear weights, no gamma: good enough to compare colours). */
    public static double luminance(int argb) {
        return (0.2126 * ((argb >> 16) & 0xFF) + 0.7152 * ((argb >> 8) & 0xFF) + 0.0722 * (argb & 0xFF)) / 255.0;
    }

    /** The colour, darkened step by step until it is at most {@code max} bright (so white text stays readable). */
    public static int atMost(int argb, double max) {
        int c = argb;
        for (int i = 0; i < 20 && luminance(c) > max; i++) {
            c = darken(c, 0.15);
        }
        return c;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
