package com.upgradetree.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Upgrade levels and points of one player. */
public final class PlayerData {

    public final UUID uuid;
    private String name;
    private final Map<String, Integer> levels = new LinkedHashMap<>();
    private double points;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public synchronized String name() {
        return name;
    }

    public synchronized void name(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    public synchronized int level(String nodeId) {
        Integer l = levels.get(nodeId);
        return l == null ? 0 : l;
    }

    public synchronized void setLevel(String nodeId, int level) {
        if (level <= 0) {
            levels.remove(nodeId);
        } else {
            levels.put(nodeId, level);
        }
    }

    public synchronized Map<String, Integer> levels() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(levels));
    }

    public synchronized void clearLevels() {
        levels.clear();
    }

    public synchronized double points() {
        return points;
    }

    public synchronized void points(double points) {
        this.points = Math.max(0, points);
    }
}
