package com.upgradetree.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A text of a content file (tree, chest, config) that may be written per language:
 * <pre>
 * name: "Keskin Kılıç"                              # every language
 * name: { tr: "Keskin Kılıç", en: "Sharp Sword" }   # per language
 * </pre>
 * Lookup order: the reader's language, the server language, then the first one written.
 */
public final class Localized<T> {

    private static final Localized<?> EMPTY = new Localized<>(null, Map.of());

    private final T all;
    private final Map<String, T> byLanguage;

    private Localized(T all, Map<String, T> byLanguage) {
        this.all = all;
        this.byLanguage = byLanguage;
    }

    @SuppressWarnings("unchecked")
    public static <T> Localized<T> empty() {
        return (Localized<T>) EMPTY;
    }

    /** The same value in every language. */
    public static <T> Localized<T> of(T value) {
        return value == null ? empty() : new Localized<>(value, Map.of());
    }

    public static <T> Localized<T> of(Map<String, T> byLanguage) {
        Map<String, T> m = new LinkedHashMap<>();
        for (Map.Entry<String, T> e : byLanguage.entrySet()) {
            if (e.getValue() != null) {
                m.put(e.getKey().trim().toLowerCase(Locale.ROOT), e.getValue());
            }
        }
        return m.isEmpty() ? empty() : new Localized<>(null, Collections.unmodifiableMap(m));
    }

    /** A text: a string, a {language: string} map, or nothing ({@code def}, which may be null). */
    public static Localized<String> text(Object raw, String def) {
        Map<?, ?> map = asMap(raw);
        if (map != null) {
            Map<String, String> m = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getValue() != null && asMap(e.getValue()) == null && !(e.getValue() instanceof List<?>)) {
                    m.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
            Localized<String> l = of(m);
            return l.isEmpty() ? of(def) : l;
        }
        if (raw == null || raw instanceof List<?>) {
            return of(def);
        }
        return of(String.valueOf(raw));
    }

    /** Lines: a list (or one string), or a {language: list} map. */
    public static Localized<List<String>> lines(Object raw) {
        Map<?, ?> map = asMap(raw);
        if (map != null) {
            Map<String, List<String>> m = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                List<String> l = list(e.getValue());
                if (l != null) {
                    m.put(String.valueOf(e.getKey()), l);
                }
            }
            return of(m);
        }
        List<String> l = list(raw);
        return l == null ? of(List.of()) : of(l);
    }

    private static List<String> list(Object raw) {
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return List.copyOf(out);
        }
        if (raw != null && asMap(raw) == null) {
            String s = String.valueOf(raw);
            return s.isBlank() ? List.of() : List.of(s);
        }
        return null;
    }

    private static Map<?, ?> asMap(Object raw) {
        if (raw instanceof ConfigurationSection s) {
            return s.getValues(false);
        }
        return raw instanceof Map<?, ?> m ? m : null;
    }

    /** The value for a reader; {@code fallback} is the server language. Null only when nothing was written. */
    public T get(String code, String fallback) {
        if (!byLanguage.isEmpty()) {
            T v = code == null ? null : byLanguage.get(code);
            if (v == null && fallback != null) {
                v = byLanguage.get(fallback);
            }
            return v != null ? v : byLanguage.values().iterator().next();
        }
        return all;
    }

    public T get(Lang lang) {
        return lang == null ? base() : get(lang.code(), lang.fallback());
    }

    /** The value used where no reader is known (console, ids): Turkish if written, else the first one. */
    public T base() {
        if (all != null || byLanguage.isEmpty()) {
            return all;
        }
        T tr = byLanguage.get("tr");
        return tr != null ? tr : byLanguage.values().iterator().next();
    }

    public boolean isEmpty() {
        return all == null && byLanguage.isEmpty();
    }

    /** Languages written separately (empty for a plain value). */
    public Set<String> languages() {
        return byLanguage.keySet();
    }
}
