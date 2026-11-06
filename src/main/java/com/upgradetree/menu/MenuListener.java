package com.upgradetree.menu;

import com.upgradetree.UpgradeTreePlugin;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Turns clicks into menu actions. The panel stays in the world and the player moves freely: only
 * while the crosshair is on the panel are clicks and keys taken by the menu (and the matching
 * gameplay action cancelled). Looking away, everything works as usual.
 */
public final class MenuListener implements Listener {

    private final UpgradeTreePlugin plugin;
    private final MenuManager menus;

    public MenuListener(UpgradeTreePlugin plugin, MenuManager menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    /** The open menu if it takes this player's input right now, else null. */
    private DisplayMenu capturing(Player p) {
        DisplayMenu menu = menus.get(p);
        return menu != null && menu.captures() ? menu : null;
    }

    /** Left click: the client always sends a swing (punch) packet, even when hitting nothing. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwing(PlayerArmSwingEvent e) {
        DisplayMenu menu = capturing(e.getPlayer());
        if (menu == null) {
            return;
        }
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND) {
            menu.click(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        DisplayMenu menu = capturing(e.getPlayer());
        if (menu == null) {
            return;
        }
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        Action a = e.getAction();
        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            // a denied item use makes the server re-send the real held item, so hide it again next tick
            menu.rehideHand();
            if (e.getHand() == EquipmentSlot.HAND) {
                menu.click(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (capturing(e.getPlayer()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if (capturing(e.getPlayer()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && capturing(p) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && plugin.settings().closeOnDamage && menus.displayOpen(p)) {
            menus.close(p, false);
        }
    }

    /** Sneaking closes the menu while looking at it (sneaking elsewhere is just sneaking). */
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking() && capturing(e.getPlayer()) != null) {
            menus.close(e.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        DisplayMenu menu = menus.get(e.getPlayer());
        if (menu == null) {
            return;
        }
        // dropping also sends a swing; never let it count as a click
        menu.ignoreClicks(2);
        if (menu.captures()) {
            e.setCancelled(true);
            menu.rehideHand();
        }
    }

    /** A picked-up item can land in the held slot; the server then shows it in the hand again. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            DisplayMenu menu = capturing(p);
            if (menu != null) {
                menu.rehideHand();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        DisplayMenu menu = capturing(e.getPlayer());
        if (menu != null) {
            e.setCancelled(true);
            menu.rehideHand();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHotbar(PlayerItemHeldEvent e) {
        DisplayMenu menu = capturing(e.getPlayer());
        if (menu != null) {
            e.setCancelled(true);
            menu.rehideHand();
        }
    }

    /*
     * While the hand is hidden, the player's own inventory screen (which opens without telling the
     * server) still holds the invisible placeholder item. Edits there are blocked so creative mode can
     * never write the placeholder into the real inventory. Other plugins' GUIs are left alone.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        blockOwnInventory(e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        blockOwnInventory(e);
    }

    private void blockOwnInventory(InventoryInteractEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || e.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            return;
        }
        DisplayMenu menu = menus.get(p);
        if (menu != null && menu.handHidden()) {
            e.setCancelled(true);
            menu.rehideHand();
        }
    }

    /** Another plugin opened a GUI (a chest, /ec, a shop...): step out of the tree so the two never overlap. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p && menus.displayOpen(p)) {
            menus.close(p, false);
        }
    }

    /** Starting to mine the block behind the panel while clicking on it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDig(BlockDamageEvent e) {
        if (capturing(e.getPlayer()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        if (capturing(e.getPlayer()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (capturing(e.getPlayer()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (menus.displayOpen(e.getPlayer()) && (e.getFrom().getWorld() != e.getTo().getWorld()
                || e.getFrom().distanceSquared(e.getTo()) > 1.0)) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTask(plugin, () -> menus.close(p, true));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        menus.close(e.getPlayer(), true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        menus.close(e.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent e) {
        menus.close(e.getPlayer(), true);
    }
}
