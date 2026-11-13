package com.upgradetree.listener;

import com.upgradetree.UpgradeTreeBootstrap;
import com.upgradetree.UpgradeTreePlugin;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** The "Open" button of the Quick Actions dialog (Controls › Quick Actions key). */
public final class QuickActionListener implements Listener {

    private final UpgradeTreePlugin plugin;

    public QuickActionListener(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent e) {
        if (!UpgradeTreeBootstrap.OPEN_KEY.equals(e.getIdentifier())
                || !(e.getCommonConnection() instanceof PlayerGameConnection conn)) {
            return;
        }
        Player p = conn.getPlayer();
        // the event can arrive off the main thread; the dialog also needs a tick to close
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            if (!p.hasPermission("upgradetree.use")) {
                p.sendMessage(plugin.lang().msg("no-permission"));
                return;
            }
            plugin.menus().open(p);
        });
    }
}
