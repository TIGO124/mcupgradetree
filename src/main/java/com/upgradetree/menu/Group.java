package com.upgradetree.menu;

import java.util.ArrayList;
import java.util.List;

/** A set of parts that scale and move together around a pivot (a node, a button, the frame). */
final class Group {

    final List<Part> parts = new ArrayList<>();
    double pivotX;
    double pivotY;
    double scale = 1.0;
    double dx;
    double dy;

    Group(double pivotX, double pivotY) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    Part add(Part p) {
        parts.add(p);
        return p;
    }

    void addAll(List<Part> list) {
        parts.addAll(list);
    }

    void apply(Frame f, int duration) {
        for (Part p : parts) {
            Part.push(p.entity, p.build(f, pivotX, pivotY, scale, dx, dy), duration);
        }
    }

    void apply(Frame f, Part p, int duration) {
        Part.push(p.entity, p.build(f, pivotX, pivotY, scale, dx, dy), duration);
    }
}
