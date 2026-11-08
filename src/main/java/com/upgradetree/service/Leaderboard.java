package com.upgradetree.service;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.data.PlayerData;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Top players by prestige or total levels, with an optional floating hologram. The hologram is one
 * text display per language in use; every player only sees the one in their own language.
 */
public final class Leaderboard {

    public enum Type { PRESTIGE, LEVELS }

    public record Entry(UUID uuid, String name, int prestige, int levels) {
    }

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, TextDisplay> holograms = new HashMap<>();

    public Leaderboard(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    /** Reloads every stored player in the background. */
    public void refreshAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerData.Snapshot> all = plugin.data().loadAll();
            for (PlayerData.Snapshot s : all) {
                entries.put(s.uuid(), new Entry(s.uuid(), s.name(), s.prestige(), s.totalLevels()));
            }
        });
    }

    /** Updates one player's entry right away. */
    public void touch(PlayerData d) {
        PlayerData.Snapshot s = d.snapshot();
        entries.put(s.uuid(), new Entry(s.uuid(), s.name(), s.prestige(), s.totalLevels()));
    }

    public List<Entry> top(Type type, int size) {
        Comparator<Entry> cmp = type == Type.PRESTIGE
                ? Comparator.comparingInt(Entry::prestige).thenComparingInt(Entry::levels)
                : Comparator.comparingInt(Entry::levels).thenComparingInt(Entry::prestige);
        List<Entry> list = new ArrayList<>();
        for (Entry e : entries.values()) {
            if (e.levels() > 0 || e.prestige() > 0) {
                list.add(e);
            }
        }
        list.sort(cmp.reversed().thenComparing(e -> e.name() == null ? "" : e.name().toLowerCase(Locale.ROOT)));
        return list.size() > size ? list.subList(0, size) : list;
    }

    public String value(Lang lang, Type type, Entry e) {
        return lang.format(type == Type.PRESTIGE ? "top-value-prestige" : "top-value-levels",
                "prestige", String.valueOf(e.prestige()), "levels", String.valueOf(e.levels()));
    }

    // ------------------------------------------------------------------ hologram

    private Location hologramLocation() {
        String raw = plugin.getConfig().getString("leaderboard.hologram", "");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] p = raw.split(";");
        if (p.length < 4) {
            return null;
        }
        World w = Bukkit.getWorld(p[0]);
        if (w == null) {
            return null;
        }
        try {
            return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setHologram(Location loc) {
        String raw = loc == null ? "" : loc.getWorld().getName() + ";" + round(loc.getX()) + ";" + round(loc.getY() + 2.2)
                + ";" + round(loc.getZ());
        plugin.getConfig().set("leaderboard.hologram", raw);
        plugin.saveConfig();
        removeEntity();
        updateHologram();
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Spawns or refreshes the holograms (called every minute and after reloads). */
    public void updateHologram() {
        Location loc = hologramLocation();
        if (loc == null) {
            removeEntity();
            return;
        }
        if (!loc.isChunkLoaded()) {
            return;
        }
        Set<String> codes = new LinkedHashSet<>();
        codes.add(plugin.lang().code());
        for (Player p : Bukkit.getOnlinePlayers()) {
            codes.add(plugin.lang(p).code());
        }
        List<Entry> list = top(Type.PRESTIGE, plugin.settings().leaderboardSize);
        holograms.entrySet().removeIf(e -> {
            boolean stale = !codes.contains(e.getKey()) || !e.getValue().isValid()
                    || !e.getValue().getWorld().equals(loc.getWorld()) || e.getValue().getLocation().distanceSquared(loc) > 0.01;
            if (stale && e.getValue().isValid()) {
                e.getValue().remove();
            }
            return stale;
        });
        for (String code : codes) {
            Component text = text(plugin.languages().get(code), list);
            TextDisplay d = holograms.get(code);
            if (d == null) {
                holograms.put(code, loc.getWorld().spawn(loc, TextDisplay.class, t -> {
                    t.setPersistent(false);
                    t.setVisibleByDefault(false);
                    t.setBillboard(Display.Billboard.CENTER);
                    t.setBackgroundColor(Color.fromARGB(0x90101426));
                    t.setShadowed(true);
                    t.setLineWidth(400);
                    t.text(text);
                }));
            } else if (!text.equals(d.text())) {
                d.text(text);
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            show(p);
        }
    }

    private Component text(Lang lang, List<Entry> list) {
        StringBuilder sb = new StringBuilder(lang.raw("hologram-title"));
        int rank = 1;
        for (Entry e : list) {
            sb.append("\n").append(lang.format("top-line", "rank", String.valueOf(rank++),
                    "player", Text.escape(e.name() == null ? "?" : e.name()), "value", value(lang, Type.PRESTIGE, e)));
        }
        if (list.isEmpty()) {
            sb.append("\n").append(lang.raw("top-empty"));
        }
        return Text.mm(sb.toString());
    }

    /** Shows the player the hologram of their language (spawning it if nobody used that language yet). */
    public void refreshViewer(Player p) {
        if (hologramLocation() == null) {
            return;
        }
        if (!holograms.containsKey(plugin.lang(p).code())) {
            updateHologram();
            return;
        }
        show(p);
    }

    private void show(Player p) {
        String code = plugin.lang(p).code();
        for (Map.Entry<String, TextDisplay> e : holograms.entrySet()) {
            if (!e.getValue().isValid()) {
                continue;
            }
            if (e.getKey().equals(code)) {
                p.showEntity(plugin, e.getValue());
            } else {
                p.hideEntity(plugin, e.getValue());
            }
        }
    }

    private void removeEntity() {
        for (TextDisplay d : holograms.values()) {
            if (d.isValid()) {
                d.remove();
            }
        }
        holograms.clear();
    }

    public void shutdown() {
        removeEntity();
    }
}
