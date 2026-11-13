package com.upgradetree.pack;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the small resource pack that carries the menu background: one picture and an item model
 * that shows it on a flat square. Pure logic (no server needed); the same picture always gives the
 * same bytes, so players keep the cached pack across restarts.
 */
public final class PackBuilder {

    /** Item model of the picture (the menu puts it on an item display). */
    public static final String MODEL = "upgradetree:menu_background";
    /** Size of the texture in the pack. */
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 512;
    /** Width / height of the 3D panel (menu.Layout PANEL_W / PANEL_H): the picture is cut to it. */
    public static final double ASPECT = 1.92 / 0.92;
    /** Resource pack format of Minecraft 26.3. */
    static final int FORMAT = 97;

    private static final FileTime TIME = FileTime.fromMillis(1_700_000_000_000L);

    /** The pack: zip bytes, their SHA-1 (hex) and the picture as it is in the pack. */
    public record Pack(byte[] zip, String sha1, BufferedImage image) {
    }

    private PackBuilder() {
    }

    /** Cuts the middle of {@code src} to the {@code aspect} (width / height) and scales it to the texture size. */
    public static BufferedImage fit(BufferedImage src, double aspect) {
        int w = src.getWidth();
        int h = src.getHeight();
        int cw = w;
        int ch = (int) Math.round(w / aspect);
        if (ch > h) {
            ch = h;
            cw = (int) Math.round(h * aspect);
        }
        BufferedImage crop = src.getSubimage((w - cw) / 2, (h - ch) / 2, Math.max(1, cw), Math.max(1, ch));
        // area averaging gives a clean downscale of big photos
        Image scaled = crop.getScaledInstance(WIDTH, HEIGHT, Image.SCALE_AREA_AVERAGING);
        BufferedImage out = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Builds the pack around an already fitted picture. */
    public static Pack build(BufferedImage picture) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            text(zip, "pack.mcmeta", """
                    {
                      "pack": {
                        "description": "UpgradeTree menu background",
                        "min_format": %d,
                        "max_format": %d
                      }
                    }
                    """.formatted(FORMAT, FORMAT));
            text(zip, "assets/upgradetree/items/menu_background.json", """
                    {
                      "model": {
                        "type": "minecraft:model",
                        "model": "upgradetree:item/menu_background"
                      }
                    }
                    """);
            // one flat square facing the viewer, unshaded; the texture sits with the block textures,
            // which every item model can use
            text(zip, "assets/upgradetree/models/item/menu_background.json", """
                    {
                      "gui_light": "front",
                      "textures": {
                        "picture": "upgradetree:block/menu_background",
                        "particle": "upgradetree:block/menu_background"
                      },
                      "elements": [
                        {
                          "from": [0, 0, 8],
                          "to": [16, 16, 8],
                          "shade": false,
                          "faces": {
                            "south": { "uv": [0, 0, 16, 16], "texture": "#picture" },
                            "north": { "uv": [16, 0, 0, 16], "texture": "#picture" }
                          }
                        }
                      ]
                    }
                    """);
            entry(zip, "assets/upgradetree/textures/block/menu_background.png", png(picture));
            entry(zip, "pack.png", png(icon(picture)));
        }
        byte[] zip = bytes.toByteArray();
        return new Pack(zip, sha1(zip), picture);
    }

    /** A square crop from the middle of the picture for the pack list. */
    static BufferedImage icon(BufferedImage picture) {
        int side = Math.min(picture.getWidth(), picture.getHeight());
        BufferedImage crop = picture.getSubimage((picture.getWidth() - side) / 2, (picture.getHeight() - side) / 2, side, side);
        BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(crop.getScaledInstance(128, 128, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g.dispose();
        return out;
    }

    private static void text(ZipOutputStream zip, String name, String content) throws IOException {
        entry(zip, name, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void entry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry e = new ZipEntry(name);
        e.setLastModifiedTime(TIME);
        zip.putNextEntry(e);
        zip.write(data);
        zip.closeEntry();
    }

    static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("no PNG writer");
        }
        return out.toByteArray();
    }

    public static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
