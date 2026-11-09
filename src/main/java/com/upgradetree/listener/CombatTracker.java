package com.upgradetree.listener;

import com.upgradetree.UpgradeTreePlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Remembers when players last fought, so the menu cannot be opened right after a hit. */
public final class CombatTracker implements Listener {

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, Long> lastCombat = new HashMap<>();

    public CombatTracker(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        long now = System.currentTimeMillis();
        if (e.getEntity() instanceof Player victim) {
            lastCombat.put(victim.getUniqueId(), now);
        }
        Entity d = e.getDamager();
        if (d instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            d = shooter;
        }
        if (d instanceof Player attacker && e.getEntity() instanceof Player) {
            lastCombat.put(attacker.getUniqueId(), now);
        }
    }

    /** A combat tag survives a rejoin; only expired tags are dropped. */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        long cutoff = System.currentTimeMillis() - plugin.settings().combatCooldownMs;
        lastCombat.values().removeIf(at -> at <= cutoff);
    }

    /** Seconds the player still has to wait (0 = free to open the menu). */
    public int secondsLeft(Player p) {
        Long last = lastCombat.get(p.getUniqueId());
        if (last == null) {
            return 0;
        }
        long left = last + plugin.settings().combatCooldownMs - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
    }
}
