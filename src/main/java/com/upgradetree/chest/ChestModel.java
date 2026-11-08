package com.upgradetree.chest;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The chest that appears in front of the player, built from block displays so its lid can really
 * open: body, two metal bands, a latch and a dark inside that lights up. Coordinates are in pixels
 * (1/16 block), centred on the chest, front (+z) towards the player. The lid turns around the back
 * hinge; every frame is pushed per tick, so interpolation never cuts the arc short.
 */
final class ChestModel {

    private static final double PX = 1.0 / 16.0;
    private static final double HINGE_Y = 9 * PX;
    private static final double HINGE_Z = -7 * PX;
    static final double HEIGHT = 14 * PX;

    private record Box(double x0, double y0, double z0, double x1, double y1, double z1, boolean lid) {
    }

    private record Piece(BlockDisplay entity, Box box) {
    }

    private final List<Piece> pieces = new ArrayList<>();
    private BlockDisplay inside;

    /** Global pose (blocks / radians), changed by the animations and pushed with {@link #apply}. */
    double lift;
    double spin;
    double tilt;
    double scaleX = 1;
    double scaleY = 1;
    double scaleZ = 1;
    /** Lid opening angle in radians (0 = closed). */
    double lid;

    void spawn(Plugin plugin, Player viewer, Location at, ChestType.Look look) {
        add(plugin, viewer, at, look.body(), new Box(-7, 0, -7, 7, 9, 7, false));
        add(plugin, viewer, at, look.trim(), new Box(-7.25, 7.6, -7.25, 7.25, 8.6, 7.25, false));
        inside = add(plugin, viewer, at, Material.BLACK_CONCRETE, new Box(-6, 9.08, -6, 6, 9.14, 6, false));
        add(plugin, viewer, at, look.body(), new Box(-7, 9, -7, 7, 14, 7, true));
        add(plugin, viewer, at, look.trim(), new Box(-7.25, 9.4, -7.25, 7.25, 10.4, 7.25, true));
        add(plugin, viewer, at, look.latch(), new Box(-1, 7, 7, 1, 11, 8, true));
    }

    private BlockDisplay add(Plugin plugin, Player viewer, Location at, Material m, Box b) {
        BlockDisplay d = Displays.spawn(plugin, viewer, at, BlockDisplay.class, e -> {
            e.setBlock(m.createBlockData());
            e.setTransformationMatrix(matrix(b));
        });
        pieces.add(new Piece(d, b));
        return d;
    }

    /** Lights up the inside (when the lid opens). */
    void glowInside(boolean on) {
        if (inside != null && inside.isValid()) {
            inside.setBlock((on ? Material.GLOWSTONE : Material.BLACK_CONCRETE).createBlockData());
        }
    }

    /** Coloured outline (see-through) while the chest waits to be opened; null turns it off. */
    void outline(Color color) {
        for (Piece p : pieces) {
            if (p.entity().isValid()) {
                p.entity().setGlowColorOverride(color);
                p.entity().setGlowing(color != null);
            }
        }
    }

    private Matrix4f matrix(Box b) {
        Matrix4f m = new Matrix4f()
                .translate(0f, (float) lift, 0f)
                .rotateY((float) spin)
                .rotateZ((float) tilt)
                .scale((float) scaleX, (float) scaleY, (float) scaleZ);
        if (b.lid() && lid != 0) {
            m.translate(0f, (float) HINGE_Y, (float) HINGE_Z)
                    .rotateX((float) -lid)
                    .translate(0f, (float) -HINGE_Y, (float) -HINGE_Z);
        }
        return m.translate((float) (b.x0() * PX), (float) (b.y0() * PX), (float) (b.z0() * PX))
                .scale((float) ((b.x1() - b.x0()) * PX), (float) ((b.y1() - b.y0()) * PX), (float) ((b.z1() - b.z0()) * PX));
    }

    /** Pushes the current pose to every piece. */
    void apply(int duration) {
        for (Piece p : pieces) {
            Displays.push(p.entity(), matrix(p.box()), duration);
        }
    }

    void remove() {
        for (Piece p : pieces) {
            Displays.remove(p.entity());
        }
        pieces.clear();
        inside = null;
    }
}
