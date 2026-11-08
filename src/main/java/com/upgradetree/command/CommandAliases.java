package com.upgradetree.command;

import java.util.Locale;
import java.util.Map;

/**
 * Commands are English. The old Turkish spellings (from 2.1 and earlier) still work as hidden
 * aliases, so command blocks, NPC plugins and players' habits keep working; help and tab completion
 * only show the English names.
 */
public final class CommandAliases {

    /** First argument: sub command names. */
    private static final Map<String, String> SUBS = Map.ofEntries(
            Map.entry("?", "help"),
            Map.entry("yardim", "help"),
            Map.entry("yardım", "help"),
            Map.entry("bilgi", "info"),
            Map.entry("ac", "open"),
            Map.entry("aç", "open"),
            Map.entry("yenile", "reload"),
            Map.entry("kontrol", "check"),
            Map.entry("sifirla", "respec"),
            Map.entry("sıfırla", "respec"),
            Map.entry("dagit", "respec"),
            Map.entry("dağıt", "respec"),
            Map.entry("reset", "respec"),
            Map.entry("ayarla", "set"),
            Map.entry("puan", "points"),
            Map.entry("prestij", "prestige"),
            Map.entry("liderler", "top"),
            Map.entry("menü", "menu"),
            Map.entry("gunluk", "daily"),
            Map.entry("günlük", "daily"),
            Map.entry("temizle", "cleanup"),
            Map.entry("panel", "admin"),
            Map.entry("yonetim", "admin"),
            Map.entry("yönetim", "admin"),
            Map.entry("dil", "language"),
            Map.entry("lang", "language"),
            Map.entry("sunucudili", "serverlanguage"),
            Map.entry("serverlang", "serverlanguage"));

    /** Later arguments: option words. Player names never collide with these in practice. */
    private static final Map<String, String> WORDS = Map.ofEntries(
            Map.entry("onayla", "confirm"),
            Map.entry("ver", "give"),
            Map.entry("ekle", "give"),
            Map.entry("al", "take"),
            Map.entry("cikar", "take"),
            Map.entry("çıkar", "take"),
            Map.entry("ayarla", "set"),
            Map.entry("koy", "place"),
            Map.entry("kaldir", "remove"),
            Map.entry("kaldır", "remove"),
            Map.entry("prestij", "prestige"),
            Map.entry("seviye", "levels"),
            Map.entry("level", "levels"),
            Map.entry("sandik", "chest"),
            Map.entry("sandık", "chest"),
            Map.entry("gui", "chest"),
            Map.entry("3d", "panel"),
            Map.entry("display", "panel"),
            Map.entry("otomatik", "auto"),
            Map.entry("herkes", "all"),
            Map.entry("*", "all"),
            Map.entry("onizleme", "preview"),
            Map.entry("önizleme", "preview"),
            Map.entry("bak", "look"),
            Map.entry("konum", "pos"),
            Map.entry("varlik", "entities"),
            Map.entry("varlık", "entities"),
            Map.entry("hedef", "target"),
            Map.entry("varsayilan", "default"),
            Map.entry("varsayılan", "default"),
            Map.entry("sunucu", "default"),
            Map.entry("server", "default"),
            Map.entry("auto", "default"));

    private CommandAliases() {
    }

    /** English name of a sub command; unknown input comes back lower-cased. */
    public static String sub(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        return SUBS.getOrDefault(s, s);
    }

    /** English name of an option word ("onayla" → "confirm"); unknown input comes back lower-cased. */
    public static String word(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        return WORDS.getOrDefault(s, s);
    }
}
