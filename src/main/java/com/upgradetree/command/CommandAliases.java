package com.upgradetree.command;

import java.util.Locale;
import java.util.Map;

/**
 * Short and alternative spellings of sub commands and option words. Help and tab completion only
 * show the main names.
 */
public final class CommandAliases {

    /** First argument: sub command names. */
    private static final Map<String, String> SUBS = Map.of(
            "?", "help",
            "reset", "respec",
            "leaderboard", "top",
            "chest", "chests");

    /** Later arguments: option words. Player names never collide with these in practice. */
    private static final Map<String, String> WORDS = Map.of(
            "add", "give",
            "level", "levels",
            "gui", "chest",
            "3d", "panel",
            "display", "panel",
            "*", "all");

    private CommandAliases() {
    }

    /** Main name of a sub command; unknown input comes back lower-cased. */
    public static String sub(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        return SUBS.getOrDefault(s, s);
    }

    /** Main name of an option word ("3d" → "panel"); unknown input comes back lower-cased. */
    public static String word(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        return WORDS.getOrDefault(s, s);
    }
}
