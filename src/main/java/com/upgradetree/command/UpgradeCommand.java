package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Settings;
import com.upgradetree.data.PlayerData;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /yukselt [yardim|bilgi|yenile|sifirla|ayarla|puan|ac] */
public final class UpgradeCommand implements TabExecutor {

    private static final String ADMIN = "upgradetree.admin";
    private static final String USE = "upgradetree.use";

    private final UpgradeTreePlugin plugin;

    public UpgradeCommand(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    private record Target(UUID uuid, String name, Player online) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Settings s = plugin.settings();
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(s.msg("players-only"));
                return true;
            }
            if (!p.hasPermission(USE)) {
                p.sendMessage(s.msg("no-permission"));
                return true;
            }
            plugin.menus().toggle(p);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help", "yardim", "yardım", "?" -> help(sender);
            case "info", "bilgi" -> info(sender, args);
            case "open", "ac", "aç" -> open(sender, args);
            case "reload", "yenile" -> {
                if (admin(sender)) {
                    boolean ok = plugin.reloadAll();
                    sender.sendMessage(ok ? s.msg("reloaded", "nodes", String.valueOf(plugin.tree().nodes().size())) : s.msg("reload-failed"));
                }
            }
            case "reset", "sifirla", "sıfırla" -> reset(sender, args);
            case "set", "ayarla" -> set(sender, args);
            case "points", "puan" -> points(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission(ADMIN)) {
            return true;
        }
        sender.sendMessage(plugin.settings().msg("no-permission"));
        return false;
    }

    private void help(CommandSender sender) {
        for (String line : plugin.settings().rawList("help")) {
            sender.sendMessage(Text.mm(line));
        }
    }

    private Target find(String name) {
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

    private PlayerData data(Target t) {
        return t.online() != null ? plugin.data().get(t.online()) : plugin.data().peek(t.uuid(), t.name());
    }

    private void open(CommandSender sender, String[] args) {
        Settings s = plugin.settings();
        if (args.length >= 2) {
            if (!admin(sender)) {
                return;
            }
            Player p = Bukkit.getPlayer(args[1]);
            if (p == null) {
                sender.sendMessage(s.msg("player-not-found", "player", Text.escape(args[1])));
                return;
            }
            plugin.menus().open(p);
            return;
        }
        if (sender instanceof Player p) {
            if (p.hasPermission(USE)) {
                plugin.menus().open(p);
            } else {
                p.sendMessage(s.msg("no-permission"));
            }
        } else {
            sender.sendMessage(s.msg("players-only"));
        }
    }

    private void info(CommandSender sender, String[] args) {
        Settings s = plugin.settings();
        Target t;
        if (args.length >= 2) {
            if (!sender.hasPermission(ADMIN) && !(sender instanceof Player self && self.getName().equalsIgnoreCase(args[1]))) {
                sender.sendMessage(s.msg("no-permission"));
                return;
            }
            t = find(args[1]);
            if (t == null) {
                sender.sendMessage(s.msg("player-not-found", "player", Text.escape(args[1])));
                return;
            }
        } else if (sender instanceof Player p) {
            t = new Target(p.getUniqueId(), p.getName(), p);
        } else {
            sender.sendMessage(s.msg("players-only"));
            return;
        }
        PlayerData d = data(t);
        int owned = plugin.service().ownedNodes(d);
        sender.sendMessage(s.msg("info-header", "player", Text.escape(t.name()),
                "owned", String.valueOf(owned), "total", String.valueOf(plugin.tree().nodes().size())));
        boolean any = false;
        for (UpgradeNode n : plugin.tree().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl > 0) {
                any = true;
                sender.sendMessage(s.plain("info-line", "node", n.nameRaw, "level", String.valueOf(lvl), "max", String.valueOf(n.maxLevel)));
            }
        }
        if (!any) {
            sender.sendMessage(s.plain("info-empty"));
        }
        if ("POINTS".equals(s.currencyType)) {
            sender.sendMessage(Text.mm("<gray>" + Text.escape(s.currencyName) + ": <yellow>" + Text.num(d.points())));
        }
    }

    private void reset(CommandSender sender, String[] args) {
        Settings s = plugin.settings();
        if (!admin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.mm("<red>/yukselt sifirla <oyuncu>"));
            return;
        }
        Target t = find(args[1]);
        if (t == null) {
            sender.sendMessage(s.msg("player-not-found", "player", Text.escape(args[1])));
            return;
        }
        PlayerData d = data(t);
        d.clearLevels();
        plugin.data().save(d);
        if (t.online() != null) {
            plugin.menus().close(t.online(), true);
            plugin.effects().apply(t.online());
            plugin.effects().clearManagedPotions(t.online());
        }
        sender.sendMessage(s.msg("reset-done", "player", Text.escape(t.name())));
    }

    private void set(CommandSender sender, String[] args) {
        Settings s = plugin.settings();
        if (!admin(sender)) {
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(Text.mm("<red>/yukselt ayarla <oyuncu> <düğüm> <seviye>"));
            return;
        }
        Target t = find(args[1]);
        if (t == null) {
            sender.sendMessage(s.msg("player-not-found", "player", Text.escape(args[1])));
            return;
        }
        UpgradeNode n = plugin.tree().node(args[2]);
        if (n == null) {
            sender.sendMessage(s.msg("node-not-found", "node", Text.escape(args[2])));
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(s.msg("invalid-number", "value", Text.escape(args[3])));
            return;
        }
        level = Math.max(0, Math.min(n.maxLevel, level));
        PlayerData d = data(t);
        d.setLevel(n.id, level);
        plugin.data().save(d);
        if (t.online() != null) {
            plugin.menus().close(t.online(), true);
            plugin.effects().apply(t.online());
            if (level == 0) {
                plugin.effects().clearManagedPotions(t.online());
                plugin.effects().apply(t.online());
            }
        }
        sender.sendMessage(s.msg("set-done", "player", Text.escape(t.name()), "node", n.nameRaw, "level", String.valueOf(level)));
    }

    private void points(CommandSender sender, String[] args) {
        Settings s = plugin.settings();
        if (!admin(sender)) {
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(Text.mm("<red>/yukselt puan <ver|al|ayarla> <oyuncu> <miktar>"));
            return;
        }
        Target t = find(args[2]);
        if (t == null) {
            sender.sendMessage(s.msg("player-not-found", "player", Text.escape(args[2])));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[3].replace(',', '.'));
        } catch (NumberFormatException e) {
            sender.sendMessage(s.msg("invalid-number", "value", Text.escape(args[3])));
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
        sender.sendMessage(s.msg("points-done", "player", Text.escape(t.name()), "points", Text.num(d.points())));
        if (!"POINTS".equals(s.currencyType)) {
            sender.sendMessage(s.msg("points-not-used"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission(ADMIN);
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("yardim", "bilgi", "ac"));
            if (admin) {
                subs.addAll(List.of("yenile", "sifirla", "ayarla", "puan"));
            }
            filter(subs, args[0], out);
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!admin && !(sub.equals("bilgi") || sub.equals("info"))) {
            return out;
        }
        if (args.length == 2) {
            if (sub.equals("puan") || sub.equals("points")) {
                filter(List.of("ver", "al", "ayarla"), args[1], out);
            } else {
                filter(onlineNames(), args[1], out);
            }
        } else if (args.length == 3) {
            if (sub.equals("ayarla") || sub.equals("set")) {
                List<String> ids = new ArrayList<>();
                for (UpgradeNode n : plugin.tree().nodes()) {
                    ids.add(n.id);
                }
                filter(ids, args[2], out);
            } else if (sub.equals("puan") || sub.equals("points")) {
                filter(onlineNames(), args[2], out);
            }
        } else if (args.length == 4 && (sub.equals("ayarla") || sub.equals("set"))) {
            UpgradeNode n = plugin.tree().node(args[2]);
            if (n != null) {
                List<String> lv = new ArrayList<>();
                for (int i = 0; i <= n.maxLevel; i++) {
                    lv.add(String.valueOf(i));
                }
                filter(lv, args[3], out);
            }
        }
        return out;
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    private static void filter(List<String> options, String prefix, List<String> out) {
        String p = prefix.toLowerCase(Locale.ROOT);
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
    }
}
