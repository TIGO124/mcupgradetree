package com.upgradetree.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caches player data and writes it on one background thread. Changes only mark the data dirty;
 * dirty data is written by the periodic autosave, on quit and on important changes. A snapshot
 * waiting to be written is also served to readers, so a quick rejoin can never read stale data.
 */
public final class DataStore {

    private final Storage storage;
    private final Logger log;
    private final DoubleSupplier startingPoints;
    /** Applied to every loaded snapshot (ids of older versions); returns its argument when nothing changes. */
    private volatile UnaryOperator<PlayerData.Snapshot> upgrader = UnaryOperator.identity();
    private static final long PRELOAD_GRACE_MS = 60_000L;

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData.Snapshot> pending = new ConcurrentHashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UpgradeTree-Saver");
        t.setDaemon(true);
        return t;
    });

    public DataStore(Storage storage, Logger log, DoubleSupplier startingPoints) {
        this.storage = storage;
        this.log = log;
        this.startingPoints = startingPoints;
    }

    public Storage storage() {
        return storage;
    }

    /** Sets the step that brings loaded data up to date (see {@link com.upgradetree.config.LegacyIds}). */
    public void upgrader(UnaryOperator<PlayerData.Snapshot> upgrader) {
        this.upgrader = upgrader == null ? UnaryOperator.identity() : upgrader;
    }

    /** Data of an online player (loaded on demand and cached). */
    public PlayerData get(Player p) {
        PlayerData d = cache.computeIfAbsent(p.getUniqueId(), id -> loadNow(id, p.getName()));
        d.name(p.getName());
        return d;
    }

    /** Loads data before the player joins (called from the async pre-login event). */
    public void preload(UUID uuid, String name) {
        if (!cache.containsKey(uuid)) {
            cache.putIfAbsent(uuid, loadNow(uuid, name));
        }
    }

    /** Cached data or null, never loads (cheap lookups). */
    public PlayerData cachedOrNull(UUID uuid) {
        return cache.get(uuid);
    }

    /** Cached data if present, otherwise loaded without caching (offline players). */
    public PlayerData peek(UUID uuid, String name) {
        PlayerData d = cache.get(uuid);
        return d != null ? d : loadNow(uuid, name);
    }

    public boolean exists(UUID uuid) {
        if (cache.containsKey(uuid) || pending.containsKey(uuid)) {
            return true;
        }
        try {
            return storage.load(uuid, "") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private PlayerData loadNow(UUID uuid, String name) {
        PlayerData.Snapshot s = pending.get(uuid);
        if (s == null) {
            try {
                s = storage.load(uuid, name);
            } catch (Exception e) {
                log.log(Level.WARNING, "Could not read player data: " + uuid, e);
            }
        }
        if (s == null) {
            PlayerData d = new PlayerData(uuid, name);
            d.points(startingPoints.getAsDouble());
            d.clearDirty();
            return d;
        }
        PlayerData.Snapshot up = upgrade(s);
        PlayerData d = PlayerData.of(up);
        if (up == s) {
            d.clearDirty();
        } else {
            d.markDirty(); // saved with the new ids by the next autosave
        }
        return d;
    }

    private PlayerData.Snapshot upgrade(PlayerData.Snapshot s) {
        try {
            PlayerData.Snapshot up = upgrader.apply(s);
            return up == null ? s : up;
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Could not update the data of " + s.uuid(), e);
            return s;
        }
    }

    /** Runs the upgrader on cached data again (after a reload changed the trees or chests). */
    public void upgradeCached() {
        for (PlayerData d : cache.values()) {
            PlayerData.Snapshot s = d.snapshot();
            PlayerData.Snapshot up = upgrade(s);
            if (up != s) {
                d.replaceIds(up);
            }
        }
    }

    /** Queues a save right away (purchases, admin changes). */
    public void save(PlayerData d) {
        PlayerData.Snapshot s = d.snapshot();
        d.clearDirty();
        pending.put(d.uuid, s);
        try {
            writer.execute(() -> write(s));
        } catch (RejectedExecutionException e) {
            write(s);
        }
    }

    private void write(PlayerData.Snapshot s) {
        try {
            storage.save(s);
            pending.remove(s.uuid(), s);
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not save player data: " + s.uuid() + " (" + s.name() + ")", e);
            PlayerData cached = cache.get(s.uuid());
            if (cached != null) {
                cached.markDirty();
            }
        }
    }

    /**
     * Writes dirty data, retries failed writes of players who already left and forgets players that
     * are no longer online. Data loaded in the last minute is kept: it may belong to a player who is
     * still logging in (pre-login loads it before the player counts as online).
     */
    public void autosave() {
        long now = System.currentTimeMillis();
        for (PlayerData d : new ArrayList<>(cache.values())) {
            if (d.isDirty()) {
                save(d);
            }
            if (Bukkit.getPlayer(d.uuid) == null && !d.isDirty() && now - d.loadedAt() > PRELOAD_GRACE_MS) {
                cache.remove(d.uuid, d);
            }
        }
        for (PlayerData.Snapshot s : new ArrayList<>(pending.values())) {
            if (!cache.containsKey(s.uuid())) {
                try {
                    writer.execute(() -> write(s));
                } catch (RejectedExecutionException e) {
                    // shutting down; shutdown() flushes what is left
                }
            }
        }
    }

    public void unload(UUID uuid) {
        PlayerData d = cache.remove(uuid);
        if (d != null) {
            save(d);
        }
    }

    public List<PlayerData> cached() {
        return new ArrayList<>(cache.values());
    }

    /**
     * Waits until every queued save is written (the writer handles them in order). Blocking: call
     * off the main thread. False when a save failed or it took longer than the timeout.
     */
    public boolean flush(long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        try {
            // a few rounds: saves queued while waiting are written too, a failed save stays pending
            for (int round = 0; round < 3; round++) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) {
                    return false;
                }
                writer.submit(() -> { }).get(left, TimeUnit.MILLISECONDS);
                if (pending.isEmpty()) {
                    return true;
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (RejectedExecutionException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            return false;
        }
    }

    /** Every stored player merged with unsaved changes. Blocking: call off the main thread. */
    public List<PlayerData.Snapshot> loadAll() {
        Map<UUID, PlayerData.Snapshot> all = new LinkedHashMap<>();
        try {
            for (PlayerData.Snapshot s : storage.loadAll()) {
                all.put(s.uuid(), upgrade(s));
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not list the player data", e);
        }
        all.putAll(pending);
        for (PlayerData d : cache.values()) {
            all.put(d.uuid, d.snapshot());
        }
        return new ArrayList<>(all.values());
    }

    /** Flushes everything synchronously (used on disable). */
    public void shutdown() {
        for (PlayerData d : cache.values()) {
            save(d);
        }
        for (PlayerData.Snapshot s : new ArrayList<>(pending.values())) {
            if (!cache.containsKey(s.uuid())) {
                writer.execute(() -> write(s));
            }
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(15, TimeUnit.SECONDS)) {
                log.warning("Some player data could not be saved in time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        storage.close();
    }
}
