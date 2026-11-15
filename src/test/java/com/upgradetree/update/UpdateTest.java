package com.upgradetree.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateTest {

    private static Version v(String s) {
        Version v = Version.parse(s);
        assertNotNull(v, s);
        return v;
    }

    @Test
    void versionsCompareByTheirNumbers() {
        assertTrue(v("2.4.0").newerThan(v("2.3.0")));
        assertTrue(v("2.10.0").newerThan(v("2.9.9")), "numbers, not text");
        assertTrue(v("3").newerThan(v("2.99.99")));
        assertEquals(v("v2.3.0"), v("2.3.0"), "a leading v is ignored");
        assertEquals(v("2.3"), v("2.3.0"));
        assertEquals(v("2.3").hashCode(), v("2.3.0").hashCode());
        assertTrue(v("2.4.0").newerThan(v("2.4.0-beta")), "a pre-release is older than the release");
        assertTrue(v("2.4.0-rc2").newerThan(v("2.4.0-rc1")));
        assertFalse(v("2.3.0").newerThan(v("2.3.0")));
        assertEquals("2.4.0", v("v2.4.0").toString());
        assertTrue(v("26.3").newerThan(v("26.2")));
        assertNull(Version.parse("latest"));
        assertNull(Version.parse(""));
        assertNull(Version.parse(null));
        assertNull(Version.parse("99999999999.0"), "too long for a number");
    }

    @Test
    void theReleaseJarIsFoundInTheGithubAnswer() {
        String json = """
                {
                  "tag_name": "v2.4.0",
                  "html_url": "https://github.com/owner/repo/releases/tag/v2.4.0",
                  "assets": [
                    { "name": "UpgradeTree-2.4.0-sources.jar", "url": "https://api.github.com/a/1", "size": 5 },
                    { "name": "notes.txt", "url": "https://api.github.com/a/2", "size": 1 },
                    { "name": "UpgradeTree-2.4.0.jar", "url": "https://api.github.com/a/3", "size": 1234,
                      "digest": "sha256:ABCDEF0123", "browser_download_url": "https://github.com/x.jar" }
                  ]
                }
                """;
        Release r = Release.parse(json, "UpgradeTree");
        assertNotNull(r);
        assertEquals("v2.4.0", r.tag());
        assertEquals("2.4.0", r.version());
        assertEquals("UpgradeTree-2.4.0.jar", r.assetName());
        assertEquals("https://api.github.com/a/3", r.assetUrl(), "the API address works for private repositories too");
        assertEquals(1234, r.size());
        assertEquals("abcdef0123", r.sha256());

        Release other = Release.parse("""
                { "tag_name": "2.5.0", "assets": [ { "name": "plugin.jar", "browser_download_url": "https://x/plugin.jar" } ] }
                """, "UpgradeTree");
        assertNotNull(other, "any jar when none is named after the plugin");
        assertEquals("https://x/plugin.jar", other.assetUrl());
        assertEquals(-1, other.size());
        assertNull(other.sha256());

        assertNull(Release.parse("{ \"tag_name\": \"v2.4.0\", \"assets\": [] }", "UpgradeTree"));
        assertNull(Release.parse("{ \"assets\": [ { \"name\": \"UpgradeTree.jar\" } ] }", "UpgradeTree"), "no tag");
    }

    private static void zip(Path file, String entry, String text) throws IOException {
        try (OutputStream raw = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(raw)) {
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(text.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    @Test
    void aPluginJarTellsItsNameAndVersions(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("UpgradeTree-2.4.0.jar");
        zip(jar, "paper-plugin.yml", "name: UpgradeTree\nversion: '2.4.0'\napi-version: '26.2'\nmain: x.Y\n");
        assertEquals(new PluginJar("UpgradeTree", "2.4.0", "26.2"), PluginJar.read(jar));

        Path old = dir.resolve("Old.jar");
        zip(old, "plugin.yml", "name: Other\nversion: 1.0\n");
        assertEquals("Other", PluginJar.read(old).name());

        Path empty = dir.resolve("Empty.jar");
        zip(empty, "readme.txt", "hi");
        assertNull(PluginJar.read(empty));

        Path page = dir.resolve("error.jar");
        Files.writeString(page, "<html>Not Found</html>");
        assertNull(PluginJar.read(page), "an error page is no jar");
    }

    @Test
    void theBackupHoldsTheOldJarAndEveryDataFile(@TempDir Path dir) throws IOException {
        Path data = dir.resolve("UpgradeTree");
        Files.createDirectories(data.resolve("players"));
        Files.createDirectories(data.resolve("trees"));
        Files.createDirectories(data.resolve("backups/2.2.0-20260101-000000"));
        Files.writeString(data.resolve("config.yml"), "config-version: 2\n");
        Files.writeString(data.resolve("players/0000.yml"), "points: 5\n");
        Files.writeString(data.resolve("trees/main.yml"), "title: Main\n");
        Files.writeString(data.resolve("resourcepack.zip"), "generated");
        Files.writeString(data.resolve("backups/2.2.0-20260101-000000/data.zip"), "old backup");
        Path jar = dir.resolve("UpgradeTree-2.3.0.jar");
        Files.writeString(jar, "jar");

        Path backup = Backup.create(data, jar, "2.3.0", LocalDateTime.of(2026, 11, 15, 22, 30, 5));
        assertEquals(data.resolve("backups/2.3.0-20261115-223005"), backup);
        assertEquals("jar", Files.readString(backup.resolve("UpgradeTree-2.3.0.jar")));
        List<String> entries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(backup.resolve("data.zip").toFile())) {
            zip.stream().forEach(e -> entries.add(e.getName()));
            assertEquals("points: 5\n", new String(zip.getInputStream(zip.getEntry("players/0000.yml")).readAllBytes(),
                    StandardCharsets.UTF_8));
        }
        assertEquals(List.of("config.yml", "players/0000.yml", "trees/main.yml"), entries,
                "older backups and the generated resource pack stay out");

        Path second = Backup.create(data, jar, "2.3.0", LocalDateTime.of(2026, 11, 15, 22, 30, 5));
        assertEquals(data.resolve("backups/2.3.0-20261115-223005-2"), second, "a backup never overwrites another one");
    }
}
