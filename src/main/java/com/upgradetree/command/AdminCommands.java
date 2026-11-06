package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Admin sub commands of /yukselt. */
final class AdminCommands {

    record Target(UUID uuid, String name, Player online) {
    }

    private final UpgradeTreePlugin plugin;

    AdminCommands(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    private Lang lang() {
        return plugin.lang();
    }

    Target find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online == null) {
            online = Bukkit.getPlayer(name);
        }
        if (online != null) {
            return new Target(online.getUniqueId(), online.getName(), online);
        }
        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(name);
        if (off != null && plugin.data().exists(off.getUniqueId())) {
            return new Target(off.getUniqueId(), off.getName() == null ? name : off.getName(), null);
        }
        return null;
    }

    PlayerData data(Target t) {
        return t.online() != null ? plugin.data().get(t.online()) : plugin.data().peek(t.uuid(), t.name());
    }

    Target require(CommandSender sender, String name) {
        Target t = find(name);
        if (t == null) {
            sender.sendMessage(lang().msg("player-not-found", "player", Text.escape(name)));
        }
        return t;
    }

    void reload(CommandSender sender) {
        boolean ok = plugin.reloadAll();
        sender.sendMessage(ok ? lang().msg("reloaded", "trees", String.valueOf(plugin.trees().trees().size()),
                "nodes", String.valueOf(plugin.trees().nodes().size())) : lang().msg("reload-failed"));
    }

    void check(CommandSender sender) {
        List<String> issues = plugin.trees().issues();
        if (issues.isEmpty()) {
            sender.sendMessage(lang().msg("check-ok", "trees", String.valueOf(plugin.trees().trees().size()),
                    "nodes", String.valueOf(plugin.trees().nodes().size())));
            return;
        }
        sender.sendMessage(lang().msg("check-header", "count", String.valueOf(issues.size())));
        for (String issue : issues) {
            sender.sendMessage(lang().plain("check-line", "issue", Text.escape(issue)));
        }
    }

    void reset(CommandSender sender, String name) {
        Target t = require(sender, name);
        if (t == null) {
            return;
        }
        PlayerData d = data(t);
        plugin.progress().adminReset(d, t.online());
        if (t.online() != null) {
            plugin.menus().refresh(t.online());
        }
        sender.sendMessage(lang().msg("reset-done", "player", Text.escape(t.name())));
    }

    void set(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.mm("<red>/yukselt ayarla <oyuncu> <düğüm> <seviye>"));
            return;
        }
        Target t = require(sender, args[1]);
        if (t == null) {
            return;
        }
        UpgradeNode n = plugin.trees().node(args[2]);
        if (n == null) {
            sender.sendMessage(lang().msg("node-not-found", "node", Text.escape(args[2])));
            return;
        }
        Integer level = integer(sender, args[3]);
        if (level == null) {
            return;
        }
        level = Math.max(0, Math.min(n.maxLevel, level));
        PlayerData d = data(t);
        d.setLevel(n.id, level);
        plugin.data().save(d);
        if (t.online() != null) {
            plugin.menus().refresh(t.online());
            plugin.effects().apply(t.online());
            if (level == 0) {
                plugin.effects().clearManagedPotions(t.online());
                plugin.effects().apply(t.online());
            }
        }
        plugin.leaderboard().touch(d);
        sender.sendMessage(lang().msg("set-done", "player", Text.escape(t.name()), "node", n.nameRaw, "level", String.valueOf(level)));
    }

    void points(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.mm("<red>/yukselt puan <ver|al|ayarla> <oyuncu> <miktar>"));
            return;
        }
        Target t = require(sender, args[2]);
        if (t == null) {
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[3].replace(',', '.'));
        } catch (NumberFormatException e) {
            sender.sendMessage(lang().msg("invalid-number", "value", Text.escape(args[3])));
            return;
        }
        PlayerData d = data(t);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "give", "ver", "ekle" -> d.points(d.points() + amount);
            case "take", "al", "cikar", "çıkar" -> d.points(d.points() - amount);
            case "set", "ayarla" -> d.points(amount);
            default -> {
                sender.sendMessage(Text.mm("<red>/yukselt puan <ver|al|ayarla> <oyuncu> <miktar>"));
                return;
            }
        }
        plugin.data().save(d);
        sender.sendMessage(lang().msg("points-done", "player", Text.escape(t.name()), "points", Text.num(d.points())));
        if (!"POINTS".equals(plugin.settings().currencyType)) {
            sender.sendMessage(lang().msg("points-not-used"));
        }
    }

    void prestigeSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.mm("<red>/yukselt prestij ayarla <oyuncu> <seviye>"));
            return;
        }
        Target t = require(sender, args[2]);
        Integer level = t == null ? null : integer(sender, args[3]);
        if (level == null) {
            return;
        }
        PlayerData d = data(t);
        d.prestige(level);
        plugin.data().save(d);
        if (t.online() != null) {
            plugin.menus().refresh(t.online());
            plugin.effects().apply(t.online());
        }
        plugin.leaderboard().touch(d);
        sender.sendMessage(lang().msg("prestige-set", "player", Text.escape(t.name()), "level", String.valueOf(d.prestige())));
    }

    void cleanup(CommandSender sender, String who) {
        int count = 0;
        if (who.equalsIgnoreCase("herkes") || who.equalsIgnoreCase("all") || who.equals("*")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.menus().close(p, true);
                if (plugin.effects().clearAll(p)) {
                    count++;
                }
            }
        } else {
            Player p = Bukkit.getPlayer(who);
            if (p == null) {
                sender.sendMessage(lang().msg("player-not-found", "player", Text.escape(who)));
                return;
            }
            plugin.menus().close(p, true);
            if (plugin.effects().clearAll(p)) {
                count++;
            }
        }
        sender.sendMessage(lang().msg("cleanup-done", "count", String.valueOf(count)));
    }

    void npc(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang().msg("players-only"));
            return;
        }
        var hit = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 6, 0.3,
                e -> !e.equals(p));
        Entity target = hit == null ? null : hit.getHitEntity();
        if (target == null) {
            p.sendMessage(lang().msg("npc-none"));
            return;
        }
        String tag = plugin.settings().entityTag;
        if (target.getScoreboardTags().contains(tag)) {
            target.removeScoreboardTag(tag);
            p.sendMessage(lang().msg("npc-untagged"));
        } else {
            target.addScoreboardTag(tag);
            p.sendMessage(lang().msg("npc-tagged"));
        }
    }

    void hologram(CommandSender sender, boolean place) {
        if (place) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(lang().msg("players-only"));
                return;
            }
            plugin.leaderboard().setHologram(p.getLocation());
            sender.sendMessage(lang().msg("hologram-set"));
        } else {
            plugin.leaderboard().setHologram(null);
            sender.sendMessage(lang().msg("hologram-removed"));
        }
    }

    /** /yukselt debug onizleme|bak|konum|varlik <oyuncu> [düğüm] */
    void debug(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.mm("<red>/yukselt debug <onizleme|bak|konum|varlik> <oyuncu> [düğüm]"));
            return;
        }
        Player p = Bukkit.getPlayer(args[2]);
        if (p == null) {
            sender.sendMessage(lang().msg("player-not-found", "player", Text.escape(args[2])));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "onizleme", "preview" -> {
                File out = new File(plugin.getDataFolder(), "debug/onizleme-" + p.getName() + ".png");
                try {
                    if (plugin.menus().snapshot(p, out, new File(plugin.getDataFolder(), "debug/textures"))) {
                        sender.sendMessage(lang().msg("debug-snapshot", "file", Text.escape(out.getPath())));
                    } else {
                        sender.sendMessage(lang().msg("debug-no-menu"));
                    }
                } catch (Exception e) {
                    sender.sendMessage(Text.mm("<red>" + Text.escape(String.valueOf(e))));
                }
            }
            case "bak", "look", "konum", "pos" -> {
                if (args.length < 4) {
                    sender.sendMessage(Text.mm("<red>/yukselt debug " + args[1] + " <oyuncu> <düğüm>"));
                    return;
                }
                UpgradeNode n = plugin.trees().node(args[3]);
                Location loc = n == null ? null : plugin.menus().nodeLocation(p, n.id);
                if (loc == null) {
                    sender.sendMessage(lang().msg("debug-no-menu"));
                    return;
                }
                if (args[1].startsWith("b") || args[1].startsWith("l")) {
                    plugin.menus().lookAt(p, n.id);
                }
                sender.sendMessage(Text.mm("<gray>NODE_POS " + n.id + " " + String.format(Locale.ROOT, "%.4f %.4f %.4f",
                        loc.getX(), loc.getY(), loc.getZ())));
            }
            case "varlik", "entities" -> sender.sendMessage(Text.mm("<gray>ENTITIES " + plugin.menus().entityCount(p)));
            case "hedef", "target" -> sender.sendMessage(Text.mm("<gray>TARGET " + Text.escape(plugin.menus().target(p))
                    + String.format(Locale.ROOT, " yaw=%.2f pitch=%.2f", p.getLocation().getYaw(), p.getLocation().getPitch())));
            default -> sender.sendMessage(Text.mm("<red>/yukselt debug <onizleme|bak|konum|varlik> <oyuncu> [düğüm]"));
        }
    }

    private Integer integer(CommandSender sender, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            sender.sendMessage(lang().msg("invalid-number", "value", Text.escape(raw)));
            return null;
        }
    }
}
