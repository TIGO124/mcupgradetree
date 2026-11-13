package com.upgradetree.menu;

import com.upgradetree.util.Palette;

import java.util.Random;

import static com.upgradetree.menu.Layout.*;

/**
 * Plank background: the panel reads as a wooden board. Horizontal seams split it into planks,
 * every other plank is a shade lighter and short vertical joints end the planks at staggered
 * places. A handful of flat rectangles, so it stays cheap (fewer still with quality LOW).
 */
final class Planks {

    private static final int ROWS = 7;
    private static final double SEAM = 0.0045;

    private Planks() {
    }

    static void spawn(Painter pt, Group chrome, int background, boolean detailed, long seed) {
        Random r = new Random(seed);
        double x0 = PANEL_CX - PANEL_W / 2.0;
        double y0 = PANEL_CY - PANEL_H / 2.0;
        double rowH = PANEL_H / ROWS;
        int seam = Palette.darken(background, 0.38);
        int light = Palette.lighten(background, 0.035);
        for (int i = 0; i < ROWS; i++) {
            double cy = y0 + rowH * (i + 0.5);
            if (detailed && i % 2 == 1) {
                pt.rect(chrome, Part.rect(PANEL_CX, cy, PANEL_W, rowH - SEAM, Z_STARS, light));
            }
            if (i > 0) {
                pt.rect(chrome, Part.rect(PANEL_CX, y0 + rowH * i, PANEL_W, SEAM, Z_STARS, seam));
            }
            if (!detailed || i == ROWS - 1) {
                continue; // the top row sits behind the header: no plank ends there
            }
            // one or two plank ends per row, never lined up with the row above
            int joints = 1 + r.nextInt(2);
            for (int j = 0; j < joints; j++) {
                double x = x0 + PANEL_W * ((j + 0.25 + r.nextDouble() * 0.5) / joints);
                pt.rect(chrome, Part.rect(x, cy, SEAM, rowH - SEAM, Z_STARS, seam));
            }
        }
    }
}
