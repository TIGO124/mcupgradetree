package com.upgradetree.menu;

import com.upgradetree.tree.HexGrid;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Spawns the display entities of one menu (visible only to its viewer) and keeps track of them. */
final class Painter {

    private final Plugin plugin;
    private final Player player;
    final Frame frame;
    private final List<Display> entities = new ArrayList<>();
    /** Every group ever created, for previews. */
    final List<Group> groups = new ArrayList<>();

    Painter(Plugin plugin, Player player, Frame frame) {
        this.plugin = plugin;
        this.player = player;
        this.frame = frame;
    }

    Group group(double pivotX, double pivotY) {
        Group g = new Group(pivotX, pivotY);
        groups.add(g);
        return g;
    }

    void forget(Group g) {
        g.remove();
        groups.remove(g);
        entities.removeIf(e -> !e.isValid());
    }

    private <T extends Display> T spawn(Class<T> type, Group g, Part p, Consumer<T> setup) {
        Transformation t = p.build(frame, g.pivotX, g.pivotY, g.scale, g.dx, g.dy);
        T e = frame.world.spawn(frame.spawn, type, d -> {
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setShadowRadius(0f);
            d.setShadowStrength(0f);
            d.setInterpolationDuration(0);
            d.setTeleportDuration(0);
            d.setTransformation(t);
            setup.accept(d);
        });
        player.showEntity(plugin, e);
        entities.add(e);
        p.entity = e;
        g.add(p);
        return e;
    }

    Part rect(Group g, Part p) {
        int color = p.color;
        spawn(TextDisplay.class, g, p, d -> {
            d.text(Component.text(" "));
            d.setBackgroundColor(Color.fromARGB(color));
            d.setDefaultBackground(false);
            d.setShadowed(false);
            d.setSeeThrough(false);
            d.setLineWidth(200);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
        return p;
    }

    Part text(Group g, Part p, Component text, int lineWidth) {
        spawn(TextDisplay.class, g, p, d -> {
            d.text(text);
            d.setBackgroundColor(Color.fromARGB(0));
            d.setDefaultBackground(false);
            d.setShadowed(true);
            d.setSeeThrough(false);
            d.setLineWidth(lineWidth);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
        return p;
    }

    Part item(Group g, Part p, ItemStack stack) {
        p.material = stack.getType();
        spawn(ItemDisplay.class, g, p, d -> {
            d.setItemStack(stack);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
        });
        return p;
    }

    /** Pointy-top hexagon of circumradius r: three overlapping rotated rectangles. */
    List<Part> hex(Group g, double cx, double cy, double r, double z, int color) {
        List<Part> list = new ArrayList<>(3);
        for (double[] rr : HexGrid.rects(r)) {
            list.add(rect(g, Part.rect(cx, cy, rr[0], rr[1], rr[2], z, color)));
        }
        return list;
    }

    /**
     * Rounded rectangle made of overlapping plain rectangles: one wide, one tall and
     * (with 3 steps) an inset one that fills the corner, giving a smooth pixel-rounded corner.
     */
    List<Part> roundRect(Group g, double cx, double cy, double w, double h, double r, double z, int color, int steps) {
        List<Part> list = new ArrayList<>(3);
        r = Math.min(r, Math.min(w, h) / 2.0);
        if (r <= 1e-4 || steps <= 1) {
            list.add(rect(g, Part.rect(cx, cy, w, h, z, color)));
            return list;
        }
        list.add(rect(g, Part.rect(cx, cy, w, h - 2 * r, z, color)));
        list.add(rect(g, Part.rect(cx, cy, w - 2 * r, h, z, color)));
        if (steps >= 3) {
            list.add(rect(g, Part.rect(cx, cy, w - 0.58 * r, h - 0.58 * r, z, color)));
        }
        return list;
    }

    static void setText(Part p, Component c) {
        if (p != null && p.entity instanceof TextDisplay td && td.isValid() && !c.equals(td.text())) {
            td.text(c);
        }
    }

    int count() {
        int c = 0;
        for (Display e : entities) {
            if (e.isValid()) {
                c++;
            }
        }
        return c;
    }

    void removeAll() {
        for (Display e : entities) {
            if (e.isValid()) {
                e.remove();
            }
        }
        entities.clear();
    }
}
