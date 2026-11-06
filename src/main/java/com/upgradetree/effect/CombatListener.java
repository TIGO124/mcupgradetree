package com.upgradetree.effect;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.tree.NodeEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Critical hits, dodging and lifesteal. */
public final class CombatListener implements Listener {

    private final UpgradeTreePlugin plugin;

    public CombatListener(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    /** Dodge runs early so other plugins see the cancelled hit. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDodge(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        double chance = Math.min(0.75, plugin.effects().sum(victim, NodeEffect.Dodge.class, NodeEffect.Dodge::amount));
        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < chance) {
            e.setCancelled(true);
            victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.02);
            victim.playSound(victim.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.6f, 1.6f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrit(EntityDamageByEntityEvent e) {
        Player p = attacker(e.getDamager());
        if (p == null || e.getEntity() == p) {
            return;
        }
        double chance = 0;
        double multiplier = 1.0;
        for (EffectManager.Typed<NodeEffect.Crit> t : plugin.effects().of(p, NodeEffect.Crit.class)) {
            chance += t.effect().amount() * t.level();
            multiplier = Math.max(multiplier, t.effect().multiplier());
        }
        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, chance)) {
            e.setDamage(e.getDamage() * multiplier);
            Entity target = e.getEntity();
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, target.getHeight() * 0.6, 0),
                    14, 0.3, 0.3, 0.3, 0.25);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLifesteal(EntityDamageByEntityEvent e) {
        Player p = attacker(e.getDamager());
        if (p == null || e.getEntity() == p || !(e.getEntity() instanceof LivingEntity) || p.isDead()) {
            return;
        }
        double frac = plugin.effects().sum(p, NodeEffect.Lifesteal.class, NodeEffect.Lifesteal::amount);
        if (frac <= 0) {
            return;
        }
        double heal = e.getFinalDamage() * Math.min(1.0, frac);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20 : max.getValue();
        if (heal > 0.01 && p.getHealth() < cap) {
            p.setHealth(Math.min(cap, p.getHealth() + heal));
        }
    }
}
