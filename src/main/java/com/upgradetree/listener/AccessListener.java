package com.upgradetree.listener;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
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
        if (!isMenuItem(e.getItem())) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // never placed or used on a block (composter, decorated pot...), whichever hand holds it
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND && !plugin.menus().capturesInput(e.getPlayer())) {
            plugin.menus().toggle(e.getPlayer());
        }
    }

    /*
     * The menu item must never leave the player's inventory: a missing item is handed out again on
     * join and respawn, so an item stored in a chest, frame or bundle would be duplicated (a free
     * nether star every relog). It cannot be used in recipes either.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        ItemStack hotbar = e.getHotbarButton() >= 0 ? e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) : null;
        ItemStack offhand = e.getClick() == ClickType.SWAP_OFFHAND ? e.getWhoClicked().getInventory().getItemInOffHand() : null;
        boolean involved = isMenuItem(e.getCurrentItem()) || isMenuItem(e.getCursor()) || isMenuItem(hotbar) || isMenuItem(offhand);
        if (!involved) {
            return;
        }
        boolean ownInventory = e.getView().getTopInventory().getType() == InventoryType.CRAFTING;
        if (!ownInventory || isBundle(e.getCurrentItem()) || isBundle(e.getCursor())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (isMenuItem(e.getOldCursor()) && e.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (isMenuItem(it)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    /** Item frames, allays, armour stands, animals: the menu item is never handed to an entity. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGiveEntity(PlayerInteractEntityEvent e) {
        if (holdsMenuItem(e)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGiveEntityAt(PlayerInteractAtEntityEvent e) {
        if (holdsMenuItem(e)) {
            e.setCancelled(true);
        }
    }

    /** True when the clicking hand holds the menu item and the entity could take it (not a trader or the menu NPC). */
    private boolean holdsMenuItem(PlayerInteractEntityEvent e) {
        Entity target = e.getRightClicked();
        String tag = plugin.settings().entityTag;
        if (target instanceof Merchant || (tag != null && !tag.isBlank() && target.getScoreboardTags().contains(tag))) {
            return false;
        }
        return isMenuItem(e.getPlayer().getInventory().getItem(e.getHand()));
    }

    private static boolean isBundle(ItemStack stack) {
        return stack != null && Tag.ITEMS_BUNDLES.isTagged(stack.getType());
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
