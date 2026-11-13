package com.upgradetree.service;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.data.PlayerData;
import com.upgradetree.util.Palette;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Top players by prestige or total levels, with an optional floating hologram. */
public final class Leaderboard {

    public enum Type { PRESTIGE, LEVELS }

    public record Entry(UUID uuid, String name, int prestige, int levels) {
    }

    private final UpgradeTreePlugin plugin;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private TextDisplay hologram;

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

    /** Position of a player on a board (1 = first), 0 when not listed (no levels and no prestige). */
    public int rank(Type type, UUID uuid) {
        List<Entry> all = top(type, Integer.MAX_VALUE);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
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

    /** Spawns or refreshes the hologram (called every minute and after reloads). */
    public void updateHologram() {
        Location loc = hologramLocation();
        if (loc == null) {
            removeEntity();
            return;
        }
        if (!loc.isChunkLoaded()) {
            return;
        }
        if (hologram != null && (!hologram.isValid() || !hologram.getWorld().equals(loc.getWorld())
                || hologram.getLocation().distanceSquared(loc) > 0.01)) {
            removeEntity();
        }
        Component text = text(plugin.lang(), top(Type.PRESTIGE, plugin.settings().leaderboardSize));
        Color background = Color.fromARGB(Palette.alpha(plugin.settings().theme.background(), 0xA0));
        if (hologram == null) {
            hologram = loc.getWorld().spawn(loc, TextDisplay.class, t -> {
                t.setPersistent(false);
                t.setBillboard(Display.Billboard.CENTER);
                t.setBackgroundColor(background);
                t.setShadowed(true);
                t.setLineWidth(400);
                t.text(text);
            });
            return;
        }
        if (!text.equals(hologram.text())) {
            hologram.text(text);
        }
        // the menu style may have changed with a reload
        if (!background.equals(hologram.getBackgroundColor())) {
            hologram.setBackgroundColor(background);
        }
    }

    private Component text(Lang lang, List<Entry> list) {
        StringBuilder sb = new StringBuilder(lang.raw("hologram-title"));
        int rank = 1;
        for (Entry e : list) {
            sb.append('\n').append(lang.format("top-line", "rank", String.valueOf(rank++),
                    "player", Text.escape(e.name() == null ? "?" : e.name()), "value", value(lang, Type.PRESTIGE, e)));
        }
        if (list.isEmpty()) {
            sb.append('\n').append(lang.raw("top-empty"));
        }
        return Text.mm(sb.toString());
    }

    private void removeEntity() {
        if (hologram != null && hologram.isValid()) {
            hologram.remove();
        }
        hologram = null;
    }

    public void shutdown() {
        removeEntity();
    }
}
