package com.upgradetree.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Routes clicks in the chest menu and keeps its items in place. */
public final class ChestListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ChestMenu menu)) {
            return;
        }
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player && e.getClickedInventory() == e.getView().getTopInventory()) {
            menu.click(e.getRawSlot());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ChestMenu) {
            e.setCancelled(true);
        }
    }
}
