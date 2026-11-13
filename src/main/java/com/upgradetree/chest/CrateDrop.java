package com.upgradetree.chest;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * One bought chest standing in front of its owner (only they see it). Right click starts the show:
 * the chest rattles, the lid opens and lights up, the roulette rises and spins, the prize is given.
 */
final class CrateDrop {

    enum Phase { APPEAR, IDLE, SHAKE, LID, SPIN, REVEAL, OUTRO, DONE }

    private static final double LID_OPEN = Math.toRadians(105);

    private final UpgradeTreePlugin plugin;
    private final ChestManager manager;
    final Player owner;
    final ChestType chest;
    private final Location base;
    private final ChestModel model = new ChestModel();
    private Interaction hitbox;
    private TextDisplay label;
    private Roulette roulette;
    private Location rouletteAt;
    private Strip strip;
    private ChestReward prize;
    private boolean committed;
    private Phase phase = Phase.APPEAR;
    private int t;
    private int idle;
    private int lastUnder = Integer.MIN_VALUE;

    CrateDrop(UpgradeTreePlugin plugin, ChestManager manager, Player owner, ChestType chest, Location spot) {
        this.plugin = plugin;
        this.manager = manager;
        this.owner = owner;
        this.chest = chest;
        this.base = Displays.facing(spot, owner.getEyeLocation());
    }

    /** Where a chest fits in front of the player: on the floor, in sight, 1-2.3 blocks ahead. */
    static Location spot(Player p) {
        Location feet = p.getLocation();
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        double yaw = Math.toRadians(feet.getYaw());
        Vector fwd = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        for (double d : new double[]{2.3, 2.0, 1.7, 1.4, 1.1}) {
            Location probe = feet.clone().add(fwd.clone().multiply(d));
            Vector to = probe.clone().add(0, 0.6, 0).toVector().subtract(eye.toVector());
            double len = to.length();
            if (len > 0.01 && w.rayTraceBlocks(eye, to.clone().multiply(1 / len), len, FluidCollisionMode.NEVER, true) != null) {
                continue;
            }
            Location top = probe.clone();
            top.setY(feet.getY() + 1.0);
            RayTraceResult down = w.rayTraceBlocks(top, new Vector(0, -1, 0), 4.0, FluidCollisionMode.NEVER, true);
            double y = down == null ? feet.getY() : down.getHitPosition().getY();
            if (y > feet.getY() + 1.0 || y < feet.getY() - 3.0) {
                continue;
            }
            Location at = probe.clone();
            at.setY(y);
            if (!w.getBlockAt(at.clone().add(0, 0.5, 0)).isPassable()) {
                continue;
            }
            return at;
        }
        return feet.clone();
    }

    private Lang lang() {
        return plugin.lang();
    }

    // ------------------------------------------------------------------ life cycle

    void spawn() {
        model.lift = 1.6;
        model.scaleX = model.scaleY = model.scaleZ = 0.55;
        model.spawn(plugin, owner, base, chest.look);
        hitbox = base.getWorld().spawn(base, Interaction.class, i -> {
            i.setPersistent(false);
            i.setVisibleByDefault(false);
            i.setInteractionWidth(0.95f);
            i.setInteractionHeight(0.95f);
            i.setResponsive(true);
        });
        owner.showEntity(plugin, hitbox);
        sound("minecraft:entity.illusioner.cast_spell", 0.5f, 1.6f);
    }

    boolean owns(Entity e) {
        return hitbox != null && hitbox.equals(e);
    }

    /** Waiting to be opened (it may still be landing). */
    boolean waiting() {
        return phase == Phase.APPEAR || phase == Phase.IDLE;
    }

    boolean done() {
        return phase == Phase.DONE;
    }

    /** Right click by the owner: rolls the prize now (so the strip can stop on it) and starts the show. */
    boolean click() {
        if (!waiting()) {
            return false;
        }
        prize = manager.roll(owner, chest);
        strip = Strip.build(chest.chances(), prize.index, manager.registry().spinTicks, ThreadLocalRandom.current());
        Displays.remove(label);
        label = null;
        model.outline(null);
        model.scaleX = model.scaleY = model.scaleZ = 1;
        model.lift = 0;
        phase = Phase.SHAKE;
        t = 0;
        return true;
    }

    /** Ends at once: a rolled prize is still given (quit, death, reload). */
    void finishNow() {
        if (prize != null && !committed) {
            committed = true;
            manager.commit(owner, chest, prize);
        }
        removeAll();
    }

