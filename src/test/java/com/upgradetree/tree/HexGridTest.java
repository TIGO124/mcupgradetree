package com.upgradetree.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexGridTest {

    private static final double EPS = 1e-9;

    @Test
    void threeRectanglesConnectTheCorners() {
        double r = 2.0;
        double[][] rects = HexGrid.rects(r);
        assertEquals(3, rects.length);
        for (double[] rect : rects) {
            assertEquals(Math.sqrt(3) * r, rect[0], EPS);
            assertEquals(r, rect[1], EPS);
            // every rectangle corner is a hexagon corner (distance r from the centre)
            double c = Math.cos(rect[2]);
            double s = Math.sin(rect[2]);
            for (double[] k : List.of(new double[]{1, 1}, new double[]{1, -1}, new double[]{-1, 1}, new double[]{-1, -1})) {
                double x = k[0] * rect[0] / 2;
                double y = k[1] * rect[1] / 2;
                assertEquals(r, Math.hypot(x * c - y * s, x * s + y * c), EPS);
            }
        }
    }

    @Test
    void containsMatchesThePointyTopShape() {
        double r = 1.0;
        assertTrue(HexGrid.contains(0, 0, r));
        assertTrue(HexGrid.contains(0, 0.99, r), "near the top corner");
        assertTrue(HexGrid.contains(0.86, 0, r), "near a flat side");
        assertFalse(HexGrid.contains(0.88, 0, r), "outside the flat side");
        assertFalse(HexGrid.contains(0.8, 0.8, r), "outside a slanted edge");
        assertFalse(HexGrid.contains(0, 1.01, r));
    }

    @Test
    void neighboursTouchButDoNotOverlap() {
        double r = HexGrid.RADIUS;
        // side neighbour one width away: the shared edge midpoint is on both borders
        assertTrue(HexGrid.contains(0.5 - 1e-6, 0, r));
        assertFalse(HexGrid.contains(0.5 + 1e-6, 0, r));
        // diagonal neighbour at (0.5, ROW_STEP)
        double mx = 0.25;
        double my = HexGrid.ROW_STEP / 2;
        assertTrue(HexGrid.contains(mx * 0.999, my * 0.999, r));
        assertFalse(HexGrid.contains(mx * 1.01, my * 1.01, r));
    }

    @Test
    void insetShrinksByBorderThickness() {
        double r = 1.0;
        double t = 0.1;
        double inner = HexGrid.inset(r, t);
        // the distance between parallel edges drops by 2t
        assertEquals(Math.sqrt(3) * r - 2 * t, Math.sqrt(3) * inner, EPS);
        assertEquals(0, HexGrid.inset(0.01, 1), EPS);
    }
}
