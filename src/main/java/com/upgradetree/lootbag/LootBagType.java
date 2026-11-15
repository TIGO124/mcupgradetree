package com.upgradetree.lootbag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

/** One rarity of loot bag (lootbags.yml "rarities"): its look, drop chance and possible contents. */
public final class LootBagType {

    /** One entry picked for an opened bag with its rolled amount (or levels). */
    public record Roll(LootEntry entry, int amount) {
    }

    public final String id;
    /** MiniMessage name of the bag item. */
    public final String name;
    public final int color;
    /** Percent chance per non-hostile mob killed by a player. */
    public final double chance;
    /** Item model of the bag, e.g. "minecraft:purple_bundle". */
    public final String model;
    public final boolean glint;
    /** A dropped bag glows. */
    public final boolean glow;
    /** The whole server is told when someone finds this bag. */
    public final boolean announce;
    /** 0 for the first (most common) rarity of the file, then 1, 2... */
    public final int rank;
    public final List<LootEntry> loot;
    private final int minItems;
    private final int maxItems;

    LootBagType(String id, String name, int color, double chance, String model, boolean glint, boolean glow,
                boolean announce, int rank, int minItems, int maxItems, List<LootEntry> loot) {
        this.id = id;
        this.name = name == null || name.isEmpty() ? id : name;
        this.color = color;
        this.chance = Math.max(0, chance);
        this.model = model;
        this.glint = glint;
        this.glow = glow;
        this.announce = announce;
        this.rank = rank;
        this.loot = List.copyOf(loot);
        // a bag never holds the same entry twice, so it cannot hold more items than it has entries
        int pickable = (int) this.loot.stream().filter(e -> e.weight > 0).count();
        this.maxItems = Math.max(0, Math.min(maxItems, pickable));
        this.minItems = Math.max(0, Math.min(minItems, this.maxItems));
    }

    /** Fewest different items an opened bag holds. */
    public int minItems() {
        return minItems;
    }

    /** Most different items an opened bag holds. */
    public int maxItems() {
        return maxItems;
    }

    /**
     * The contents of one opened bag: between {@link #minItems()} and {@link #maxItems()} different
     * entries, each picked by its weight, in the order of the loot list.
     */
    public List<Roll> open(RandomGenerator random) {
        int count = minItems == maxItems ? maxItems : minItems + random.nextInt(maxItems - minItems + 1);
        List<LootEntry> pool = new ArrayList<>();
        for (LootEntry e : loot) {
            if (e.weight > 0) {
                pool.add(e);
            }
        }
        List<Roll> out = new ArrayList<>();
        while (out.size() < count && !pool.isEmpty()) {
            double total = 0;
            for (LootEntry e : pool) {
                total += e.weight;
            }
            double r = random.nextDouble() * total;
            int pick = pool.size() - 1;
            for (int i = 0; i < pool.size(); i++) {
                r -= pool.get(i).weight;
                if (r < 0) {
                    pick = i;
                    break;
                }
            }
            LootEntry e = pool.remove(pick);
            out.add(new Roll(e, e.roll(random)));
        }
        out.sort(Comparator.comparingInt(roll -> roll.entry().index));
        return out;
    }
}