    /** Removes an unopened chest; it stays in the player's chests. */
    void discard() {
        removeAll();
    }

    private void removeAll() {
        model.remove();
        if (roulette != null) {
            roulette.remove();
            roulette = null;
        }
        Displays.remove(label);
        label = null;
        if (hitbox != null && hitbox.isValid()) {
            hitbox.remove();
        }
        hitbox = null;
        phase = Phase.DONE;
    }

    // ------------------------------------------------------------------ ticking

    void tick() {
        if (phase == Phase.DONE) {
            return;
        }
        t++;
        switch (phase) {
            case APPEAR -> appear();
            case IDLE -> idle();
            case SHAKE -> shake();
            case LID -> lid();
            case SPIN -> spin();
            case REVEAL -> reveal();
            case OUTRO -> outro();
            default -> {
            }
        }
    }

    private void appear() {
        if (t == 1) {
            model.lift = 0;
            model.scaleX = model.scaleY = model.scaleZ = 1;
            model.apply(5);
        } else if (t == 6) {
            model.scaleX = model.scaleZ = 1.12;
            model.scaleY = 0.8;
            model.apply(2);
            sound("minecraft:block.chest.close", 0.8f, 0.7f);
            sound("minecraft:block.amethyst_block.chime", 0.8f, 1.3f);
            particles(Particle.POOF, base.clone().add(0, 0.1, 0), 14, 0.35, 0.02, 0.35, 0.02);
        } else if (t == 8) {
            model.scaleX = model.scaleY = model.scaleZ = 1;
            model.apply(3);
        } else if (t >= 11) {
            phase = Phase.IDLE;
            t = 0;
            model.outline(Color.fromRGB(Palette.rgb(chest.color)));
            label = Displays.spawn(plugin, owner, base.clone().add(0, ChestModel.HEIGHT + 0.35, 0), TextDisplay.class, d -> {
                d.setBillboard(Display.Billboard.CENTER);
                d.text(Text.mm("<b>" + chest.name() + "</b>\n" + lang().raw("chest-label-hint")));
                d.setBackgroundColor(Color.fromARGB(0x50000000));
                d.setDefaultBackground(false);
                d.setShadowed(true);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.7f, 0.7f, 0.7f),
                        new Quaternionf()));
            });
            owner.sendActionBar(lang().plain("chest-appeared", "chest", chest.name()));
        }
    }

    private void idle() {
        idle++;
        if (idle % 40 == 0) {
            particles(Particle.ENCHANT, base.clone().add(0, 0.7, 0), 10, 0.3, 0.3, 0.3, 0.4);
        }
        if (idle % 120 == 0) {
            owner.sendActionBar(lang().plain("chest-appeared", "chest", chest.name()));
        }
        boolean away = !owner.getWorld().equals(base.getWorld()) || owner.getLocation().distanceSquared(base) > 24 * 24;
        if (away || idle > manager.registry().despawnTicks) {
            discard();
            if (owner.isOnline()) {
                owner.sendMessage(lang().msg("chest-returned"));
            }
        }
    }

    private void shake() {
        double decay = 1 - t / 16.0;
        model.spin = 0.15 * Math.sin(t * Math.PI / 2) * decay;
        model.tilt = 0.05 * Math.sin(t * Math.PI / 2 + 1) * decay;
        model.lift = 0.04 * decay;
        if (t == 1 || t == 5 || t == 9) {
            sound("minecraft:block.chest.locked", 0.9f, 0.9f + t * 0.03f);
        }
        if (t >= 14) {
            model.spin = model.tilt = model.lift = 0;
            phase = Phase.LID;
            t = 0;
        }
        model.apply(1);
    }

    private void lid() {
        if (t == 1) {
            sound("minecraft:block.chest.open", 1.0f, 0.8f);
            sound("minecraft:block.ender_chest.open", 0.6f, 1.3f);
            model.glowInside(true);
            particles(Particle.END_ROD, base.clone().add(0, 0.7, 0), 18, 0.2, 0.15, 0.2, 0.06);
        }
        if (t <= 10) {
            model.lid = LID_OPEN * Strip.ease(t / 10.0);
            model.apply(1);
        }
        if (t == 4) {
            Vector toOwner = owner.getLocation().toVector().subtract(base.toVector()).setY(0);
            if (toOwner.lengthSquared() > 1e-4) {
                toOwner.normalize().multiply(0.3);
            }
            rouletteAt = Displays.facing(base.clone().add(0, 1.5, 0).add(toOwner), owner.getEyeLocation());
            roulette = new Roulette(plugin, owner, rouletteAt, chest, strip);
            roulette.spawn(lang());
        } else if (t == 5 && roulette != null) {
            roulette.setScale(1.0, 6);
            sound("minecraft:block.beacon.activate", 0.5f, 1.8f);
        } else if (t >= 12) {
            phase = Phase.SPIN;
            t = 0;
        }
    }

    private void spin() {
        double p = strip.position(t);
        if (roulette != null) {
            roulette.update(p, 2, false);
        }
        int under = Strip.under(p);
        if (under != lastUnder) {
            lastUnder = under;
            sound("minecraft:block.note_block.hat", 0.45f, 1.75f);
        }
        if (t % 5 == 0) {
            particles(Particle.END_ROD, base.clone().add(0, 0.75, 0), 1, 0.15, 0.05, 0.15, 0.02);
        }
        if (strip.finished(t)) {
            if (roulette != null) {
                roulette.update(strip.end, 2, false);
            }
            phase = Phase.REVEAL;
            t = 0;
        }
    }

    private void reveal() {
        if (t == 1 && owner.isDead()) {
            t = 0; // wait for the respawn: a dead player would lose the prize
            return;
        }
        if (t == 1) {
            if (roulette != null) {
                roulette.reveal(prize, lang());
            }
            committed = true;
            manager.commit(owner, chest, prize);
            celebrate();
        } else if (t == 16 && roulette != null) {
            double dist = Math.hypot(owner.getEyeLocation().getX() - rouletteAt.getX(), owner.getEyeLocation().getZ() - rouletteAt.getZ());
            roulette.flyPrize(dist, owner.getEyeLocation().getY() - rouletteAt.getY() - 0.4);
        } else if (t == 26) {
            sound("minecraft:entity.item.pickup", 0.7f, 1.2f);
            if (roulette != null) {
                roulette.hidePrize();
            }
        } else if (t >= 46) {
            phase = Phase.OUTRO;
            t = 0;
        }
    }

    private void celebrate() {
        Rarity r = prize.rarity;
        if (r.announce()) {
            sound("minecraft:ui.toast.challenge_complete", 0.9f, 1.0f);
        } else if (r.rank() == 0) {
            sound("minecraft:entity.experience_orb.pickup", 0.8f, 1.1f);
        } else {
            sound("minecraft:entity.player.levelup", 0.7f, 1.0f + 0.15f * Math.min(3, r.rank()));
        }
        if (rouletteAt == null) {
            return;
        }
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(Palette.rgb(r.color())), 1.1f);
        double yaw = Math.toRadians(rouletteAt.getYaw());
        Vector right = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2 * i / 28;
            Location at = rouletteAt.clone().add(right.clone().multiply(Math.cos(a) * 0.32)).add(0, Math.sin(a) * 0.32, 0);
            owner.spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, dust);
        }
        if (r.rank() >= 2) {
            particles(Particle.END_ROD, rouletteAt, 30, 0.5, 0.3, 0.2, 0.08);
        }
        if (r.announce()) {
            particles(Particle.TOTEM_OF_UNDYING, rouletteAt, 60, 0.6, 0.4, 0.3, 0.35);
        }
    }

    private void outro() {
        if (t == 1 && roulette != null) {
            roulette.setScale(0.001, 6);
        }
        if (t <= 8) {
            model.lid = LID_OPEN * (1 - Strip.ease(t / 8.0));
            model.apply(1);
        }
        if (t == 7) {
            sound("minecraft:block.chest.close", 0.8f, 1.0f);
        } else if (t == 8) {
            model.glowInside(false);
        } else if (t == 10) {
            model.scaleX = model.scaleY = model.scaleZ = 0.001;
            model.lift = 0.3;
            model.apply(5);
        } else if (t == 14) {
            particles(Particle.POOF, base.clone().add(0, 0.4, 0), 16, 0.3, 0.25, 0.3, 0.02);
            sound("minecraft:entity.chicken.egg", 0.6f, 0.6f);
        } else if (t >= 15) {
            removeAll();
        }
    }

    private void sound(String key, float volume, float pitch) {
        if (owner.isOnline()) {
            owner.playSound(base, key, SoundCategory.MASTER, volume, pitch);
        }
    }

    private void particles(Particle type, Location at, int count, double dx, double dy, double dz, double speed) {
        if (owner.isOnline() && plugin.settings().particles) {
            owner.spawnParticle(type, at, count, dx, dy, dz, speed);
        }
    }
}
