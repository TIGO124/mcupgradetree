package com.upgradetree.data;

import java.util.List;
import java.util.UUID;

/** Where player data lives. All methods block and are called from the saver thread (or at startup). */
public interface Storage {

    /** Returns null if the player has no saved data yet. */
    PlayerData.Snapshot load(UUID uuid, String name) throws Exception;

    void save(PlayerData.Snapshot snapshot) throws Exception;

    List<PlayerData.Snapshot> loadAll() throws Exception;

    String describe();

    void close();
}
