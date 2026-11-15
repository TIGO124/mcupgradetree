package com.upgradetree.chest;

import com.upgradetree.config.Lang;
import com.upgradetree.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One possible prize of a chest: a Minecraft item stack or experience levels. Item stacks are only
 * built when needed (they need a running server), so chest files can be parsed in unit tests.
 */
public final class ChestReward {

    public enum Type { ITEM, LEVELS }

    public final int index;
    public final Type type;
    public final Rarity rarity;
    public final Material material;
    public final int amount;
    /** Optional MiniMessage name (items only), null for the plain item name. */
    public final String customName;
    /** Enchantment key → level, resolved when the item is created. */
    public final Map<String, Integer> enchantments;
    public final List<String> lore;
    public final int levels;
    /** Relative weight inside its rarity (1 = same as the others). */
    public final double weight;

    ChestReward(int index, Type type, Rarity rarity, Material material, int amount, String customName,
                Map<String, Integer> enchantments, List<String> lore, int levels, double weight) {
        this.index = index;
        this.type = type;
        this.rarity = rarity;
        this.material = material;
        this.amount = Math.max(1, amount);
        this.customName = customName == null || customName.isBlank() ? null : customName;
        this.enchantments = Map.copyOf(enchantments);
        this.lore = lore == null ? List.of() : List.copyOf(lore);
        this.levels = Math.max(0, levels);
        this.weight = Math.max(0, weight);
    }

    /** The stack given to the player (items only). */
    public ItemStack create() {
        ItemStack it = new ItemStack(material, Math.min(amount, 99));
        decorate(it, customName, lore, enchantments);
        return it;
    }

    /**
     * Gives a prize stack its optional MiniMessage name, lore lines and enchantments (keys such as
     * "sharpness"; unknown ones are left out). An enchanted book stores them, so they work in an anvil.
     */
    public static void decorate(ItemStack it, String name, List<String> lore, Map<String, Integer> enchantments) {
        it.editMeta(m -> {
            if (name != null) {
                m.customName(Text.mm(name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            if (lore != null && !lore.isEmpty()) {
                m.lore(lore.stream().map(l -> Text.mm(l).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList());
            }
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
            for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
                NamespacedKey key = NamespacedKey.fromString(e.getKey().toLowerCase(Locale.ROOT));
                Enchantment ench = key == null ? null : registry.get(key);
                if (ench == null) {
                    continue;
                }
                int level = Math.max(1, e.getValue());
                if (m instanceof EnchantmentStorageMeta book) {
                    book.addStoredEnchant(ench, level, true);
                } else {
                    m.addEnchant(ench, level, true);
                }
            }
        });
    }

    /** Icon shown on the roulette and in the menus (levels use an experience bottle). */
    public ItemStack icon() {
        if (type == Type.LEVELS) {
            ItemStack it = new ItemStack(Material.EXPERIENCE_BOTTLE);
            it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            return it;
        }
        ItemStack it = new ItemStack(material);
        if (!enchantments.isEmpty()) {
            it.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        return it;
    }

    /** MiniMessage name without colour, e.g. "&lt;lang:item.minecraft.diamond&gt; ×5" or "+10 Levels". */
    public String label(Lang lang) {
        if (type == Type.LEVELS) {
            return lang.format("chest-reward-levels", "amount", String.valueOf(levels));
        }
        String base = customName != null ? customName : "<lang:" + material.translationKey() + ">";
        return amount > 1 ? base + " ×" + amount : base;
    }

    /** The label in the colour of its rarity. */
    public String coloredLabel(Lang lang) {
        return rarity.tag() + label(lang) + "</color>";
    }

    /** Short amount text for a roulette card ("×5", "+30"), or "" for a single item. */
    public String badge() {
        if (type == Type.LEVELS) {
            return "+" + levels;
        }
        return amount > 1 ? "×" + amount : "";
    }
}
