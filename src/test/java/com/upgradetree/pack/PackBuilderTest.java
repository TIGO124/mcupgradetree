package com.upgradetree.pack;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackBuilderTest {

    private static BufferedImage picture(int w, int h, int seed) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, (x * 7 + y * 13 + seed) & 0xFFFFFF);
            }
        }
        return img;
    }

    private static Map<String, byte[]> entries(byte[] zip) throws Exception {
        Map<String, byte[]> out = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                out.put(e.getName(), in.readAllBytes());
            }
        }
        return out;
    }

    @Test
    void fitCutsTheMiddleToThePanel() {
        // a tall picture: the full width stays, top and bottom are cut
        BufferedImage tall = picture(1952, 2000, 1);
        tall.setRGB(976, 1000, 0xFFFFFF);
        BufferedImage fitted = PackBuilder.fit(tall, PackBuilder.ASPECT);
        assertEquals(PackBuilder.WIDTH, fitted.getWidth());
        assertEquals(PackBuilder.HEIGHT, fitted.getHeight());
        BufferedImage wide = PackBuilder.fit(picture(4000, 500, 2), PackBuilder.ASPECT);
        assertEquals(PackBuilder.WIDTH, wide.getWidth());
    }

    @Test
    void packHasModelTextureAndMetadata() throws Exception {
        PackBuilder.Pack pack = PackBuilder.build(PackBuilder.fit(picture(800, 600, 3), PackBuilder.ASPECT));
        Map<String, byte[]> files = entries(pack.zip());
        String meta = new String(files.get("pack.mcmeta"), StandardCharsets.UTF_8);
        assertTrue(meta.contains("\"min_format\": " + PackBuilder.FORMAT), meta);
        String item = new String(files.get("assets/upgradetree/items/menu_background.json"), StandardCharsets.UTF_8);
        assertTrue(item.contains("upgradetree:item/menu_background"));
        String model = new String(files.get("assets/upgradetree/models/item/menu_background.json"), StandardCharsets.UTF_8);
        assertTrue(model.contains("upgradetree:block/menu_background"));
        BufferedImage texture = ImageIO.read(new ByteArrayInputStream(files.get("assets/upgradetree/textures/block/menu_background.png")));
        assertNotNull(texture);
        assertEquals(PackBuilder.WIDTH, texture.getWidth());
        assertEquals(PackBuilder.HEIGHT, texture.getHeight());
        assertNotNull(files.get("pack.png"));
        assertEquals(40, pack.sha1().length());
        assertEquals(PackBuilder.MODEL, "upgradetree:menu_background");
    }

    /** Same picture, same bytes: players keep their cached copy across restarts. */
    @Test
    void sameInputGivesTheSamePack() throws Exception {
        BufferedImage fitted = PackBuilder.fit(picture(900, 500, 4), PackBuilder.ASPECT);
        assertEquals(PackBuilder.build(fitted).sha1(), PackBuilder.build(fitted).sha1());
        BufferedImage other = PackBuilder.fit(picture(900, 500, 5), PackBuilder.ASPECT);
        assertNotEquals(PackBuilder.build(fitted).sha1(), PackBuilder.build(other).sha1());
    }

    @Test
    void serverOnlyKnowsTheCurrentPack() {
        assertEquals("/upgradetree-abc.zip", PackServer.path("abc"));
    }
}
