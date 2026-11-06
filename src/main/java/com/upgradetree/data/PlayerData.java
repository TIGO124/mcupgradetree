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
    private volatile boolean dirty;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    /** Immutable copy used for saving on another thread. */
    public record Snapshot(UUID uuid, String name, Map<String, Integer> levels, Map<String, Double> spent,
                           double points, int prestige, long lastRespec, String lastDaily, String menuMode) {

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
                lastDaily, menuMode);
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
        return d;
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

    public synchronized void points(double points) {
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
}
