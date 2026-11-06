package com.upgradetree.effect;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.tree.UpgradeNode;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Applies the effects of purchased upgrades to players. */
public final class EffectManager implements Listener {

    /** Potion effects are refreshed with this duration (ticks). */
    private static final int POTION_DURATION = 600;

    private final UpgradeTreePlugin plugin;

    public EffectManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    private record Desired(double amount, AttributeModifier.Operation operation) {
    }

    /** Re-applies every effect of the player's current upgrades. */
    public void apply(Player p) {
        if (!p.isOnline()) {
            return;
        }
        PlayerData d = plugin.data().get(p);
        syncAttributes(p, d);
        refreshPotions(p, d);
    }

    private NamespacedKey modifierKey(UpgradeNode n, int index) {
        return new NamespacedKey(plugin, "node_" + n.key + "_" + index);
    }

    /**
     * Brings the player's attribute modifiers in line with their upgrades. Unchanged
     * modifiers are left untouched so that health is never lost on re-sync.
     */
    public void syncAttributes(Player p, PlayerData d) {
        Map<Attribute, Map<NamespacedKey, Desired>> desired = new HashMap<>();
        for (UpgradeNode n : plugin.tree().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl <= 0) {
                continue;
            }
            int idx = 0;
            for (NodeEffect e : n.effects) {
                if (e instanceof NodeEffect.AttributeBonus ab) {
                    desired.computeIfAbsent(ab.attribute, k -> new HashMap<>())
                            .put(modifierKey(n, idx), new Desired(ab.amount * lvl, ab.operation));
                }
                idx++;
            }
        }
        String ns = plugin.namespace();
        for (Attribute attr : RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)) {
            AttributeInstance inst = p.getAttribute(attr);
            if (inst == null) {
                continue;
            }
            Map<NamespacedKey, Desired> want = desired.getOrDefault(attr, Map.of());
            Set<NamespacedKey> present = new HashSet<>();
            for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) {
                NamespacedKey k = m.getKey();
                if (!k.getNamespace().equals(ns) || !k.getKey().startsWith("node_")) {
                    continue;
                }
                Desired w = want.get(k);
                if (w != null && Math.abs(w.amount() - m.getAmount()) < 1e-9 && w.operation() == m.getOperation()) {
                    present.add(k);
                } else {
                    inst.removeModifier(m);
                }
            }
            for (Map.Entry<NamespacedKey, Desired> e : want.entrySet()) {
                if (present.contains(e.getKey())) {
                    continue;
                }
                try {
                    inst.removeModifier(e.getKey());
                    inst.addModifier(new AttributeModifier(e.getKey(), e.getValue().amount(), e.getValue().operation(), EquipmentSlotGroup.ANY));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Özellik uygulanamadı (" + e.getKey() + "): " + ex.getMessage());
                }
            }
        }
    }

    private Map<PotionEffectType, Integer> desiredPotions(PlayerData d) {
        Map<PotionEffectType, Integer> want = new HashMap<>();
        for (UpgradeNode n : plugin.tree().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl <= 0) {
                continue;
            }
            for (NodeEffect e : n.effects) {
                if (e instanceof NodeEffect.PotionBonus pb) {
                    want.merge(pb.type, pb.amplifierAt(lvl), Math::max);
                }
            }
        }
        return want;
    }

    /** Tops up the permanent potion effects. Stronger effects from real potions are left alone. */
    public void refreshPotions(Player p, PlayerData d) {
        for (Map.Entry<PotionEffectType, Integer> e : desiredPotions(d).entrySet()) {
            PotionEffect cur = p.getPotionEffect(e.getKey());
            int amp = e.getValue();
            if (cur != null) {
                if (cur.getAmplifier() > amp) {
                    continue;
                }
                if (cur.getAmplifier() == amp && (cur.isInfinite() || cur.getDuration() > POTION_DURATION / 2)) {
                    continue;
                }
            }
            p.addPotionEffect(new PotionEffect(e.getKey(), POTION_DURATION, amp, true, false, true));
        }
    }

    /** Removes potion effects that this plugin gave (used after a reset). */
    public void clearManagedPotions(Player p) {
        Set<PotionEffectType> types = new HashSet<>();
        for (UpgradeNode n : plugin.tree().nodes()) {
            for (NodeEffect e : n.effects) {
                if (e instanceof NodeEffect.PotionBonus pb) {
                    types.add(pb.type);
                }
            }
        }
        for (PotionEffectType t : types) {
            PotionEffect cur = p.getPotionEffect(t);
            if (cur != null && cur.isAmbient() && !cur.hasParticles() && !cur.isInfinite() && cur.getDuration() <= POTION_DURATION) {
                p.removePotionEffect(t);
            }
        }
    }

    /** Runs the console commands of a node for a newly purchased level. */
    public void runCommands(Player p, UpgradeNode n, int level) {
        for (NodeEffect e : n.effects) {
            if (e instanceof NodeEffect.Commands c) {
                for (String cmd : c.commands) {
                    String s = cmd.replace("{player}", p.getName())
                            .replace("{uuid}", p.getUniqueId().toString())
                            .replace("{level}", String.valueOf(level))
                            .replace("{node}", n.id);
                    if (s.startsWith("/")) {
                        s = s.substring(1);
                    }
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Komut çalıştırılamadı '" + s + "': " + ex.getMessage());
                    }
                }
            }
        }
    }

    public double xpBoost(PlayerData d) {
        double total = 0;
        for (UpgradeNode n : plugin.tree().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl <= 0) {
                continue;
            }
            for (NodeEffect e : n.effects) {
                if (e instanceof NodeEffect.XpBoost x) {
                    total += x.amount * lvl;
                }
            }
        }
        return total;
    }

    public double doubleDropChance(PlayerData d) {
        double total = 0;
        for (UpgradeNode n : plugin.tree().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl <= 0) {
                continue;
            }
            for (NodeEffect e : n.effects) {
                if (e instanceof NodeEffect.DoubleDrop x) {
                    total += x.amount * lvl;
                }
            }
        }
        return Math.min(1.0, total);
    }

    /** Called every few seconds for all online players. */
    public void tickPotions() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isDead()) {
                refreshPotions(p, plugin.data().get(p));
            }
        }
    }

    // ------------------------------------------------------------------ listeners

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(p), 2L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.MILK_BUCKET) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    refreshPotions(p, plugin.data().get(p));
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
        double bonus = xpBoost(plugin.data().get(e.getPlayer()));
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockDrop(BlockDropItemEvent e) {
        Settings s = plugin.settings();
        Material type = e.getBlockState().getType();
        if (!s.doubleDropBlocks.contains(type)) {
            return;
        }
        Player p = e.getPlayer();
        double chance = doubleDropChance(plugin.data().get(p));
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        Location loc = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        boolean dropped = false;
        for (Item item : e.getItems()) {
            ItemStack stack = item.getItemStack();
            if (stack.getType() == type || stack.getType().isAir()) {
                continue; // silk touch: never duplicate the ore block itself
            }
            loc.getWorld().dropItemNaturally(loc, stack.clone());
            dropped = true;
        }
        if (dropped && s.particles) {
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 8, 0.3, 0.3, 0.3, 0);
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Settings s = plugin.settings();
        if (!"POINTS".equals(s.currencyType)) {
            return;
        }
        Player killer = e.getEntity().getKiller();
        if (killer == null || killer.equals(e.getEntity())) {
            return;
        }
        double amount = e.getEntity() instanceof Player ? s.pointsPerPlayer : s.pointsPerMob;
        if (amount <= 0) {
            return;
        }
        PlayerData d = plugin.data().get(killer);
        d.points(d.points() + amount);
        plugin.data().save(d);
    }
}
