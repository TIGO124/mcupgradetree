package com.upgradetree.listener;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.data.PlayerData;
import com.upgradetree.effect.PlacedBlocks;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Earning upgrade points (only when the currency is POINTS): kills, playtime, mining, advancements, daily. */
public final class PointsListener implements Listener {

    private static final long AFK_MS = 5 * 60 * 1000L;

    private final UpgradeTreePlugin plugin;
    private final PlacedBlocks placed;
    private final Map<UUID, Map<UUID, Long>> playerKills = new HashMap<>();
    private final Map<UUID, Long> lastActive = new HashMap<>();
    private final Map<UUID, Integer> activeMinutes = new HashMap<>();

    public PointsListener(UpgradeTreePlugin plugin, PlacedBlocks placed) {
        this.plugin = plugin;
        this.placed = placed;
    }

    private boolean pointsMode() {
        return "POINTS".equals(plugin.settings().currencyType);
    }

    private void award(Player p, double amount, String reason) {
        if (amount <= 0) {
            return;
        }
        PlayerData d = plugin.data().get(p);
        d.addPoints(amount);
        p.sendActionBar(plugin.lang(p).plain("points-earned", "amount", Text.num(amount),
                "currency", Text.escape(plugin.currencyName(plugin.lang(p))), "reason", plugin.lang(p).raw("reason-" + reason)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        if (!pointsMode()) {
            return;
        }
        Settings s = plugin.settings();
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null || killer.equals(dead)) {
            return;
        }
        if (dead instanceof Player victim) {
            long now = System.currentTimeMillis();
            Map<UUID, Long> mine = playerKills.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>());
            Long last = mine.get(victim.getUniqueId());
            if (last != null && now - last < s.playerKillCooldownMs) {
                return;
            }
            mine.put(victim.getUniqueId(), now);
            award(killer, s.pointsPerPlayer, "kill");
            return;
        }
        if (s.ignoredSpawnReasons.contains(dead.getEntitySpawnReason())) {
            return;
        }
        award(killer, s.mobPoints.getOrDefault(dead.getType(), s.pointsPerMob), "kill");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMine(BlockBreakEvent e) {
        Settings s = plugin.settings();
        if (!pointsMode() || s.miningAmount <= 0 || e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        if (s.miningBlocks.contains(e.getBlock().getType()) && !placed.isPlaced(e.getBlock())) {
            award(e.getPlayer(), s.miningAmount, "mining");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        if (!pointsMode() || e.getAdvancement().getDisplay() == null) {
            return; // recipes and hidden technical advancements have no display
        }
        award(e.getPlayer(), plugin.settings().advancementPoints, "advancement");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.hasChangedOrientation() || e.hasChangedBlock()) {
            lastActive.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        lastActive.put(p.getUniqueId(), System.currentTimeMillis());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                claimDaily(p, false);
            }
        }, 60L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastActive.remove(id);
        activeMinutes.remove(id);
        playerKills.remove(id);
    }

    /** Gives the daily reward once per calendar day. Returns true if it was given now. */
    public boolean claimDaily(Player p, boolean tellIfClaimed) {
        Settings s = plugin.settings();
        if (!pointsMode() || s.dailyPoints <= 0) {
            return false;
        }
        PlayerData d = plugin.data().get(p);
        String today = LocalDate.now().toString();
        if (today.equals(d.lastDaily())) {
            return false;
        }
        d.lastDaily(today);
        d.addPoints(s.dailyPoints);
        plugin.data().save(d);
        p.sendMessage(plugin.lang(p).msg("daily-claimed", "amount", Text.num(s.dailyPoints),
                "currency", Text.escape(plugin.currencyName(plugin.lang(p)))));
        return true;
    }

    /** Called once a minute. */
    public void tickPlaytime() {
        Settings s = plugin.settings();
        if (!pointsMode() || s.playtimeMinutes <= 0 || s.playtimeAmount <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Long last = lastActive.get(p.getUniqueId());
            if (last == null || now - last > AFK_MS) {
                continue;
            }
            int m = activeMinutes.merge(p.getUniqueId(), 1, Integer::sum);
            if (m >= s.playtimeMinutes) {
                activeMinutes.put(p.getUniqueId(), 0);
                award(p, s.playtimeAmount, "playtime");
            }
        }
    }
}
