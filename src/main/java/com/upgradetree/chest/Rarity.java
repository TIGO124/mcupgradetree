package com.upgradetree.chest;

import com.upgradetree.config.Lang;
import com.upgradetree.config.Localized;

/**
 * A reward tier (CS2-style colour grade). {@code weight} is the share of openings that land on
 * this tier; {@code rank} orders tiers from the most common (0) to the rarest.
 */
public record Rarity(String id, Localized<String> names, int color, double weight, boolean announce, int rank) {

    /** Name in the reader's language (MiniMessage, coloured in the file). */
    public String name(Lang lang) {
        String n = names.get(lang);
        return n == null ? id : n;
    }

    /** MiniMessage colour tag of this tier, e.g. "&lt;color:#4B69FF&gt;". */
    public String tag() {
        return String.format("<color:#%06X>", color & 0xFFFFFF);
    }
}
