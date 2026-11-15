package com.upgradetree.update;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The copy made before an update: plugins/UpgradeTree/backups/&lt;version&gt;-&lt;time&gt;/ holds the
 * jar that was running and data.zip with every file of the plugin folder (settings, trees, chests,
 * messages, player data). Unzipping data.zip into the plugin folder and putting the jar back into
 * plugins/ restores the old version as it was.
 */
public final class Backup {

    public static final String FOLDER = "backups";
    /** Built again from background.jpg on every start. */
    private static final String GENERATED = "resourcepack.zip";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Backup() {
    }

    /** Writes the backup and returns its folder. Any file that cannot be copied fails the whole backup. */
    public static Path create(Path dataFolder, Path jar, String version, LocalDateTime now) throws IOException {
        Path dir = dataFolder.resolve(FOLDER).resolve(version + "-" + now.format(STAMP));
        for (int i = 2; Files.exists(dir); i++) {
            dir = dataFolder.resolve(FOLDER).resolve(version + "-" + now.format(STAMP) + "-" + i);
        }
        Files.createDirectories(dir);
        if (jar != null && Files.isRegularFile(jar)) {
            Files.copy(jar, dir.resolve(jar.getFileName().toString()), StandardCopyOption.COPY_ATTRIBUTES);
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dataFolder)) {
            files = walk.filter(Files::isRegularFile).filter(f -> !skipped(dataFolder.relativize(f))).sorted().toList();
        }
        try (OutputStream raw = Files.newOutputStream(dir.resolve("data.zip")); ZipOutputStream zip = new ZipOutputStream(raw)) {
            for (Path f : files) {
                zip.putNextEntry(new ZipEntry(dataFolder.relativize(f).toString().replace('\\', '/')));
                Files.copy(f, zip);
                zip.closeEntry();
            }
        }
        return dir;
    }

    /** Older backups and the generated resource pack are left out. */
    static boolean skipped(Path relative) {
        return relative.getName(0).toString().equals(FOLDER) || relative.toString().equals(GENERATED);
    }
}
