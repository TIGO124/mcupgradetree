package com.upgradetree.effect;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.api.event.AbilityUseEvent;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Active abilities. Each upgrade with an ABILITY effect is cast with its trigger:
 * Shift + F (SNEAK_SWAP), double sneak (DOUBLE_SNEAK) or sneak + jump (SNEAK_JUMP).
 */
public final class AbilityManager implements Listener {

    private static final long DOUBLE_SNEAK_MS = 350;

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> lastSneak = new HashMap<>();
    private final Map<UUID, Long> noFall = new HashMap<>();

    public AbilityManager(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking() && !plugin.menus().capturesInput(p) && cast(p, NodeEffect.Trigger.SNEAK_SWAP)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }
        Player p = e.getPlayer();
        if (plugin.menus().capturesInput(p)) {
            lastSneak.remove(p.getUniqueId());
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastSneak.put(p.getUniqueId(), now);
        if (last != null && now - last <= DOUBLE_SNEAK_MS) {
            lastSneak.remove(p.getUniqueId());
            cast(p, NodeEffect.Trigger.DOUBLE_SNEAK);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking() && !plugin.menus().capturesInput(p)) {
            cast(p, NodeEffect.Trigger.SNEAK_JUMP);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && e.getEntity() instanceof Player p) {
            Long until = noFall.remove(p.getUniqueId());
            if (until != null && until >= System.currentTimeMillis()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        cooldowns.remove(id);
        lastSneak.remove(id);
        noFall.remove(id);
    }

    /** Seconds left on the cooldown of an ability node (0 = ready). */
    public int cooldownLeft(Player p, String nodeId) {
        Long ready = cooldowns.getOrDefault(p.getUniqueId(), Map.of()).get(nodeId);
        long left = ready == null ? 0 : ready - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
    }

    /** Casts the first ability bound to the trigger. Returns true if the player has one. */
    public boolean cast(Player p, NodeEffect.Trigger trigger) {
        List<EffectManager.Typed<NodeEffect.Ability>> list = plugin.effects().of(p, NodeEffect.Ability.class);
        EffectManager.Typed<NodeEffect.Ability> chosen = null;
        for (EffectManager.Typed<NodeEffect.Ability> t : list) {
            if (t.effect().trigger() == trigger) {
                chosen = t;
                break;
            }
        }
        if (chosen == null) {
            return false;
        }
        NodeEffect.Ability ability = chosen.effect();
        String id = chosen.node() == null ? "prestige" : chosen.node().id;
        String name = plugin.lang(p).raw("ability-" + ability.type().name());
        long now = System.currentTimeMillis();
        Map<String, Long> cd = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        Long ready = cd.get(id);
        if (ready != null && ready > now) {
            p.sendActionBar(plugin.lang(p).plain("ability-cooldown", "ability", Text.escape(name),
                    "seconds", String.valueOf((int) Math.ceil((ready - now) / 1000.0))));
            return true;
        }
        AbilityUseEvent event = new AbilityUseEvent(p, chosen.node(), ability, chosen.level());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        perform(p, ability.type(), ability.powerAt(chosen.level()));
        cd.put(id, now + (long) (ability.cooldownAt(chosen.level()) * 1000));
        p.sendActionBar(plugin.lang(p).plain("ability-used", "ability", Text.escape(name)));
        return true;
    }

    private void sound(Player p, String key, float volume, float pitch) {
        p.getWorld().playSound(p.getLocation(), key, SoundCategory.PLAYERS, volume, pitch);
    }

    private void perform(Player p, NodeEffect.AbilityType type, double power) {
        Location loc = p.getLocation();
        switch (type) {
            case DASH -> {
                Vector dir = loc.getDirection().setY(0);
                if (dir.lengthSquared() < 1e-6) {
                    dir = new Vector(0, 0, 1);
                }
                p.setVelocity(dir.normalize().multiply(1.1 + 0.35 * power).setY(0.25));
                noFall.put(p.getUniqueId(), System.currentTimeMillis() + 3000);
                p.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.2, 0), 16, 0.3, 0.1, 0.3, 0.05);
                sound(p, "minecraft:item.trident.riptide_1", 0.7f, 1.4f);
            }
            case LEAP -> {
                Vector dir = loc.getDirection().setY(0);
                if (dir.lengthSquared() > 1e-6) {
                    dir.normalize().multiply(0.5 + 0.15 * power);
                }
                p.setVelocity(dir.setY(0.75 + 0.15 * power));
                noFall.put(p.getUniqueId(), System.currentTimeMillis() + 6000);
                p.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.1, 0), 20, 0.4, 0.05, 0.4, 0.02);
                sound(p, "minecraft:entity.goat.long_jump", 1.0f, 1.0f);
            }
            case HEAL -> {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                double cap = max == null ? 20 : max.getValue();
                p.setHealth(Math.min(cap, p.getHealth() + 4.0 * power));
                p.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 2.0, 0), 6, 0.4, 0.3, 0.4, 0);
                sound(p, "minecraft:block.amethyst_block.chime", 1.0f, 1.4f);
            }
            case SHIELD -> {
                int amp = Math.max(0, (int) Math.round(power) - 1);
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, amp, false, true, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, false, true, true));
                p.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 24, 0.5, 0.7, 0.5, 0.02);
                sound(p, "minecraft:item.armor.equip_diamond", 1.0f, 0.8f);
            }
            case SPEED -> {
                int amp = Math.max(0, Math.min(4, (int) Math.round(power)));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, amp, false, true, true));
                p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);
                sound(p, "minecraft:entity.firework_rocket.launch", 0.8f, 1.3f);
            }
            case NOVA -> {
                double radius = 4.0 + power;
                for (Entity ent : p.getNearbyEntities(radius, radius / 2, radius)) {
                    if (ent instanceof LivingEntity le && ent instanceof Enemy && !le.isDead()) {
                        le.damage(2.5 * power, p);
                        Vector push = le.getLocation().toVector().subtract(loc.toVector()).setY(0);
                        if (push.lengthSquared() < 1e-6) {
                            push = new Vector(0, 0, 1);
                        }
                        le.setVelocity(push.normalize().multiply(0.8 + 0.2 * power).setY(0.35));
                    }
                }
                for (int i = 0; i < 24; i++) {
                    double a = Math.PI * 2 * i / 24;
                    p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                            loc.clone().add(Math.cos(a) * radius * 0.6, 0.6, Math.sin(a) * radius * 0.6), 1, 0, 0, 0, 0);
                }
                sound(p, "minecraft:entity.generic.explode", 0.6f, 1.5f);
            }
        }
    }
}
