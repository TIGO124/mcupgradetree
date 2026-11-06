package com.upgradetree.effect;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.tree.NodeEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** XP boost, death effects, mob drops, hunger saving and keeping effects applied. */
public final class LifeListener implements Listener {

    private final UpgradeTreePlugin plugin;

    public LifeListener(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.effects().apply(p), 2L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        plugin.effects().apply(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.MILK_BUCKET) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    plugin.effects().apply(p);
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onExp(PlayerExpChangeEvent e) {
        int amount = e.getAmount();
        if (amount <= 0) {
            return;
        }
        double bonus = plugin.effects().sum(e.getPlayer(), NodeEffect.XpBoost.class, NodeEffect.XpBoost::amount);
        if (bonus <= 0) {
            return;
        }
        double extra = amount * bonus;
        int add = (int) Math.floor(extra);
        if (ThreadLocalRandom.current().nextDouble() < extra - add) {
            add++;
        }
        e.setAmount(amount + add);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        if (e.getKeepInventory()) {
            return;
        }
        double keepInv = plugin.effects().sum(p, NodeEffect.KeepInventory.class, NodeEffect.KeepInventory::amount);
        if (keepInv > 0 && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, keepInv)) {
            e.setKeepInventory(true);
            e.getDrops().clear();
        }
        double keepXp = Math.min(1.0, plugin.effects().sum(p, NodeEffect.KeepXp.class, NodeEffect.KeepXp::amount));
        if (keepXp > 0 && !e.getKeepLevel()) {
            int level = (int) Math.floor(p.getLevel() * keepXp);
            e.setNewLevel(level);
            e.setNewExp(keepXp >= 1.0 ? (int) Math.floor(p.getExp() * p.getExpToLevel()) : 0);
            e.setDroppedExp((int) Math.floor(e.getDroppedExp() * (1.0 - keepXp)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null || dead instanceof Player || e.getDrops().isEmpty()) {
            return;
        }
        if (plugin.settings().ignoredSpawnReasons.contains(dead.getEntitySpawnReason())) {
            return;
        }
        double chance = plugin.effects().sum(killer, NodeEffect.MobDrops.class, NodeEffect.MobDrops::amount);
        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, chance)) {
            List<ItemStack> extra = new ArrayList<>();
            for (ItemStack drop : e.getDrops()) {
                if (drop != null && !drop.getType().isAir()) {
                    extra.add(drop.clone());
                }
            }
            e.getDrops().addAll(extra);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getFoodLevel() >= p.getFoodLevel()) {
            return;
        }
        double chance = plugin.effects().sum(p, NodeEffect.HungerSave.class, NodeEffect.HungerSave::amount);
        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, chance)) {
            e.setCancelled(true);
        }
    }
}
