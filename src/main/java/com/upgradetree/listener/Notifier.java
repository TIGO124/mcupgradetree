package com.upgradetree.listener;

import com.upgradetree.UpgradeTreePlugin;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tells players when a new upgrade becomes affordable. */
public final class Notifier implements Listener {

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, Integer> lastCount = new HashMap<>();
    private final Map<UUID, Long> lastSent = new HashMap<>();

    public Notifier(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastCount.remove(e.getPlayer().getUniqueId());
        lastSent.remove(e.getPlayer().getUniqueId());
    }

    /** Called every few seconds. */
    public void tick() {
        if (!plugin.settings().notify) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.menus().isOpen(p) || !p.hasPermission("upgradetree.use") || !plugin.effects().enabledFor(p)) {
                continue;
            }
            int count = plugin.service().affordable(p);
            Integer before = lastCount.put(p.getUniqueId(), count);
            if (before == null || count <= before) {
                continue;
            }
            Long sent = lastSent.get(p.getUniqueId());
            if (sent != null && now - sent < plugin.settings().notifyCooldownMs) {
                continue;
            }
            lastSent.put(p.getUniqueId(), now);
            p.sendActionBar(plugin.lang().plain("notify-available"));
            p.playSound(p.getLocation(), "minecraft:block.note_block.chime", SoundCategory.MASTER, 0.6f, 1.8f);
        }
    }
}
