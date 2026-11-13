package com.upgradetree.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text helpers: MiniMessage parsing (with legacy &amp; code support), placeholders,
 * and a width estimator for Minecraft's default font (used for line wrapping).
 */
public final class Text {

    /**
     * Message colour names of the chosen style ({@code <hi>}, {@code <body>}, {@code <dim>}, {@code <accent>},
     * {@code <value>}, {@code <bad>}, {@code <good>}, {@code <info>}, {@code <magic>}), name → #RRGGBB.
     */
    private static volatile Map<String, String> palette = com.upgradetree.config.Style.DEFAULT.palette;
    private static volatile MiniMessage MM = MiniMessage.builder().tags(paletteTags(palette)).build();

    /** Switches the message colours (style chosen in config.yml). */
    public static void palette(Map<String, String> colors) {
        palette = Map.copyOf(colors);
        MM = MiniMessage.builder().tags(paletteTags(palette)).build();
    }

    public static Map<String, String> palette() {
        return palette;
    }

    private static TagResolver paletteTags(Map<String, String> colors) {
        TagResolver.Builder b = TagResolver.builder().resolver(TagResolver.standard());
        for (Map.Entry<String, String> e : colors.entrySet()) {
            b.resolver(Placeholder.styling(e.getKey(), TextColor.fromHexString(e.getValue())));
        }
        return b.build();
    }
    private static final Pattern HEX_AMP = Pattern.compile("[&§]#([0-9a-fA-F]{6})");
    private static final Pattern BUKKIT_HEX = Pattern.compile("[&§][xX]((?:[&§][0-9a-fA-F]){6})");
    private static final Map<Character, String> CODES = new HashMap<>();

    static {
        String[] colors = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
                "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
        String hex = "0123456789abcdef";
        for (int i = 0; i < 16; i++) {
            CODES.put(hex.charAt(i), "<reset><" + colors[i] + ">");
        }
        CODES.put('l', "<b>");
        CODES.put('o', "<i>");
        CODES.put('n', "<u>");
        CODES.put('m', "<st>");
        CODES.put('k', "<obf>");
        CODES.put('r', "<reset>");
    }

    private Text() {
    }

    /** Converts legacy (&amp;a, &amp;#RRGGBB, &amp;x&amp;R...) codes into MiniMessage tags. */
    public static String legacyToMini(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf('&') < 0 && s.indexOf('§') < 0) {
            return s;
        }
        Matcher m = BUKKIT_HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String hex = m.group(1).replaceAll("[&§]", "");
            m.appendReplacement(sb, Matcher.quoteReplacement("<reset><#" + hex + ">"));
        }
        m.appendTail(sb);
        s = sb.toString();

        m = HEX_AMP.matcher(s);
        sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("<reset><#" + m.group(1) + ">"));
        }
        m.appendTail(sb);
        s = sb.toString();

        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < s.length()) {
                String rep = CODES.get(Character.toLowerCase(s.charAt(i + 1)));
                if (rep != null) {
                    out.append(rep);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /** Parses a MiniMessage (or legacy coded) string. Never throws. */
    public static Component mm(String s) {
        if (s == null || s.isEmpty()) {
            return Component.empty();
        }
        try {
            return MM.deserialize(legacyToMini(s));
        } catch (Exception e) {
            return Component.text(s);
        }
    }

    public static Component mm(String s, String... placeholders) {
        return mm(apply(s, placeholders));
    }

    /** Replaces {key} placeholders. Arguments are key, value, key, value... */
    public static String apply(String s, String... kv) {
        if (s == null) {
            return "";
        }
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return s;
    }

    /** The currency symbol before an amount ("{symbol}" in menu-currency), or nothing when none is set. */
    public static String symbol(String symbol) {
        return symbol == null || symbol.isEmpty() ? "" : "<accent>" + escape(symbol) + "</accent> ";
    }

    /** Roman numeral for levels and prestige (1 → I, 4 → IV); plain digits from 4000 on or below 1. */
    public static String roman(int n) {
        if (n < 1 || n >= 4000) {
            return String.valueOf(n);
        }
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] marks = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                sb.append(marks[i]);
                n -= values[i];
            }
        }
        return sb.toString();
    }

    /** Escapes a value that must not be interpreted as MiniMessage (player names etc.). */
    public static String escape(String s) {
        return s == null ? "" : MM.escapeTags(s);
    }

    /** Plain text without any tags. */
    public static String plain(String mini) {
        if (mini == null) {
            return "";
        }
        try {
            return MM.stripTags(legacyToMini(mini));
        } catch (Exception e) {
            return mini;
        }
    }

    /** Approximate advance (in font pixels) of a character in Minecraft's default font. */
    public static int charWidth(char c) {
        switch (c) {
            case ' ':
                return 4;
            case '!':
            case '\'':
            case ',':
            case '.':
            case ':':
            case ';':
            case '|':
            case 'i':
                return 2;
            case '`':
            case 'l':
                return 3;
            case '"':
            case '(':
            case ')':
            case '*':
            case 'I':
            case '[':
            case ']':
            case 't':
            case '{':
            case '}':
                return 4;
            case '<':
            case '>':
            case 'f':
            case 'k':
                return 5;
            case '@':
            case '~':
                return 7;
            default:
                return c < 0x0250 ? 6 : 9;
        }
    }

    /** Approximate rendered width of a plain string, in font pixels. */
    public static int width(String plain, boolean bold) {
        int w = 0;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            w += charWidth(c) + (bold && c != ' ' ? 1 : 0);
        }
        return w;
    }

    /**
     * Word-wraps a MiniMessage string so that every visual line is at most {@code maxPx}
     * font pixels wide. Tags are kept in place and are not counted. Returns the lines;
     * join them with "\n" to keep styles flowing across lines.
     */
    public static List<String> wrap(String mini, int maxPx) {
        List<String> out = new ArrayList<>();
        String src = legacyToMini(mini == null ? "" : mini);
        for (String para : src.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            int lineW = 0;
            int lastSpace = -1;
            int widthBeforeSpace = 0;
            boolean bold = false;
            int i = 0;
            while (i < para.length()) {
                char c = para.charAt(i);
                if (c == '<') {
                    int j = para.indexOf('>', i);
                    if (j > i) {
                        String tag = para.substring(i + 1, j).toLowerCase(Locale.ROOT);
                        if (tag.equals("b") || tag.equals("bold")) {
                            bold = true;
                        } else if (tag.equals("/b") || tag.equals("/bold") || tag.equals("reset") || tag.equals("r")) {
                            bold = false;
                        }
                        line.append(para, i, j + 1);
                        i = j + 1;
                        continue;
                    }
                }
                int cw = charWidth(c) + (bold && c != ' ' ? 1 : 0);
                if (c == ' ') {
                    lastSpace = line.length();
                    widthBeforeSpace = lineW;
                } else if (lineW + cw > maxPx && lastSpace >= 0) {
                    out.add(line.substring(0, lastSpace));
                    String rest = line.substring(lastSpace + 1);
                    line = new StringBuilder(rest);
                    lineW = Math.max(0, lineW - widthBeforeSpace - 4);
                    lastSpace = -1;
                }
                line.append(c);
                lineW += cw;
                i++;
            }
            out.add(line.toString());
        }
        return out;
    }

    /** Formats a duration as h:mm:ss or m:ss. */
    public static String duration(long millis) {
        long s = Math.max(0, (millis + 999) / 1000);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) : String.format(Locale.ROOT, "%d:%02d", m, sec);
    }

    /** Formats a number without needless decimals (5, 2.5, 0.75). */
    public static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return Long.toString(Math.round(v));
        }
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
