package com.upgradetree.lootbag;

import com.upgradetree.chest.ChestReward;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * One possible content of a loot bag: an item with an amount range, or a range of XP levels.
 * Item stacks are only built when needed (they need a running server), so lootbags.yml can be
 * parsed in unit tests.
 */
public final class LootEntry {

    public enum Type { ITEM, LEVELS }

    /** Position in the bag's loot list (the order items are listed in when a bag opens). */
    public final int index;
    public final Type type;
    /** The item (an experience bottle for levels). */
    public final Material material;
    /** Amount (items) or levels, both inclusive. */
    public final int min;
    public final int max;
    /** How often this entry is picked compared to the others of the bag. */
    public final double weight;
    /** Optional MiniMessage name (items only), null for the plain item name. */
    public final String customName;
    public final List<String> lore;
    /** Enchantment key → level, resolved when the item is created. */
    public final Map<String, Integer> enchantments;

    LootEntry(int index, Type type, Material material, int min, int max, double weight, String customName,
              List<String> lore, Map<String, Integer> enchantments) {
        this.index = index;
        this.type = type;
        this.material = material;
        this.min = Math.max(1, Math.min(min, max));
        this.max = Math.max(this.min, max);
        this.weight = Math.max(0, weight);
        this.customName = customName == null || customName.isBlank() ? null : customName;
        this.lore = lore == null ? List.of() : List.copyOf(lore);
        this.enchantments = Map.copyOf(enchantments);
    }

    /** A random amount (or number of levels) between min and max. */
    public int roll(RandomGenerator random) {
        return min == max ? min : min + random.nextInt(max - min + 1);
    }

    /** One item of this entry with its name, lore and enchantments (items only). */
    public ItemStack create() {
        ItemStack it = new ItemStack(material);
        ChestReward.decorate(it, customName, lore, enchantments);
        return it;
    }
}
