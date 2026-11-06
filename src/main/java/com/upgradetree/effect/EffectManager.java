package com.upgradetree.effect;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.tree.UpgradeNode;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/** Works out which effects a player has and keeps attributes and potions in sync with them. */
public final class EffectManager {

    /** Potion effects are refreshed with this duration (ticks). */
    private static final int POTION_DURATION = 600;

    private final UpgradeTreePlugin plugin;

    public EffectManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    /** An effect together with the level it is active at; node is null for prestige effects. */
    public record Active(NodeEffect effect, int level, UpgradeNode node, int index) {
    }

    private record Desired(double amount, AttributeModifier.Operation operation) {
    }

    /** Every effect of the player's upgrades and prestige. */
    public List<Active> active(PlayerData d) {
        List<Active> out = new ArrayList<>();
        for (UpgradeNode n : plugin.trees().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl <= 0) {
                continue;
            }
            for (int i = 0; i < n.effects.size(); i++) {
                out.add(new Active(n.effects.get(i), lvl, n, i));
            }
        }
        int prestige = d.prestige();
        if (prestige > 0) {
            List<NodeEffect> pe = plugin.settings().prestigeEffects;
            for (int i = 0; i < pe.size(); i++) {
                out.add(new Active(pe.get(i), prestige, null, i));
            }
        }
        return out;
    }

    /** False in disabled worlds and in cleanup mode: upgrades then do nothing. */
    public boolean enabledFor(Player p) {
        Settings s = plugin.settings();
        return !s.cleanupMode && !s.worldDisabled(p.getWorld().getName());
    }

    /** Active effects of one type for an online player (empty when disabled). */
    public <T extends NodeEffect> List<Typed<T>> of(Player p, Class<T> type) {
        List<Typed<T>> out = new ArrayList<>();
        if (!enabledFor(p)) {
            return out;
        }
        for (Active a : active(plugin.data().get(p))) {
            if (type.isInstance(a.effect())) {
                out.add(new Typed<>(type.cast(a.effect()), a.level(), a.node()));
            }
        }
        return out;
    }

    public record Typed<T extends NodeEffect>(T effect, int level, UpgradeNode node) {
    }

    /** Sum of amount * level over the player's effects of one type. */
    public <T extends NodeEffect> double sum(Player p, Class<T> type, ToDoubleFunction<T> amount) {
        double total = 0;
        for (Typed<T> t : of(p, type)) {
            total += amount.applyAsDouble(t.effect()) * t.level();
        }
        return total;
    }

    /** Re-applies every effect of the player's current upgrades. */
    public void apply(Player p) {
        if (!p.isOnline()) {
            return;
        }
        PlayerData d = plugin.data().get(p);
        if (!enabledFor(p)) {
            clearAll(p);
            return;
        }
        syncAttributes(p, d);
        refreshPotions(p, d);
    }

    private NamespacedKey modifierKey(Active a) {
        String k = a.node() == null ? "prestige_" + a.index() : "node_" + a.node().key + "_" + a.index();
        return new NamespacedKey(plugin, k);
    }

    private boolean ours(NamespacedKey k) {
        return k.getNamespace().equals(plugin.namespace())
                && (k.getKey().startsWith("node_") || k.getKey().startsWith("prestige_"));
    }

    /**
     * Brings the player's attribute modifiers in line with their upgrades. Unchanged
     * modifiers are left untouched so that health is never lost on re-sync.
     */
    public void syncAttributes(Player p, PlayerData d) {
        Map<Attribute, Map<NamespacedKey, Desired>> desired = new HashMap<>();
        for (Active a : active(d)) {
            if (a.effect() instanceof NodeEffect.AttributeBonus ab) {
                desired.computeIfAbsent(ab.attribute(), k -> new HashMap<>())
                        .put(modifierKey(a), new Desired(ab.amount() * a.level(), ab.operation()));
            }
        }
        for (Attribute attr : RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)) {
            AttributeInstance inst = p.getAttribute(attr);
            if (inst == null) {
                continue;
            }
            Map<NamespacedKey, Desired> want = desired.getOrDefault(attr, Map.of());
            Set<NamespacedKey> present = new HashSet<>();
            for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) {
                NamespacedKey k = m.getKey();
                if (!ours(k)) {
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
        for (Active a : active(d)) {
            if (a.effect() instanceof NodeEffect.PotionBonus pb) {
                want.merge(pb.type(), pb.amplifierAt(a.level()), Math::max);
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

    /** Removes potion effects that look like ours (used after a reset). */
    public void clearManagedPotions(Player p) {
        Set<PotionEffectType> types = new HashSet<>();
        for (UpgradeNode n : plugin.trees().nodes()) {
            for (NodeEffect e : n.effects) {
                if (e instanceof NodeEffect.PotionBonus pb) {
                    types.add(pb.type());
                }
            }
        }
        for (NodeEffect e : plugin.settings().prestigeEffects) {
            if (e instanceof NodeEffect.PotionBonus pb) {
                types.add(pb.type());
            }
        }
        for (PotionEffectType t : types) {
            PotionEffect cur = p.getPotionEffect(t);
            if (cur != null && cur.isAmbient() && !cur.hasParticles() && !cur.isInfinite() && cur.getDuration() <= POTION_DURATION) {
                p.removePotionEffect(t);
            }
        }
    }

    /** Removes every attribute modifier and potion this plugin gave the player. Returns true if anything changed. */
    public boolean clearAll(Player p) {
        boolean changed = false;
        for (Attribute attr : RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)) {
            AttributeInstance inst = p.getAttribute(attr);
            if (inst == null) {
                continue;
            }
            for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) {
                if (ours(m.getKey())) {
                    inst.removeModifier(m);
                    changed = true;
                }
            }
        }
        clearManagedPotions(p);
        return changed;
    }

    /** Runs the console commands of a node for a newly purchased level. */
    public void runCommands(Player p, UpgradeNode n, int level) {
        for (NodeEffect e : n.effects) {
            if (e instanceof NodeEffect.Commands c) {
                for (String cmd : c.commands()) {
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

    /** Called every few seconds for all online players. */
    public void tickPotions() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isDead() && enabledFor(p)) {
                refreshPotions(p, plugin.data().get(p));
            }
        }
    }
}
