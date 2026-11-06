package com.upgradetree.menu;

import com.upgradetree.util.Palette;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/** Dust particle effects drawn on the panel (only the viewer sees them). */
final class Sparks {

    private Sparks() {
    }

    private static float size(Frame f, double base, double min, double max) {
        return (float) Math.max(min, Math.min(max, base * f.dist / 1.6));
    }

    /** Ring burst after a purchase; a second golden ring when the upgrade is maxed. */
    static void burst(DisplayMenu menu, double cx, double cy, double radius, int color, boolean max) {
        Frame f = menu.frame();
        float size = size(f, 0.6, 0.25, 1.0);
        Particle.DustOptions main = new Particle.DustOptions(Color.fromRGB(Palette.rgb(Palette.lighten(color, 0.2))), size);
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, size * 0.8f);
        double r = radius * 1.25;
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            Location loc = f.point(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0.1);
            menu.player().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, (i % 2 == 0) ? main : white);
        }
        if (max) {
            Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(Palette.rgb(menu.settings().theme.gold())), size);
            for (int i = 0; i < 18; i++) {
                double a = Math.PI * 2 * i / 18 + 0.17;
                Location loc = f.point(cx + Math.cos(a) * r * 1.4, cy + Math.sin(a) * r * 1.4, 0.1);
                menu.player().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, gold);
            }
        }
    }

    /** Six sparks on the corners of a hexagon that just appeared. */
    static void ring(DisplayMenu menu, double cx, double cy, double radius, int color) {
        Frame f = menu.frame();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(Palette.rgb(Palette.lighten(color, 0.45))),
                size(f, 0.45, 0.2, 0.8));
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + Math.PI / 6;
            Location loc = f.point(cx + Math.cos(a) * radius, cy + Math.sin(a) * radius, 0.1);
            menu.player().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
        }
    }
}
