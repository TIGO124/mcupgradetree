package com.upgradetree.chest;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Display entities placed in the world for one viewer (the chest and its roulette), in block units.
 * Local axes of a display spawned with yaw facing the viewer: +x = viewer's right, +y = up,
 * +z = towards the viewer. Rect/text geometry matches the 3D menu (see menu.Part).
 */
final class Displays {

    private Displays() {
    }

    static <T extends Display> T spawn(Plugin plugin, Player viewer, Location at, Class<T> type, Consumer<T> setup) {
        T e = at.getWorld().spawn(at, type, d -> {
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setShadowRadius(0f);
            d.setShadowStrength(0f);
            d.setInterpolationDuration(0);
            d.setTeleportDuration(0);
            setup.accept(d);
        });
        viewer.showEntity(plugin, e);
        return e;
    }

    /** A flat coloured rectangle (text display background). */
    static TextDisplay rect(Plugin plugin, Player viewer, Location at, int argb, Transformation t) {
        return spawn(plugin, viewer, at, TextDisplay.class, d -> {
            d.text(Component.text(" "));
            d.setBackgroundColor(Color.fromARGB(argb));
            d.setDefaultBackground(false);
            d.setShadowed(false);
            d.setSeeThrough(false);
            d.setLineWidth(200);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setTransformation(t);
        });
    }

    static TextDisplay text(Plugin plugin, Player viewer, Location at, Component text, Transformation t) {
        return spawn(plugin, viewer, at, TextDisplay.class, d -> {
            d.text(text);
            d.setBackgroundColor(Color.fromARGB(0));
            d.setDefaultBackground(false);
            d.setShadowed(true);
            d.setSeeThrough(false);
            d.setLineWidth(400);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setTransformation(t);
        });
    }

    static ItemDisplay item(Plugin plugin, Player viewer, Location at, ItemStack stack, Transformation t) {
        return spawn(plugin, viewer, at, ItemDisplay.class, d -> {
            d.setItemStack(stack);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            d.setTransformation(t);
        });
    }

    /** Rectangle centred on (cx, cy) at depth z, rotated by {@code angle} (radians) around its centre. */
    static Transformation rect(double cx, double cy, double z, double w, double h, double angle) {
        double ww = Math.max(w, 1e-4);
        double hh = Math.max(h, 1e-4);
        double ox = 0.1 * ww;
        double oy = 0.5 * hh;
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Transformation(
                new Vector3f((float) (cx - (ox * c - oy * s)), (float) (cy - (ox * s + oy * c)), (float) z),
                new Quaternionf().rotationZ((float) angle),
                new Vector3f((float) (ww / 0.125), (float) (hh / 0.25), 1f), new Quaternionf());
    }

    static Transformation rect(double cx, double cy, double z, double w, double h) {
        return rect(cx, cy, z, w, h, 0);
    }

    /** Text centred on (cx, cy); {@code lineHeight} is the height of one line in blocks. */
    static Transformation text(double cx, double cy, double z, double lineHeight, int lines) {
        double sc = Math.max(4.0 * lineHeight, 1e-4);
        double ty = cy - (5.0 * Math.max(1, lines) + 0.5) * 0.025 * sc;
        return new Transformation(new Vector3f((float) (cx - 0.0125 * sc), (float) ty, (float) z), new Quaternionf(),
                new Vector3f((float) sc, (float) sc, (float) sc), new Quaternionf());
    }

    /** Inventory-style icon centred on (cx, cy) (item displays face away by default, hence the half turn). */
    static Transformation item(double cx, double cy, double z, double size) {
        float s = (float) Math.max(size, 1e-4);
        return new Transformation(new Vector3f((float) cx, (float) cy, (float) z), new Quaternionf().rotationY((float) Math.PI),
                new Vector3f(s, s, s), new Quaternionf());
    }

    static void push(Display d, Transformation t, int duration) {
        if (d == null || !d.isValid()) {
            return;
        }
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(0, duration));
        d.setTransformation(t);
    }

    static void push(Display d, Matrix4f m, int duration) {
        if (d == null || !d.isValid()) {
            return;
        }
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(0, duration));
        d.setTransformationMatrix(m);
    }

    /** Background colour change, faded by the client over {@code duration} ticks. */
    static void recolor(TextDisplay d, int argb, int duration) {
        if (d == null || !d.isValid()) {
            return;
        }
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(0, duration));
        d.setBackgroundColor(Color.fromARGB(argb));
    }

    static void remove(Display d) {
        if (d != null && d.isValid()) {
            d.remove();
        }
    }

    /** A location at {@code base} whose yaw makes display +z point (horizontally) at {@code target}. */
    static Location facing(Location base, Location target) {
        Location l = base.clone();
        double dx = target.getX() - base.getX();
        double dz = target.getZ() - base.getZ();
        if (dx * dx + dz * dz < 1e-6) {
            l.setYaw(target.getYaw() + 180f);
        } else {
            l.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        }
        l.setPitch(0f);
        return l;
    }
}
