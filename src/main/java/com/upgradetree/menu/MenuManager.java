package com.upgradetree.menu;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.gui.ChestMenu;
import com.upgradetree.gui.ChestShopMenu;
import com.upgradetree.tree.UpgradeTree;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Opens the right menu (3D panel or chest), keeps track of open 3D menus and ticks them. */
public final class MenuManager {

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, DisplayMenu> open = new HashMap<>();
    private final List<DisplayMenu> closing = new ArrayList<>();
    private final Map<UUID, String> lastTree = new HashMap<>();

    public MenuManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    /** True while a 3D panel or one of the chest menus (tree, chests shop) is open. */
    public boolean isOpen(Player p) {
        return open.containsKey(p.getUniqueId()) || chestGui(p);
    }

    private static boolean chestGui(Player p) {
        return ChestMenu.isOpen(p) || ChestShopMenu.isOpen(p);
    }

    DisplayMenu get(Player p) {
        return open.get(p.getUniqueId());
    }

    /** True only while the 3D panel is open. */
    public boolean displayOpen(Player p) {
        return open.containsKey(p.getUniqueId());
    }

    public void toggle(Player p) {
        if (isOpen(p)) {
            close(p, false);
        } else {
            open(p);
        }
    }

    public List<UpgradeTree> treesFor(Player p) {
        List<UpgradeTree> list = new ArrayList<>();
        for (UpgradeTree t : plugin.trees().trees()) {
            if (t.permission == null || p.hasPermission(t.permission)) {
                list.add(t);
            }
        }
        return list;
    }

    public void remember(Player p, UpgradeTree t) {
        if (t != null) {
            lastTree.put(p.getUniqueId(), t.id);
        }
    }

    public boolean open(Player p) {
        if (open.containsKey(p.getUniqueId()) || ChestMenu.isOpen(p)) {
            return true;
        }
        return open(p, false);
    }

    /** /tigo chests: the Chests section (3D tab, or the chests shop menu for chest-menu users). */
    public boolean openChests(Player p) {
        if (!plugin.chestTypes().active()) {
            p.sendMessage(plugin.lang().msg("chests-disabled"));
            return false;
        }
        DisplayMenu shown = open.get(p.getUniqueId());
        if (shown != null) {
            shown.showTab(ChestLayer.TAB, false);
            return true;
        }
        if (ChestShopMenu.isOpen(p)) {
            return true;
        }
        return open(p, true);
    }

    private boolean open(Player p, boolean chests) {
        Settings s = plugin.settings();
        if (s.worldDisabled(p.getWorld().getName())) {
            p.sendMessage(plugin.lang().msg("disabled-world"));
            return false;
        }
        int combat = plugin.combat().secondsLeft(p);
        if (combat > 0) {
            p.sendMessage(plugin.lang().msg("cannot-open-combat", "seconds", String.valueOf(combat)));
            return false;
        }
        List<UpgradeTree> trees = treesFor(p);
        if (trees.isEmpty() && !(chests && plugin.chestTypes().active())) {
            p.sendMessage(plugin.lang().msg("tree-not-found", "tree", "-"));
            return false;
        }
        UpgradeTree start = trees.isEmpty() ? null : trees.get(0);
        String last = lastTree.get(p.getUniqueId());
        for (UpgradeTree t : trees) {
            if (t.id.equals(last)) {
                start = t;
            }
        }
        if (useChest(p)) {
            if (chests) {
                ChestShopMenu.open(plugin, p);
            } else {
                ChestMenu.open(plugin, p, trees, start);
            }
            return true;
        }
        if (!canOpen(p)) {
            p.sendMessage(plugin.lang().msg("cannot-open"));
            return false;
        }
        DisplayMenu menu = new DisplayMenu(plugin, this, p, trees, start, null, chests);
        if (!start(menu)) {
            p.sendMessage(plugin.lang().msg("cannot-open"));
            return false;
        }
        open.put(p.getUniqueId(), menu);
        return true;
    }

    /**
     * Opens a 3D menu. A failed build is logged and cleaned up by the menu itself; this also undoes
     * the freeze/aim set up before the build, so the player is never left stuck without feedback.
     */
    private boolean start(DisplayMenu menu) {
        try {
            menu.open();
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Could not open the menu (" + menu.player().getName() + "): " + ex);
            try {
                menu.forceRemove();
            } catch (RuntimeException ignored) {
                // nothing more to clean up
            }
            return false;
        }
    }

    private boolean useChest(Player p) {
        String pref = plugin.data().get(p).menuMode();
        if ("CHEST".equals(pref)) {
            return true;
        }
        if ("PANEL".equals(pref)) {
            return false;
        }
        return switch (plugin.settings().menuMode) {
            case CHEST -> true;
            case DISPLAY -> false;
            case AUTO -> plugin.bedrock().isBedrock(p);
        };
    }

