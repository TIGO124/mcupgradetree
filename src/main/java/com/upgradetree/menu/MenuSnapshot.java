package com.upgradetree.menu;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.TextDisplay;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.upgradetree.menu.Layout.*;

/**
 * Debug helper: draws the current state of a menu into a PNG, the way the viewer sees it
 * (layout units are perspective-correct, so a flat drawing matches the screen). Item icons
 * come from {@code debug/textures/<item>.png} when present, otherwise a placeholder is drawn.
 */
final class MenuSnapshot {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final int[] CODES = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private record Item(Group group, Part part) {
    }

    private MenuSnapshot() {
    }

    static void save(DisplayMenu menu, File out, File textures) throws IOException {
        double ppu = 1000;
        int w = (int) ((PANEL_W + 0.1) * ppu);
        int h = (int) ((PANEL_H + 0.1) * ppu);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x2B3A2E));
        g.fillRect(0, 0, w, h);

        List<Item> items = new ArrayList<>();
        for (Group gr : menu.groups()) {
            for (Part p : gr.parts) {
                if (p.entity != null && p.entity.isValid()) {
                    items.add(new Item(gr, p));
                }
            }
        }
        items.sort((a, b) -> Double.compare(a.part().z, b.part().z));
        for (Item it : items) {
            Part p = it.part();
            double[] pl = it.group().placed(p);
            double px = w / 2.0 + (pl[0] - PANEL_CX) * ppu;
            double py = h / 2.0 - (pl[1] - PANEL_CY) * ppu;
            double s = pl[2];
            switch (p.kind) {
                case RECT -> drawRect(g, p, px, py, s, ppu);
                case TEXT -> drawText(g, p, px, py, s, ppu);
                case ITEM -> drawItem(g, p, px, py, s, ppu, textures);
            }
        }
        g.dispose();
        File parent = out.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Klasör oluşturulamadı: " + parent);
        }
        ImageIO.write(img, "png", out);
    }

    private static void drawRect(Graphics2D g, Part p, double px, double py, double s, double ppu) {
        double rw = p.w * s * ppu;
        double rh = p.h * s * ppu;
        if (rw < 0.3 || rh < 0.3 || (p.color >>> 24) == 0) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(px, py);
        g.rotate(-p.angle);
        g.setColor(new Color(p.color, true));
        g.fill(new Rectangle2D.Double(-rw / 2, -rh / 2, rw, rh));
        g.setTransform(old);
    }

    private static void drawText(Graphics2D g, Part p, double px, double py, double s, double ppu) {
        if (!(p.entity instanceof TextDisplay td)) {
            return;
        }
        String legacy = LEGACY.serialize(td.text());
        if (legacy.isBlank()) {
            return;
        }
        String[] lines = legacy.split("\n", -1);
        double lineH = p.lineHeight * s * ppu;
        if (lineH < 1) {
            return;
        }
        double top = p.topAnchored ? py : py - lines.length * lineH / 2.0;
        int carried = 0xFFFFFF;
        boolean bold = false;
        for (int i = 0; i < lines.length; i++) {
            List<Object[]> segs = new ArrayList<>();
            int[] state = {carried, bold ? 1 : 0};
            parse(lines[i], segs, state);
            carried = state[0];
            bold = state[1] == 1;
            double width = 0;
            for (Object[] seg : segs) {
                width += g.getFontMetrics(font((Boolean) seg[2], lineH)).stringWidth((String) seg[0]);
            }
            double x = px - width / 2.0;
            double baseline = top + (i + 0.8) * lineH;
            for (Object[] seg : segs) {
                Font f = font((Boolean) seg[2], lineH);
                g.setFont(f);
                FontMetrics fm = g.getFontMetrics(f);
                String str = (String) seg[0];
                int rgb = (Integer) seg[1];
                g.setColor(new Color(darken(rgb)));
                g.drawString(str, (float) (x + lineH * 0.08), (float) (baseline + lineH * 0.08));
                g.setColor(new Color(rgb));
                g.drawString(str, (float) x, (float) baseline);
                x += fm.stringWidth(str);
            }
        }
    }

    private static Font font(boolean bold, double lineH) {
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, Math.max(1, (int) Math.round(lineH * 0.78)));
    }

    private static int darken(int rgb) {
        return ((((rgb >> 16) & 0xFF) / 4) << 16) | ((((rgb >> 8) & 0xFF) / 4) << 8) | ((rgb & 0xFF) / 4);
    }

    /** Splits a §-coded line into {text, rgb, bold} segments. */
    private static void parse(String line, List<Object[]> out, int[] state) {
        StringBuilder cur = new StringBuilder();
        int color = state[0];
        boolean bold = state[1] == 1;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '§' && i + 1 < line.length()) {
                char code = Character.toLowerCase(line.charAt(i + 1));
                if (cur.length() > 0) {
                    out.add(new Object[]{cur.toString(), color, bold});
                    cur.setLength(0);
                }
                if (code == 'x' && i + 13 < line.length()) {
                    StringBuilder hex = new StringBuilder();
                    for (int k = 0; k < 6; k++) {
                        hex.append(line.charAt(i + 3 + k * 2));
                    }
                    try {
                        color = Integer.parseInt(hex.toString(), 16);
                    } catch (NumberFormatException ignored) {
                        // keep colour
                    }
                    bold = false;
                    i += 13;
                    continue;
                }
                int idx = "0123456789abcdef".indexOf(code);
                if (idx >= 0) {
                    color = CODES[idx];
                    bold = false;
                } else if (code == 'l') {
                    bold = true;
                } else if (code == 'r') {
                    color = 0xFFFFFF;
                    bold = false;
                }
                i++;
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0) {
            out.add(new Object[]{cur.toString(), color, bold});
        }
        state[0] = color;
        state[1] = bold ? 1 : 0;
    }

    private static void drawItem(Graphics2D g, Part p, double px, double py, double s, double ppu, File textures) {
        double size = p.size * s * ppu;
        if (size < 1 || p.material == null) {
            return;
        }
        File tex = null;
        if (textures != null) {
            String base = p.material.name().toLowerCase(Locale.ROOT);
            for (String name : new String[]{base, base + "_side", base + "_front", base + "_top", base.replace("_stairs", "_planks")}) {
                File f = new File(textures, name + ".png");
                if (f.isFile()) {
                    tex = f;
                    break;
                }
            }
        }
        if (tex != null && tex.isFile()) {
            try {
                BufferedImage icon = ImageIO.read(tex);
                if (icon != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int sz = (int) Math.round(size);
                    BufferedImage frame = icon.getHeight() > icon.getWidth() ? icon.getSubimage(0, 0, icon.getWidth(), icon.getWidth()) : icon;
                    g.drawImage(frame, (int) Math.round(px - sz / 2.0), (int) Math.round(py - sz / 2.0), sz, sz, null);
                    return;
                }
            } catch (IOException ignored) {
                // placeholder below
            }
        }
        int hash = p.material.name().hashCode();
        Color c = Color.getHSBColor((hash & 0xFF) / 255f, 0.55f, 0.95f);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setColor(c);
        double box = size * 0.7;
        g.fillRoundRect((int) (px - box / 2), (int) (py - box / 2), (int) box, (int) box, (int) (box / 3), (int) (box / 3));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect((int) (px - box / 2), (int) (py - box / 2), (int) box, (int) box, (int) (box / 3), (int) (box / 3));
        g.setComposite(AlphaComposite.SrcOver);
        String label = p.material.name().replace("_", "");
        label = label.substring(0, Math.min(2, label.length()));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(1, (int) (box * 0.45))));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, (float) (px - fm.stringWidth(label) / 2.0), (float) (py + fm.getAscent() / 2.5));
    }
}
