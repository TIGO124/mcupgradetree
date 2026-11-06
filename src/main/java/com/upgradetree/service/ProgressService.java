package com.upgradetree.service;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.api.event.PrestigeEvent;
import com.upgradetree.api.event.UpgradeResetEvent;
import com.upgradetree.config.Settings;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;

/** Respec (reset with refund) and prestige (reset for permanent bonuses). */
public final class ProgressService {

    public enum RespecResult { DONE, DISABLED, COOLDOWN, NOTHING, CANCELLED }

    public enum PrestigeResult { DONE, DISABLED, MAX, NOT_READY, CANCELLED }

    private final UpgradeTreePlugin plugin;

    public ProgressService(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ respec

    /** Currency returned by a respec right now. */
    public double refund(PlayerData d) {
        double total = 0;
        for (Map.Entry<String, Integer> e : d.levels().entrySet()) {
            double spent = d.spent(e.getKey());
            if (spent <= 0) {
                // data from before 2.0 has no spending record: use the base prices
                UpgradeNode n = plugin.trees().node(e.getKey());
                if (n != null) {
                    for (int i = 0; i < Math.min(e.getValue(), n.maxLevel); i++) {
                        spent += n.costFor(i);
                    }
                }
            }
            total += spent;
        }
        return refundOf(total, plugin.settings().respecRefund);
    }

    /** Pure refund formula, rounded down to 2 decimals. */
    public static double refundOf(double spent, double fraction) {
        return Math.floor(Math.max(0, spent) * Math.max(0, Math.min(1, fraction)) * 100.0 + 1e-6) / 100.0;
    }

    public long respecCooldownLeft(PlayerData d) {
        long left = d.lastRespec() + plugin.settings().respecCooldownMs - System.currentTimeMillis();
        return Math.max(0, left);
    }

    public RespecResult respec(Player p) {
        Settings s = plugin.settings();
        if (!s.respecEnabled) {
            return RespecResult.DISABLED;
        }
        PlayerData d = plugin.data().get(p);
        if (d.levels().isEmpty()) {
            return RespecResult.NOTHING;
        }
        if (respecCooldownLeft(d) > 0) {
            return RespecResult.COOLDOWN;
        }
        double refund = refund(d);
        UpgradeResetEvent event = new UpgradeResetEvent(p, UpgradeResetEvent.Cause.RESPEC, refund);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return RespecResult.CANCELLED;
        }
        d.clearLevels();
        d.lastRespec(System.currentTimeMillis());
        if (refund > 0 && plugin.currency().available()) {
            plugin.currency().deposit(p, refund);
        }
        plugin.data().save(d);
        plugin.effects().apply(p);
        plugin.effects().clearManagedPotions(p);
        plugin.effects().apply(p);
        return RespecResult.DONE;
    }

    /** Admin reset without refund (works for offline data too). */
    public void adminReset(PlayerData d, Player online) {
        Bukkit.getPluginManager().callEvent(new UpgradeResetEvent(
                online != null ? online : Bukkit.getOfflinePlayer(d.uuid), UpgradeResetEvent.Cause.ADMIN, 0));
        d.clearLevels();
        plugin.data().save(d);
        if (online != null) {
            plugin.effects().apply(online);
            plugin.effects().clearManagedPotions(online);
            plugin.effects().apply(online);
        }
    }

    // ------------------------------------------------------------------ prestige

    public boolean prestigeReady(PlayerData d, Player p) {
        Settings s = plugin.settings();
        return s.prestigeEnabled && d.prestige() < s.prestigeMax
                && plugin.service().progress(d, p) * 100.0 + 1e-6 >= s.prestigeRequiredPercent
                && !plugin.trees().nodes().isEmpty();
    }

    public PrestigeResult prestige(Player p) {
        Settings s = plugin.settings();
        if (!s.prestigeEnabled) {
            return PrestigeResult.DISABLED;
        }
        PlayerData d = plugin.data().get(p);
        if (d.prestige() >= s.prestigeMax) {
            return PrestigeResult.MAX;
        }
        if (!prestigeReady(d, p)) {
            return PrestigeResult.NOT_READY;
        }
        int next = d.prestige() + 1;
        PrestigeEvent event = new PrestigeEvent(p, next);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return PrestigeResult.CANCELLED;
        }
        Bukkit.getPluginManager().callEvent(new UpgradeResetEvent(p, UpgradeResetEvent.Cause.PRESTIGE, 0));
        d.clearLevels();
        d.prestige(next);
        if (s.prestigeResetPoints) {
            d.points(0);
        }
        plugin.data().save(d);
        plugin.effects().apply(p);
        plugin.effects().clearManagedPotions(p);
        plugin.effects().apply(p);
        for (String cmd : s.prestigeCommands) {
            String c = cmd.replace("{player}", p.getName()).replace("{uuid}", p.getUniqueId().toString())
                    .replace("{prestige}", String.valueOf(next));
            if (c.startsWith("/")) {
                c = c.substring(1);
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
            } catch (Exception ex) {
                plugin.getLogger().warning("Prestij komutu çalıştırılamadı '" + c + "': " + ex.getMessage());
            }
        }
        Bukkit.broadcast(plugin.lang().msg("prestige-broadcast", "player", Text.escape(p.getName()),
                "level", String.valueOf(next)));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        plugin.leaderboard().touch(d);
        return PrestigeResult.DONE;
    }
}
