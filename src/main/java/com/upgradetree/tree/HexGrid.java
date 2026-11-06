package com.upgradetree.tree;

/**
 * Pointy-top hexagon grid used by tree files. Side neighbours sit at x±1 (same y), diagonal
 * neighbours at x±0.5 and y±1. One x step equals one hexagon width (flat side to flat side).
 * Pure math, shared by the menu, the validator and the tests.
 */
public final class HexGrid {

    /** Vertical distance between rows, in hexagon widths: 1.5 * R with width = sqrt(3) * R. */
    public static final double ROW_STEP = Math.sqrt(3.0) / 2.0;
    /** Circumradius of a hexagon of width 1. */
    public static final double RADIUS = 1.0 / Math.sqrt(3.0);

    private HexGrid() {
    }

    /**
     * The three rectangles whose union is a pointy-top hexagon of circumradius r, as
     * {width, height, angleRadians}. Each rectangle connects four of the six corners.
     */
    public static double[][] rects(double r) {
        double w = Math.sqrt(3.0) * r;
        return new double[][]{
                {w, r, 0.0},
                {w, r, Math.PI / 3.0},
                {w, r, -Math.PI / 3.0}
        };
    }

    /** True if point (px, py) lies inside a pointy-top hexagon of circumradius r centred at the origin. */
    public static boolean contains(double px, double py, double r) {
        double x = Math.abs(px);
        double y = Math.abs(py);
        double halfW = Math.sqrt(3.0) / 2.0 * r;
        if (x > halfW || y > r) {
            return false;
        }
        // slanted edge from (0, r) to (halfW, r/2)
        return y <= r - x / Math.sqrt(3.0) + 1e-12;
    }

    /** Circumradius of the hexagon left after removing a border of the given thickness. */
    public static double inset(double r, double thickness) {
        return Math.max(0, r - 2.0 * thickness / Math.sqrt(3.0));
    }
}
