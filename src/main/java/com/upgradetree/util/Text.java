package com.upgradetree.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

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

    private static final MiniMessage MM = MiniMessage.miniMessage();
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
            case 'ı':
                return 2;
            case '`':
            case 'l':
                return 3;
            case '"':
            case '(':
            case ')':
            case '*':
            case 'I':
            case 'İ':
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
