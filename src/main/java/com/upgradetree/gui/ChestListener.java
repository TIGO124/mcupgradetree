package com.upgradetree.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/** Routes clicks in the chest menus (tree, chests shop, roulette, admin panel) and keeps their items in place. */
public final class ChestListener implements Listener {

    private static boolean ours(InventoryHolder h) {
        return h instanceof ChestMenu || h instanceof AdminPanel || h instanceof AdminPlayerPage
                || h instanceof ChestShopMenu || h instanceof RouletteMenu;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getView().getTopInventory().getHolder();
        if (!ours(holder)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player) || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        switch (holder) {
            case ChestMenu menu -> menu.click(e.getRawSlot());
            case AdminPanel panel -> panel.click(e.getRawSlot(), e.getClick());
            case AdminPlayerPage page -> page.click(e.getRawSlot(), e.getClick());
            case ChestShopMenu shop -> shop.click(e.getRawSlot(), e.getClick());
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (ours(e.getView().getTopInventory().getHolder())) {
            e.setCancelled(true);
        }
    }

    /** Closing the roulette early still gives the prize. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof RouletteMenu roulette) {
            roulette.closed();
        }
    }
}
