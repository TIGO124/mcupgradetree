package com.upgradetree.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A plugin or Minecraft version such as "2.4.0", "v2.4.0" or "26.3". Missing parts count as 0
 * ("2.4" equals "2.4.0"); a suffix after "-" marks a pre-release, older than the plain version.
 */
public final class Version implements Comparable<Version> {

    private static final Pattern FORMAT = Pattern.compile("[vV]?(\\d+(?:\\.\\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?");

    private final int[] numbers;
    private final String preRelease;
    private final String text;

    private Version(int[] numbers, String preRelease, String text) {
        this.numbers = numbers;
        this.preRelease = preRelease;
        this.text = text;
    }

    /** The version, or null when the text is not one. */
    public static Version parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        Matcher m = FORMAT.matcher(s);
        if (!m.matches()) {
            return null;
        }
        String[] parts = m.group(1).split("\\.");
        int[] numbers = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                numbers[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null; // too long to be a version
        }
        return new Version(numbers, m.group(2), s.startsWith("v") || s.startsWith("V") ? s.substring(1) : s);
    }

    @Override
    public int compareTo(Version o) {
        for (int i = 0; i < Math.max(numbers.length, o.numbers.length); i++) {
            int a = i < numbers.length ? numbers[i] : 0;
            int b = i < o.numbers.length ? o.numbers[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        if (preRelease == null || o.preRelease == null) {
            return preRelease == null ? (o.preRelease == null ? 0 : 1) : -1;
        }
        return preRelease.compareTo(o.preRelease);
    }

    public boolean newerThan(Version o) {
        return compareTo(o) > 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Version v && compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        int h = preRelease == null ? 0 : preRelease.hashCode();
        int last = numbers.length;
        while (last > 0 && numbers[last - 1] == 0) {
            last--;
        }
        for (int i = 0; i < last; i++) {
            h = 31 * h + numbers[i];
        }
        return h;
    }

    /** The version as written, without a leading "v". */
    @Override
    public String toString() {
        return text;
    }
}
