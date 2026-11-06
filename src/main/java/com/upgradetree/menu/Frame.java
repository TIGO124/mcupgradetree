package com.upgradetree.menu;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * The virtual "screen" in front of the player.
 * <p>
 * Layout coordinates are "units": 1 unit equals the panel distance, so the UI always has
 * the same size on screen no matter how close the panel has to be placed. A layer depth
 * {@code z} moves an element towards the camera by {@code z * dist}; its position and size are
 * scaled by {@code 1 - z} so that it projects exactly onto the base plane (perspective-correct
 * layering, no visible offsets between stacked layers).
 */
final class Frame {

    final World world;
    /** Spawn point of every display entity: the panel centre, rotated to face the player. */
    final Location spawn;
    final Vector eye;
    final Vector forward;
    final Vector right;
    final Vector center;
    final Vector feet;
    final double dist;
    final double uiScale;
    /** World size of one layout unit on the base plane. */
    final double unit;

    Frame(Player player, double baseDistance, double uiScale, double[][] extent) {
        Location eyeLoc = player.getEyeLocation();
        this.world = player.getWorld();
        float yaw = eyeLoc.getYaw();
        double rad = Math.toRadians(yaw);
        this.forward = new Vector(-Math.sin(rad), 0, Math.cos(rad));
        this.right = new Vector(-Math.cos(rad), 0, -Math.sin(rad));
        this.eye = eyeLoc.toVector();
        this.uiScale = uiScale;
        this.dist = fitDistance(player, eyeLoc, baseDistance, extent);
        this.unit = dist * uiScale;
        this.center = eye.clone().add(forward.clone().multiply(dist));
        this.spawn = new Location(world, center.getX(), center.getY(), center.getZ(), yaw + 180f, 0f);
        this.feet = player.getLocation().toVector();
    }

    /** Moves the panel closer if blocks are in the way, so nothing hides the UI. */
    private double fitDistance(Player player, Location eyeLoc, double base, double[][] extent) {
        double best = base;
        double x0 = extent[0][0], x1 = extent[0][1], y0 = extent[1][0], y1 = extent[1][1];
        double[][] samples = {
                {0, 0}, {x0, y1}, {x1, y1}, {x0, y0}, {x1, y0}, {0, y1}, {0, y0}, {x0, 0}, {x1, 0},
                {x0 * 0.5, y1 * 0.5}, {x1 * 0.5, y1 * 0.5}, {x0 * 0.5, y0 * 0.5}, {x1 * 0.5, y0 * 0.5}
        };
        World w = player.getWorld();
        for (double[] s : samples) {
            Vector dir = forward.clone()
                    .add(right.clone().multiply(s[0] * uiScale))
                    .add(new Vector(0, s[1] * uiScale, 0));
            double len = dir.length();
            dir.multiply(1.0 / len);
            double max = base * len + 0.3;
            RayTraceResult res;
            try {
                res = w.rayTraceBlocks(eyeLoc, dir, max, FluidCollisionMode.NEVER, false);
            } catch (Exception e) {
                res = null;
            }
            if (res != null && res.getHitPosition() != null) {
                double fwd = res.getHitPosition().clone().subtract(eye).dot(forward);
                best = Math.min(best, fwd - 0.12);
            }
        }
        return Math.max(0.3, best);
    }

    /** World position of layout point (u, v) at layer depth z. */
    Location point(double u, double v, double z) {
        double k = 1.0 - z;
        Vector p = eye.clone()
                .add(forward.clone().multiply(dist * k))
                .add(right.clone().multiply(u * unit * k))
                .add(new Vector(0, v * unit * k, 0));
        return p.toLocation(world);
    }

    /**
     * Intersects a look ray with the base plane. Returns {u, v} in layout units, or null if
     * the player is not looking towards the panel.
     */
    double[] project(Location eyeNow) {
        Vector dir = eyeNow.getDirection();
        double dot = dir.dot(forward);
        if (dot < 0.2) {
            return null;
        }
        Vector e = eyeNow.toVector();
        double t = center.clone().subtract(e).dot(forward) / dot;
        if (t <= 0) {
            return null;
        }
        Vector rel = e.add(dir.multiply(t)).subtract(center);
        return new double[]{rel.dot(right) / unit, rel.getY() / unit};
    }
}
