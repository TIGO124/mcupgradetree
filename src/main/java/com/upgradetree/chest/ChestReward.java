package com.upgradetree.chest;

import com.upgradetree.config.Lang;
import com.upgradetree.config.Localized;
import com.upgradetree.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

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
    /** Optional MiniMessage name (items only), possibly per language. */
    public final Localized<String> names;
    /** Enchantment key → level, resolved when the item is created. */
    public final Map<String, Integer> enchantments;
    public final Localized<List<String>> lore;
    public final int levels;
    /** Relative weight inside its rarity (1 = same as the others). */
    public final double weight;

    ChestReward(int index, Type type, Rarity rarity, Material material, int amount, Localized<String> names,
                Map<String, Integer> enchantments, Localized<List<String>> lore, int levels, double weight) {
        this.index = index;
        this.type = type;
        this.rarity = rarity;
        this.material = material;
        this.amount = Math.max(1, amount);
        this.names = names == null ? Localized.empty() : names;
        this.enchantments = Map.copyOf(enchantments);
        this.lore = lore == null ? Localized.of(List.of()) : lore;
        this.levels = Math.max(0, levels);
        this.weight = Math.max(0, weight);
    }

    /** Custom name in the reader's language, or null for the plain item name. */
    public String customName(Lang lang) {
        String n = names.get(lang);
        return n == null || n.isBlank() ? null : n;
    }

    /** The stack given to the player (items only), named in the receiver's language. */
    public ItemStack create(Lang lang) {
        ItemStack it = new ItemStack(material, Math.min(amount, 99));
        String name = customName(lang);
        List<String> lines = lore.get(lang);
        it.editMeta(m -> {
            if (name != null) {
                m.customName(Text.mm(name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            if (lines != null && !lines.isEmpty()) {
                m.lore(lines.stream().map(l -> Text.mm(l).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList());
            }
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
            for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
                NamespacedKey key = NamespacedKey.fromString(e.getKey().toLowerCase(Locale.ROOT));
                Enchantment ench = key == null ? null : registry.get(key);
                if (ench != null) {
                    m.addEnchant(ench, Math.max(1, e.getValue()), true);
                }
            }
        });
        return it;
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
        String custom = customName(lang);
        String base = custom != null ? custom : "<lang:" + material.translationKey() + ">";
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
