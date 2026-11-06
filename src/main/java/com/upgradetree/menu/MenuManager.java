package com.upgradetree.menu;

import com.upgradetree.UpgradeTreePlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps track of open menus and ticks them. */
public final class MenuManager {

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, TreeMenu> open = new HashMap<>();
    private final List<TreeMenu> closing = new ArrayList<>();

    public MenuManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOpen(Player p) {
        return open.containsKey(p.getUniqueId());
    }

    TreeMenu get(Player p) {
        return open.get(p.getUniqueId());
    }

    public void toggle(Player p) {
        if (isOpen(p)) {
            close(p, false);
        } else {
            open(p);
        }
    }

    public boolean open(Player p) {
        if (isOpen(p)) {
            return true;
        }
        if (!canOpen(p)) {
            p.sendMessage(plugin.settings().msg("cannot-open"));
            return false;
        }
        TreeMenu menu = new TreeMenu(plugin, this, p);
        try {
            menu.open();
        } catch (RuntimeException ex) {
            return false;
        }
        open.put(p.getUniqueId(), menu);
        return true;
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
        TreeMenu menu = open.remove(p.getUniqueId());
        if (menu == null) {
            return;
        }
        menu.close(instant);
        if (!menu.isDone()) {
            closing.add(menu);
        }
    }

    public void closeAll(boolean instant) {
        for (UUID id : new ArrayList<>(open.keySet())) {
            TreeMenu menu = open.remove(id);
            if (menu != null) {
                menu.close(instant);
                if (!menu.isDone()) {
                    closing.add(menu);
                }
            }
        }
        if (instant) {
            for (TreeMenu menu : closing) {
                menu.forceRemove();
            }
            closing.clear();
        }
    }

    /** Undoes a freeze that survived a crash or reload. */
    public void recover(Player p) {
        Freeze.release(plugin, p);
    }

    public void tick() {
        for (TreeMenu menu : new ArrayList<>(open.values())) {
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
}
