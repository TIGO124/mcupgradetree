package com.upgradetree.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** One small YAML file per player in plugins/UpgradeTree/players/. */
public final class YamlStorage implements Storage {

    private final File folder;
    private final Logger log;

    public YamlStorage(File folder, Logger log) {
        this.folder = folder;
        this.log = log;
        if (!folder.exists() && !folder.mkdirs()) {
            log.warning("Could not create the players folder: " + folder);
        }
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    @Override
    public PlayerData.Snapshot load(UUID uuid, String name) throws Exception {
        File f = file(uuid);
        if (!f.exists()) {
            return null;
        }
        return DataCodec.decode(uuid, name, Files.readString(f.toPath(), StandardCharsets.UTF_8));
    }

    @Override
    public void save(PlayerData.Snapshot s) throws IOException {
        File f = file(s.uuid());
        File tmp = new File(folder, s.uuid() + ".yml.tmp");
        Files.writeString(tmp.toPath(), DataCodec.encode(s), StandardCharsets.UTF_8);
        try {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public List<PlayerData.Snapshot> loadAll() {
        List<PlayerData.Snapshot> out = new ArrayList<>();
        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return out;
        }
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().substring(0, f.getName().length() - 4));
                PlayerData.Snapshot s = load(id, id.toString());
                if (s != null) {
                    out.add(s);
                }
            } catch (Exception e) {
                log.fine("Skipped: " + f.getName() + " (" + e.getMessage() + ")");
            }
        }
        return out;
    }

    @Override
    public String describe() {
        return "YAML (" + folder.getName() + "/)";
    }

    @Override
    public void close() {
        // nothing to close
    }
}