    /** The panel stays in the world and the player keeps moving, so only a few states block it. */
    private boolean canOpen(Player p) {
        if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.isSleeping() || p.isGliding()) {
            return false;
        }
        if (plugin.settings().lockPlayer) {
            return !p.isInsideVehicle() && !p.isSwimming() && !p.isInWater() && !p.isSneaking() && (p.isFlying() || p.isOnGround());
        }
        return true;
    }

    /** True when the player's clicks and keys go to a menu (chest open, or crosshair on the 3D panel). */
    public boolean capturesInput(Player p) {
        DisplayMenu menu = open.get(p.getUniqueId());
        return menu != null ? menu.captures() : ChestMenu.isOpen(p);
    }

    /**
     * Opens the 3D tree of another player (or yourself) in the admin edit mode. Operators only.
     * {@code offline} is the loaded data of an offline player (ignored while they are online).
     */
    public boolean openAdmin(Player viewer, UUID target, String name, com.upgradetree.data.PlayerData offline) {
        if (!viewer.isOp()) {
            viewer.sendMessage(plugin.lang().msg("admin-only-op"));
            return false;
        }
        close(viewer, true);
        if (ChestMenu.isOpen(viewer) || viewer.getOpenInventory().getTopInventory().getHolder() != null) {
            viewer.closeInventory();
        }
        List<UpgradeTree> trees = plugin.trees().trees();
        if (trees.isEmpty() || !canOpen(viewer)) {
            viewer.sendMessage(plugin.lang().msg("cannot-open"));
            return false;
        }
        DisplayMenu menu = new DisplayMenu(plugin, this, viewer, trees, trees.get(0),
                new DisplayMenu.AdminTarget(target, name, offline), false);
        if (!start(menu)) {
            viewer.sendMessage(plugin.lang().msg("cannot-open"));
            return false;
        }
        open.put(viewer.getUniqueId(), menu);
        return true;
    }

    public void close(Player p, boolean instant) {
        DisplayMenu menu = open.remove(p.getUniqueId());
        if (menu == null) {
            if (chestGui(p)) {
                p.closeInventory();
            }
            return;
        }
        remember(p, menu.tree());
        menu.close(instant);
        if (!menu.isDone()) {
            closing.add(menu);
        }
    }

    public void closeAll(boolean instant) {
        for (UUID id : new ArrayList<>(open.keySet())) {
            DisplayMenu menu = open.remove(id);
            if (menu != null) {
                menu.close(instant);
                if (!menu.isDone()) {
                    closing.add(menu);
                }
            }
        }
        if (instant) {
            for (DisplayMenu menu : closing) {
                menu.forceRemove();
            }
            closing.clear();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (chestGui(p)) {
                p.closeInventory();
            }
        }
    }

    /** Undoes a freeze that survived a crash or reload. */
    public void recover(Player p) {
        Freeze.release(plugin, p);
    }

    /** Something changed outside the menu (admin command): close it so it never shows stale data. */
    public void refresh(Player p) {
        if (open.containsKey(p.getUniqueId())) {
            close(p, true);
        } else if (p.getOpenInventory().getTopInventory().getHolder() instanceof ChestMenu chest) {
            chest.render();
        }
    }

    public void tick() {
        for (DisplayMenu menu : new ArrayList<>(open.values())) {
            try {
                menu.tick();
            } catch (Exception ex) {
                plugin.getLogger().warning("Menu error (" + menu.player().getName() + "): " + ex);
                close(menu.player(), true);
            }
        }
        if (!closing.isEmpty()) {
            closing.removeIf(menu -> {
                try {
                    menu.tick();
                } catch (Exception ex) {
                    menu.forceRemove();
                }
                return menu.isDone();
            });
        }
    }

    // ------------------------------------------------------------------ debug helpers

    /** Saves a PNG of the player's open 3D menu. Returns false if none is open. */
    public boolean snapshot(Player p, File out, File textures) throws IOException {
        DisplayMenu menu = open.get(p.getUniqueId());
        if (menu == null) {
            return false;
        }
        MenuSnapshot.save(menu, out, textures);
        return true;
    }

    public int entityCount(Player p) {
        DisplayMenu menu = open.get(p.getUniqueId());
        return menu == null ? 0 : MenuDebug.entityCount(menu);
    }

    public String target(Player p) {
        DisplayMenu menu = open.get(p.getUniqueId());
        return menu == null ? "-" : MenuDebug.target(menu);
    }

    /** World position of a shown node, tab ("tab:id") or button, or null. */
    public Location nodeLocation(Player p, String target) {
        DisplayMenu menu = open.get(p.getUniqueId());
        if (menu == null) {
            return null;
        }
        double[] uv = MenuDebug.position(menu, target);
        return uv == null ? null : menu.frame().point(uv[0], uv[1], 0);
    }

    /** Turns the player's view to a node (works for real clients). */
    public boolean lookAt(Player p, String nodeId) {
        Location loc = nodeLocation(p, nodeId);
        if (loc == null) {
            return false;
        }
        p.lookAt(loc, LookAnchor.EYES);
        return true;
    }
}
