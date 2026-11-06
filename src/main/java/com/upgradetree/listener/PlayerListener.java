package com.upgradetree.listener;

import com.upgradetree.UpgradeTreePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Loads/saves player data and keeps upgrade effects applied across sessions. */
public final class PlayerListener implements Listener {

    private final UpgradeTreePlugin plugin;

    public PlayerListener(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    /** Reads the data off the main thread before the player joins. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.data().preload(e.getUniqueId(), e.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.menus().recover(p);
        plugin.data().get(p);
        plugin.effects().apply(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                plugin.effects().apply(p);
                plugin.leaderboard().touch(plugin.data().get(p));
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.menus().close(p, true);
        plugin.leaderboard().touch(plugin.data().get(p));
        plugin.data().unload(p.getUniqueId());
    }
}
