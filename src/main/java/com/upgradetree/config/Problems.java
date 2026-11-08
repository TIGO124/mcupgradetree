package com.upgradetree.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration problems (tree and chest files, config.yml), written in Turkish and English so
 * /tigo check shows them in the reader's language and the console in the server language.
 */
public final class Problems {

    /** Where a problem goes. */
    @FunctionalInterface
    public interface Sink {
        void warn(String tr, String en);
    }

    private Problems() {
    }

    /** The problem as a localized text; other languages fall back to English. */
    public static Localized<String> of(String tr, String en) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("en", en);
        m.put("tr", tr);
        return Localized.of(m);
    }

    /** Logs every problem in {@code language} (Turkish for "tr", English otherwise). */
    public static Sink log(Logger log, String language) {
        return (tr, en) -> log.warning("tr".equals(language) ? tr : en);
    }

    /** Text of a problem for the console in {@code language}. */
    public static String console(Localized<String> problem, String language) {
        String s = problem.get(language, "en");
        return s == null ? "" : s;
    }
}
