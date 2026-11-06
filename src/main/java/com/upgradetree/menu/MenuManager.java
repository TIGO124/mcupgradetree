package com.upgradetree.menu;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.gui.ChestMenu;
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

    /** True while a 3D panel or the chest menu is open. */
    public boolean isOpen(Player p) {
        return open.containsKey(p.getUniqueId()) || ChestMenu.isOpen(p);
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
        if (isOpen(p)) {
            return true;
        }
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
        if (trees.isEmpty()) {
            p.sendMessage(plugin.lang().msg("tree-not-found", "tree", "-"));
            return false;
        }
        UpgradeTree start = trees.get(0);
        String last = lastTree.get(p.getUniqueId());
        for (UpgradeTree t : trees) {
            if (t.id.equals(last)) {
                start = t;
            }
        }
        if (useChest(p)) {
            ChestMenu.open(plugin, p, trees, start);
            return true;
        }
        if (!canOpen(p)) {
            p.sendMessage(plugin.lang().msg("cannot-open"));
            return false;
        }
        DisplayMenu menu = new DisplayMenu(plugin, this, p, trees, start);
        try {
            menu.open();
        } catch (RuntimeException ex) {
            return false;
        }
        open.put(p.getUniqueId(), menu);
        return true;
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

    @SuppressWarnings("deprecation")
    private boolean canOpen(Player p) {
        if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        if (p.isInsideVehicle() || p.isGliding() || p.isSleeping() || p.isSwimming() || p.isInWater() || p.isSneaking()) {
            return false;
        }
        return p.isFlying() || p.isOnGround();
    }

    public void close(Player p, boolean instant) {
        DisplayMenu menu = open.remove(p.getUniqueId());
        if (menu == null) {
            if (ChestMenu.isOpen(p)) {
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
            if (ChestMenu.isOpen(p)) {
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
                plugin.getLogger().warning("Menü hatası (" + menu.player().getName() + "): " + ex);
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
        return menu == null ? 0 : menu.entityCount();
    }

    public String target(Player p) {
        DisplayMenu menu = open.get(p.getUniqueId());
        return menu == null ? "-" : menu.target();
    }

    /** World position of a shown node, or null. */
    public Location nodeLocation(Player p, String nodeId) {
        DisplayMenu menu = open.get(p.getUniqueId());
        if (menu == null) {
            return null;
        }
        double[] uv = menu.nodePosition(nodeId);
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
