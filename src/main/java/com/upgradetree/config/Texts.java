package com.upgradetree.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the texts of content files (trees, chests, config). A text is a string, a list of lines is
 * a list (or a single string). Files written by older versions may still hold per-language maps
 * such as {@code { tr: "...", en: "..." }}: the English entry is used, or the first one when there
 * is no English entry.
 */
public final class Texts {

    private Texts() {
    }

    /** A single text, or {@code def} (may be null) when nothing usable is written. */
    public static String text(Object raw, String def) {
        Object v = english(raw);
        if (v == null || v instanceof List<?> || asMap(v) != null) {
            return def;
        }
        return String.valueOf(v);
    }

    /** Lines: a list, a single string (one line, none when blank) or nothing (no lines). */
    public static List<String> lines(Object raw) {
        Object v = english(raw);
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return List.copyOf(out);
        }
        if (v == null || asMap(v) != null) {
            return List.of();
        }
        String s = String.valueOf(v);
        return s.isBlank() ? List.of() : List.of(s);
    }

    /** The English entry of a per-language map (or its first entry); anything else as it is. */
    static Object english(Object raw) {
        Map<?, ?> map = asMap(raw);
        if (map == null) {
            return raw;
        }
        Object en = map.get("en");
        if (en != null) {
            return en;
        }
        for (Object v : map.values()) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Map<?, ?> asMap(Object raw) {
        if (raw instanceof ConfigurationSection s) {
            return s.getValues(false);
        }
        return raw instanceof Map<?, ?> m ? m : null;
    }
}
