package com.upgradetree.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Small helpers for chest menu items. */
public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack named(ItemStack it, Component name, List<Component> lore) {
        it.editMeta(m -> {
            m.itemName(name);
            List<Component> clean = new ArrayList<>();
            for (Component c : lore) {
                clean.add(c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            m.lore(clean);
        });
        return it;
    }

    public static ItemStack item(Material m, Component name, List<Component> lore) {
        return named(new ItemStack(m), name, lore);
    }

    public static ItemStack glow(ItemStack it, boolean on) {
        if (on) {
            it.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        return it;
    }

    /** Glass of the empty slots (follows the menu style). */
    private static volatile Material glass = Material.GRAY_STAINED_GLASS_PANE;

    public static void glass(Material m) {
        glass = m;
    }

    public static ItemStack filler() {
        return named(new ItemStack(glass), Component.empty(), List.of());
    }

    /** Player head with the skin of an online player (plain head for offline ones). */
    public static ItemStack head(UUID uuid, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        Player online = Bukkit.getPlayer(uuid);
        it.editMeta(SkullMeta.class, meta -> {
            if (online != null) {
                meta.setPlayerProfile(online.getPlayerProfile());
            } else {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            }
        });
        return named(it, name, lore);
    }
}
