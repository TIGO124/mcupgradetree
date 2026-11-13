package com.upgradetree.chest;

/**
 * A reward tier (CS2-style colour grade). {@code name} is MiniMessage (coloured in the file),
 * {@code weight} the share of openings that land on this tier and {@code rank} orders tiers from
 * the most common (0) to the rarest.
 */
public record Rarity(String id, String name, int color, double weight, boolean announce, int rank) {

    public Rarity {
        name = name == null || name.isEmpty() ? id : name;
    }

    /** MiniMessage colour tag of this tier, e.g. "&lt;color:#4B69FF&gt;". */
    public String tag() {
        return String.format("<color:#%06X>", color & 0xFFFFFF);
    }
}
