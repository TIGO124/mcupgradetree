package com.upgradetree.menu;

import net.kyori.adventure.text.Component;

import java.util.List;

import static com.upgradetree.menu.Layout.*;

/** A rounded button on the info card (buy, or the -1 / +1 pair of the admin mode). */
final class CardButton {

    final Group group;
    private final List<Part> fill;
    private final Part label;
    final double cx;
    final double cy;
    private final double w;
    private final double h;
    private boolean visible;
    private boolean hover;

    private CardButton(Group group, List<Part> fill, Part label, double cx, double cy, double w, double h) {
        this.group = group;
        this.fill = fill;
        this.label = label;
        this.cx = cx;
        this.cy = cy;
        this.w = w;
        this.h = h;
    }

    static CardButton spawn(Painter pt, double cx, double cy, double w, double h, int stroke, int color) {
        Group g = pt.group(cx, cy);
        g.scale = 0.001;
        pt.roundRect(g, cx, cy - 0.004, w + 0.016, h + 0.024, 0.03, Z_BORDER, stroke, 3);
        List<Part> fill = pt.roundRect(g, cx, cy, w, h, 0.026, Z_FILL, color, 3);
        Part label = pt.text(g, Part.text(cx, cy, 0.033, 1, false, Z_TEXT), Component.empty(), 2000);
        return new CardButton(g, fill, label, cx, cy, w, h);
    }

    boolean hit(double u, double v) {
        return visible && Math.abs(u - cx) <= w / 2.0 + 0.01 && Math.abs(v - cy) <= h / 2.0 + 0.01;
    }

    void show(DisplayMenu menu, Component text, int color) {
        if (!visible) {
            visible = true;
            group.scale = hover ? 1.05 : 1.0;
            group.apply(menu.frame(), 3);
        }
        Painter.setText(label, text);
        for (Part p : fill) {
            p.recolor(color, 4);
        }
    }

    void hide(DisplayMenu menu) {
        if (visible) {
            visible = false;
            group.scale = 0.001;
            group.apply(menu.frame(), 3);
        }
    }

    void setHover(DisplayMenu menu, boolean on) {
        hover = on;
        if (visible) {
            group.scale = on ? 1.05 : 1.0;
            group.apply(menu.frame(), 3);
        }
    }

    void shrink(DisplayMenu menu, int duration) {
        group.scale = 0.001;
        group.apply(menu.frame(), duration);
    }
}
