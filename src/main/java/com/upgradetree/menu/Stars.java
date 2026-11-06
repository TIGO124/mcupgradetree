package com.upgradetree.menu;

import com.upgradetree.util.Text;

import java.util.Random;

import static com.upgradetree.menu.Layout.*;

/** Starry space background made of two text displays (cheap: two entities for the whole sky). */
final class Stars {

    private static final String[] COLORS = {"#FFFFFF", "#E0E7FF", "#C7D2FE", "#93C5FD", "#A5B4FC", "#7C86A8", "#5B6385"};

    private Stars() {
    }

    static void spawn(Painter pt, Group chrome, long seed) {
        Random r = new Random(seed);
        double big = 0.042;
        double small = 0.028;
        int bigRows = (int) Math.floor(PANEL_H / big) - 1;
        int smallRows = (int) Math.floor(PANEL_H / small) - 1;
        pt.text(chrome, Part.text(PANEL_CX, PANEL_CY, big, bigRows, false, Z_STARS),
                Text.mm(field(r, bigRows, (int) (PANEL_W / big * 10 / 4.6), 0.012, true)), 20000);
        pt.text(chrome, Part.text(PANEL_CX, PANEL_CY, small, smallRows, false, Z_STARS),
                Text.mm(field(r, smallRows, (int) (PANEL_W / small * 10 / 4.6), 0.022, false)), 20000);
    }

    /** Rows of spaces with a few coloured star glyphs (MiniMessage). */
    static String field(Random r, int rows, int cols, double density, boolean big) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < rows; row++) {
            if (row > 0) {
                sb.append('\n');
            }
            for (int col = 0; col < cols; col++) {
                if (r.nextDouble() >= density) {
                    sb.append(' ');
                    continue;
                }
                String color = COLORS[r.nextInt(COLORS.length)];
                String glyph;
                double g = r.nextDouble();
                if (big) {
                    glyph = g < 0.55 ? "." : (g < 0.85 ? "+" : "✦");
                } else {
                    glyph = g < 0.85 ? "." : "·";
                }
                sb.append("<color:").append(color).append('>').append(glyph).append("</color>");
            }
        }
        return sb.toString();
    }
}
