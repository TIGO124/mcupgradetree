package com.upgradetree.listener;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Other ways to open the menu: a hotbar item and tagged entities (NPCs). */
public final class AccessListener implements Listener {

    private final UpgradeTreePlugin plugin;
    private final NamespacedKey itemKey;

    public AccessListener(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "menu_item");
    }

    public boolean isMenuItem(ItemStack stack) {
        return stack != null && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /** The menu item, named in the player's language. */
    public ItemStack createItem(Player p) {
        Settings s = plugin.settings();
        ItemStack it = new ItemStack(s.accessItemMaterial);
        it.editMeta(m -> {
            m.itemName(Text.mm(s.accessItemName(plugin.lang(p))));
            m.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        });
        return it;
    }

    /** Gives the menu item if the player does not have one yet; renames an existing one to the player's language. */
    public void giveItem(Player p) {
        Settings s = plugin.settings();
        if (!s.accessItem || !p.hasPermission("upgradetree.use")) {
            return;
        }
        boolean has = false;
        for (ItemStack it : p.getInventory().getContents()) {
            if (isMenuItem(it)) {
                has = true;
                it.editMeta(m -> m.itemName(Text.mm(s.accessItemName(plugin.lang(p)))));
            }
        }
        if (has) {
            return;
        }
        ItemStack inSlot = p.getInventory().getItem(s.accessItemSlot);
        if (inSlot == null || inSlot.getType().isAir()) {
            p.getInventory().setItem(s.accessItemSlot, createItem(p));
        } else {
            p.getInventory().addItem(createItem(p));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                giveItem(p);
            }
        }, 5L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                giveItem(p);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || plugin.menus().capturesInput(e.getPlayer())) {
            return;
        }
        Action a = e.getAction();
        if ((a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) && isMenuItem(e.getItem())) {
            e.setCancelled(true);
            plugin.menus().toggle(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (isMenuItem(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isMenuItem);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntity(PlayerInteractEntityEvent e) {
        String tag = plugin.settings().entityTag;
        if (e.getHand() != EquipmentSlot.HAND || tag == null || tag.isBlank()
                || !e.getRightClicked().getScoreboardTags().contains(tag)) {
            return;
        }
        e.setCancelled(true);
        if (e.getPlayer().hasPermission("upgradetree.use")) {
            plugin.menus().open(e.getPlayer());
        }
    }
}
