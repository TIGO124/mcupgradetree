package com.upgradetree.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Upgrade levels, points and progress of one player. Every change marks the data dirty. */
public final class PlayerData {

    public final UUID uuid;
    private String name;
    private final Map<String, Integer> levels = new LinkedHashMap<>();
    /** Currency spent per node, used for fair refunds. */
    private final Map<String, Double> spent = new LinkedHashMap<>();
    private double points;
    private int prestige;
    private long lastRespec;
    private String lastDaily = "";
    private String menuMode = "";
    private String language = "";
    /** Bought but not yet opened chests (chest id → count). */
    private final Map<String, Integer> chests = new LinkedHashMap<>();
    private volatile boolean dirty;
    /** When this object was loaded (not saved); fresh pre-login data must survive the autosave sweep. */
    private final long loadedAt = System.currentTimeMillis();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    /** Immutable copy used for saving on another thread. */
    public record Snapshot(UUID uuid, String name, Map<String, Integer> levels, Map<String, Double> spent,
                           double points, int prestige, long lastRespec, String lastDaily, String menuMode,
                           String language, Map<String, Integer> chests) {

        public Snapshot {
            chests = chests == null ? Map.of() : chests;
        }

        public int totalLevels() {
            int t = 0;
            for (int v : levels.values()) {
                t += v;
            }
            return t;
        }
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(uuid, name, Map.copyOf(levels), Map.copyOf(spent), points, prestige, lastRespec,
                lastDaily, menuMode, language, Map.copyOf(chests));
    }

    public static PlayerData of(Snapshot s) {
        PlayerData d = new PlayerData(s.uuid(), s.name());
        d.levels.putAll(s.levels());
        d.spent.putAll(s.spent());
        d.points = s.points();
        d.prestige = s.prestige();
        d.lastRespec = s.lastRespec();
        d.lastDaily = s.lastDaily() == null ? "" : s.lastDaily();
        d.menuMode = s.menuMode() == null ? "" : s.menuMode();
        d.language = s.language() == null ? "" : s.language();
        for (Map.Entry<String, Integer> e : s.chests().entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                d.chests.put(e.getKey(), e.getValue());
            }
        }
        return d;
    }

    public long loadedAt() {
        return loadedAt;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    void clearDirty() {
        dirty = false;
    }

    public synchronized String name() {
        return name;
    }

    public synchronized void name(String name) {
        if (name != null && !name.equals(this.name)) {
            this.name = name;
            dirty = true;
        }
    }

    public synchronized int level(String nodeId) {
        Integer l = levels.get(nodeId);
        return l == null ? 0 : l;
    }

    public synchronized void setLevel(String nodeId, int level) {
        if (level <= 0) {
            levels.remove(nodeId);
            spent.remove(nodeId);
        } else {
            levels.put(nodeId, level);
        }
        dirty = true;
    }

    public synchronized Map<String, Integer> levels() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(levels));
    }

    public synchronized void clearLevels() {
        levels.clear();
        spent.clear();
        dirty = true;
    }

    public synchronized double spent(String nodeId) {
        return spent.getOrDefault(nodeId, 0.0);
    }

    public synchronized void addSpent(String nodeId, double amount) {
        if (amount > 0) {
            spent.merge(nodeId, amount, Double::sum);
            dirty = true;
        }
    }

    public synchronized double points() {
        return points;
    }

    /** Sets the balance (never below zero); NaN or infinity from a command or another plugin is ignored. */
    public synchronized void points(double points) {
        if (!Double.isFinite(points)) {
            return;
        }
        this.points = Math.max(0, points);
        dirty = true;
    }

    public synchronized void addPoints(double amount) {
        points(points + amount);
    }

    public synchronized int prestige() {
        return prestige;
    }

    public synchronized void prestige(int prestige) {
        this.prestige = Math.max(0, prestige);
        dirty = true;
    }

    public synchronized long lastRespec() {
        return lastRespec;
    }

    public synchronized void lastRespec(long millis) {
        this.lastRespec = millis;
        dirty = true;
    }

    public synchronized String lastDaily() {
        return lastDaily;
    }

    public synchronized void lastDaily(String day) {
        this.lastDaily = day == null ? "" : day;
        dirty = true;
    }

    public synchronized String menuMode() {
        return menuMode;
    }

    public synchronized void menuMode(String mode) {
        this.menuMode = mode == null ? "" : mode;
        dirty = true;
    }

    /** Chosen message language ("" = server language). */
    public synchronized String language() {
        return language;
    }

    public synchronized void language(String code) {
        this.language = code == null ? "" : code;
        dirty = true;
    }

    /** Unopened chests of one type. */
    public synchronized int chests(String chestId) {
        return chests.getOrDefault(chestId, 0);
    }

    /** Every unopened chest (copy). */
    public synchronized Map<String, Integer> chests() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(chests));
    }

    public synchronized int totalChests() {
        int t = 0;
        for (int v : chests.values()) {
            t += v;
        }
        return t;
    }

    /** Adds (or with a negative amount removes) unopened chests; never goes below zero. */
    public synchronized void addChests(String chestId, int amount) {
        int next = Math.max(0, chests.getOrDefault(chestId, 0) + amount);
        if (next == 0) {
            chests.remove(chestId);
        } else {
            chests.put(chestId, next);
        }
        dirty = true;
    }

    /** Removes one unopened chest; false (and no change) if there is none. */
    public synchronized boolean takeChest(String chestId) {
        int have = chests.getOrDefault(chestId, 0);
        if (have <= 0) {
            return false;
        }
        addChests(chestId, -1);
        return true;
    }
}
