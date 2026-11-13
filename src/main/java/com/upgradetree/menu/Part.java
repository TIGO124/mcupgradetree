package com.upgradetree.menu;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One display entity of the menu together with its layout in units.
 * <p>
 * Geometry facts used here (taken from the 26.3 client renderer):
 * a text display showing a single space draws its background as a 5 x 10 font-pixel quad
 * (1 font pixel = 0.025 blocks at scale 1) whose bottom edge sits on the entity origin and
 * whose centre is shifted 0.5 px to the right. Text lines are anchored the same way. Item
 * displays render centred but rotated 180 degrees around Y, which is undone here so icons
 * look exactly like inventory icons.
 */
final class Part {

    enum Kind { RECT, TEXT, ITEM, IMAGE }

    final Kind kind;
    Display entity;

    double cx;
    double cy;
    double w;
    double h;
    double angle;
    double z;

    /** TEXT: line height in units, number of lines, and whether cy is the top of the text. */
    double lineHeight;
    int lines = 1;
    boolean topAnchored;

    /** ITEM: icon size in units and the shown material (for previews). */
    double size;
    Material material;

    /** RECT: current background colour (ARGB). */
    int color;

    /** RECT used as a line: end points and thickness. */
    private double lx1;
    private double ly1;
    private double lx2;
    private double ly2;
    private double thickness;

    boolean hidden;

    private Part(Kind kind) {
        this.kind = kind;
    }

    static Part rect(double cx, double cy, double w, double h, double z, int color) {
        return rect(cx, cy, w, h, 0, z, color);
    }

    static Part rect(double cx, double cy, double w, double h, double angle, double z, int color) {
        Part p = new Part(Kind.RECT);
        p.cx = cx;
        p.cy = cy;
        p.w = w;
        p.h = h;
        p.angle = angle;
        p.z = z;
        p.color = color;
        return p;
    }

    static Part line(double x1, double y1, double x2, double y2, double thickness, double z, int color, double progress) {
        Part p = new Part(Kind.RECT);
        p.lx1 = x1;
        p.ly1 = y1;
        p.lx2 = x2;
        p.ly2 = y2;
        p.thickness = thickness;
        p.z = z;
        p.color = color;
        p.progress(progress);
        return p;
    }

    static Part text(double cx, double cy, double lineHeight, int lines, boolean top, double z) {
        Part p = new Part(Kind.TEXT);
        p.cx = cx;
        p.cy = cy;
        p.lineHeight = lineHeight;
        p.lines = Math.max(1, lines);
        p.topAnchored = top;
        p.z = z;
        return p;
    }

    /** A flat picture (resource pack model) covering w x h units. */
    static Part image(double cx, double cy, double w, double h, double z) {
        Part p = new Part(Kind.IMAGE);
        p.cx = cx;
        p.cy = cy;
        p.w = w;
        p.h = h;
        p.z = z;
        return p;
    }

    static Part item(double cx, double cy, double size, double z) {
        Part p = new Part(Kind.ITEM);
        p.cx = cx;
        p.cy = cy;
        p.size = size;
        p.z = z;
        return p;
    }

    /** For line parts: draw the line from its start up to {@code progress} (0..1). */
    void progress(double progress) {
        double ex = lx1 + (lx2 - lx1) * progress;
        double ey = ly1 + (ly2 - ly1) * progress;
        cx = (lx1 + ex) / 2.0;
        cy = (ly1 + ey) / 2.0;
        w = Math.hypot(ex - lx1, ey - ly1);
        h = thickness;
        angle = Math.atan2(ly2 - ly1, lx2 - lx1);
    }

    /**
     * Builds the entity transformation.
     *
     * @param px    pivot x (units) used for group scaling
     * @param py    pivot y (units)
     * @param scale group scale factor around the pivot
     * @param dx    extra x offset (units)
     * @param dy    extra y offset (units)
     */
    Transformation build(Frame f, double px, double py, double scale, double dx, double dy) {
        double s = hidden ? 0.001 : scale;
        double k = 1.0 - z;
        double l = f.unit * k;
        float depth = (float) (z * f.dist);
        double ux = px + (cx - px) * s + dx;
        double uy = py + (cy - py) * s + dy;
        double x = ux * l;
        double y = uy * l;
        switch (kind) {
            case RECT: {
                double ww = Math.max(w * s * l, 1e-4);
                double hh = Math.max(h * s * l, 1e-4);
                double ox = 0.1 * ww;
                double oy = 0.5 * hh;
                double c = Math.cos(angle);
                double sn = Math.sin(angle);
                float tx = (float) (x - (ox * c - oy * sn));
                float ty = (float) (y - (ox * sn + oy * c));
                return new Transformation(new Vector3f(tx, ty, depth), new Quaternionf().rotationZ((float) angle),
                        new Vector3f((float) (ww / 0.125), (float) (hh / 0.25), 1f), new Quaternionf());
            }
            case TEXT: {
                double sc = Math.max(4.0 * lineHeight * s * l, 1e-4);
                double ty = topAnchored
                        ? y - (10.0 * lines - 1.0) * 0.025 * sc
                        : y - (5.0 * lines + 0.5) * 0.025 * sc;
                float tx = (float) (x - 0.0125 * sc);
                return new Transformation(new Vector3f(tx, (float) ty, depth), new Quaternionf(),
                        new Vector3f((float) sc, (float) sc, (float) sc), new Quaternionf());
            }
            case IMAGE: {
                // the model is a 1 x 1 square centred on the origin; the half turn undoes the item display's own
                return new Transformation(new Vector3f((float) x, (float) y, depth),
                        new Quaternionf().rotationY((float) Math.PI),
                        new Vector3f((float) Math.max(w * s * l, 1e-4), (float) Math.max(h * s * l, 1e-4), 1f), new Quaternionf());
            }
            default: {
                double sz = Math.max(size * s * l, 1e-4);
                return new Transformation(new Vector3f((float) x, (float) y, depth),
                        new Quaternionf().rotationY((float) Math.PI),
                        new Vector3f((float) sz, (float) sz, (float) sz), new Quaternionf());
            }
        }
    }

    /** Pushes a transformation with interpolation (0 = instant). */
    static void push(Display e, Transformation t, int duration) {
        if (e == null || !e.isValid()) {
            return;
        }
        e.setInterpolationDelay(0);
        e.setInterpolationDuration(Math.max(0, duration));
        e.setTransformation(t);
    }

    /** Changes the background colour of a RECT, fading over {@code duration} ticks. */
    void recolor(int argb, int duration) {
        if (kind != Kind.RECT || color == argb || entity == null || !entity.isValid()) {
            color = argb;
            return;
        }
        color = argb;
        TextDisplay td = (TextDisplay) entity;
        td.setInterpolationDelay(0);
        td.setInterpolationDuration(Math.max(0, duration));
        td.setBackgroundColor(Color.fromARGB(argb));
    }

    void remove() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        entity = null;
    }
}
