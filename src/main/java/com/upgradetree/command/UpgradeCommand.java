package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.Leaderboard;
import com.upgradetree.service.ProgressService;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /yukselt – opens the menu; sub commands for players and admins. */
public final class UpgradeCommand implements TabExecutor {

    private static final String ADMIN = "upgradetree.admin";
    private static final String USE = "upgradetree.use";

    private final UpgradeTreePlugin plugin;
    private final AdminCommands admin;

    public UpgradeCommand(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
        this.admin = new AdminCommands(plugin);
    }

    private Lang lang() {
        return plugin.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(lang().msg("players-only"));
            } else if (!p.hasPermission(USE)) {
                p.sendMessage(lang().msg("no-permission"));
            } else {
                plugin.menus().toggle(p);
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help", "yardim", "yardım", "?" -> help(sender);
            case "info", "bilgi" -> info(sender, args);
            case "open", "ac", "aç" -> open(sender, args);
            case "reload", "yenile" -> {
                if (isAdmin(sender)) {
                    admin.reload(sender);
                }
            }
            case "check", "kontrol" -> {
                if (isAdmin(sender)) {
                    admin.check(sender);
                }
            }
            case "reset", "sifirla", "sıfırla", "respec", "dagit", "dağıt" -> reset(sender, args);
            case "set", "ayarla" -> {
                if (isAdmin(sender)) {
                    admin.set(sender, args);
                }
            }
            case "points", "puan" -> {
                if (isAdmin(sender)) {
                    admin.points(sender, args);
                }
            }
            case "prestige", "prestij" -> prestige(sender, args);
            case "top", "liderler" -> top(sender, args);
            case "menu", "menü" -> menuMode(sender, args);
            case "daily", "gunluk", "günlük" -> {
                if (sender instanceof Player p) {
                    plugin.points().claimDaily(p, true);
                }
            }
            case "cleanup", "temizle" -> {
                if (isAdmin(sender)) {
                    admin.cleanup(sender, args.length >= 2 ? args[1] : (sender instanceof Player p ? p.getName() : "herkes"));
                }
            }
            case "npc" -> {
                if (isAdmin(sender)) {
                    admin.npc(sender);
                }
            }
            case "debug" -> {
                if (isAdmin(sender)) {
                    admin.debug(sender, args);
                }
            }
            default -> help(sender);
        }
        return true;
    }

    private boolean isAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN)) {
            return true;
        }
        sender.sendMessage(lang().msg("no-permission"));
        return false;
    }

    private void help(CommandSender sender) {
        for (String line : lang().rawList("help")) {
            sender.sendMessage(Text.mm(line));
        }
        if (sender.hasPermission(ADMIN)) {
            for (String line : lang().rawList("help-admin")) {
                sender.sendMessage(Text.mm(line));
            }
        }
    }

    private void open(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!isAdmin(sender)) {
                return;
            }
            Player p = Bukkit.getPlayer(args[1]);
            if (p == null) {
                sender.sendMessage(lang().msg("player-not-found", "player", Text.escape(args[1])));
                return;
            }
            plugin.menus().open(p);
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang().msg("players-only"));
        } else if (!p.hasPermission(USE)) {
            p.sendMessage(lang().msg("no-permission"));
        } else {
            plugin.menus().open(p);
        }
    }

    private void info(CommandSender sender, String[] args) {
        AdminCommands.Target t;
        if (args.length >= 2) {
            if (!sender.hasPermission(ADMIN) && !(sender instanceof Player self && self.getName().equalsIgnoreCase(args[1]))) {
                sender.sendMessage(lang().msg("no-permission"));
                return;
            }
            t = admin.require(sender, args[1]);
            if (t == null) {
                return;
            }
        } else if (sender instanceof Player p) {
            t = new AdminCommands.Target(p.getUniqueId(), p.getName(), p);
        } else {
            sender.sendMessage(lang().msg("players-only"));
            return;
        }
        PlayerData d = admin.data(t);
        sender.sendMessage(lang().msg("info-header", "player", Text.escape(t.name()),
                "owned", String.valueOf(plugin.service().ownedAll(d)), "total", String.valueOf(plugin.trees().nodes().size())));
        boolean any = false;
        for (UpgradeNode n : plugin.trees().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl > 0) {
                any = true;
                sender.sendMessage(lang().plain("info-line", "node", n.nameRaw, "level", String.valueOf(lvl), "max", String.valueOf(n.maxLevel)));
            }
        }
        if (!any) {
            sender.sendMessage(lang().plain("info-empty"));
        }
        if ("POINTS".equals(plugin.settings().currencyType)) {
            sender.sendMessage(lang().plain("info-points", "currency", Text.escape(plugin.settings().currencyName),
                    "amount", Text.num(d.points())));
        }
        if (d.prestige() > 0) {
            sender.sendMessage(lang().plain("info-prestige", "level", String.valueOf(d.prestige())));
        }
    }

    /** /yukselt sifirla [onayla] for players, /yukselt sifirla <oyuncu> for admins. */
    private void reset(CommandSender sender, String[] args) {
        boolean confirm = args.length >= 2 && (args[1].equalsIgnoreCase("onayla") || args[1].equalsIgnoreCase("confirm"));
        if (args.length >= 2 && !confirm) {
            if (isAdmin(sender)) {
                admin.reset(sender, args[1]);
            }
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang().msg("players-only"));
            return;
        }
        ProgressService ps = plugin.progress();
        PlayerData d = plugin.data().get(p);
        if (!confirm) {
            if (!plugin.settings().respecEnabled) {
                p.sendMessage(lang().msg("respec-disabled"));
            } else {
                p.sendMessage(lang().msg("respec-confirm", "refund", Text.escape(plugin.currency().format(ps.refund(d)))));
                p.sendMessage(Text.mm("<gray>» <yellow>/yukselt sifirla onayla"));
            }
            return;
        }
        double refund = ps.refund(d);
        switch (ps.respec(p)) {
            case DONE -> {
                plugin.menus().refresh(p);
                p.sendMessage(lang().msg("respec-done", "refund", Text.escape(plugin.currency().format(refund))));
            }
            case COOLDOWN -> p.sendMessage(lang().msg("respec-cooldown", "time", Text.duration(ps.respecCooldownLeft(d))));
            case NOTHING -> p.sendMessage(lang().msg("respec-nothing"));
            case DISABLED -> p.sendMessage(lang().msg("respec-disabled"));
            case CANCELLED -> p.sendMessage(lang().msg("purchase-cancelled"));
        }
    }

    private void prestige(CommandSender sender, String[] args) {
        if (args.length >= 2 && (args[1].equalsIgnoreCase("ayarla") || args[1].equalsIgnoreCase("set"))) {
            if (isAdmin(sender)) {
                admin.prestigeSet(sender, args);
            }
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang().msg("players-only"));
            return;
        }
        PlayerData d = plugin.data().get(p);
        boolean confirm = args.length >= 2 && (args[1].equalsIgnoreCase("onayla") || args[1].equalsIgnoreCase("confirm"));
        if (!confirm && plugin.progress().prestigeReady(d, p)) {
            p.sendMessage(lang().msg("prestige-confirm"));
            p.sendMessage(Text.mm("<gray>» <yellow>/yukselt prestij onayla"));
            return;
        }
        switch (plugin.progress().prestige(p)) {
            case DONE -> {
                plugin.menus().refresh(p);
                p.sendMessage(lang().msg("prestige-done", "level", String.valueOf(d.prestige())));
            }
            case MAX -> p.sendMessage(lang().msg("prestige-max"));
            case DISABLED -> p.sendMessage(lang().msg("prestige-disabled"));
            case NOT_READY -> p.sendMessage(lang().msg("prestige-not-ready",
                    "percent", String.valueOf(Math.round(plugin.service().progress(d, p) * 100)),
                    "required", Text.num(plugin.settings().prestigeRequiredPercent)));
            case CANCELLED -> p.sendMessage(lang().msg("purchase-cancelled"));
        }
    }

    private void top(CommandSender sender, String[] args) {
        String mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "prestij";
        if (mode.equals("koy") || mode.equals("place") || mode.equals("kaldir") || mode.equals("kaldır") || mode.equals("remove")) {
            if (isAdmin(sender)) {
                admin.hologram(sender, mode.equals("koy") || mode.equals("place"));
            }
            return;
        }
        Leaderboard.Type type = mode.startsWith("sev") || mode.startsWith("lev") ? Leaderboard.Type.LEVELS : Leaderboard.Type.PRESTIGE;
        int size = plugin.settings().leaderboardSize;
        sender.sendMessage(lang().msg("top-header", "size", String.valueOf(size),
                "type", lang().raw(type == Leaderboard.Type.PRESTIGE ? "top-type-prestige" : "top-type-levels")));
        List<Leaderboard.Entry> list = plugin.leaderboard().top(type, size);
        if (list.isEmpty()) {
            sender.sendMessage(lang().plain("top-empty"));
        }
        int rank = 1;
        for (Leaderboard.Entry e : list) {
            sender.sendMessage(lang().plain("top-line", "rank", String.valueOf(rank++),
                    "player", Text.escape(e.name() == null ? "?" : e.name()), "value", plugin.leaderboard().value(type, e)));
        }
    }

    private void menuMode(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang().msg("players-only"));
            return;
        }
        String arg = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        String mode = switch (arg) {
            case "panel", "3d", "display" -> "PANEL";
            case "sandik", "sandık", "chest", "gui" -> "CHEST";
            default -> "";
        };
        PlayerData d = plugin.data().get(p);
        d.menuMode(mode);
        plugin.menus().close(p, true);
        String shown = mode.equals("CHEST") ? lang().raw("menu-mode-chest")
                : mode.equals("PANEL") ? lang().raw("menu-mode-panel") : "AUTO";
        p.sendMessage(lang().msg("menu-mode-set", "mode", shown));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean isAdmin = sender.hasPermission(ADMIN);
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("yardim", "bilgi", "ac", "sifirla", "prestij", "top", "menu", "gunluk"));
            if (isAdmin) {
                subs.addAll(List.of("yenile", "kontrol", "ayarla", "puan", "temizle", "npc", "debug"));
            }
            filter(subs, args[0], out);
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "menu", "menü" -> filter(List.of("panel", "sandik", "otomatik"), args[1], out);
                case "top" -> filter(isAdmin ? List.of("prestij", "seviye", "koy", "kaldir") : List.of("prestij", "seviye"), args[1], out);
                case "prestij", "prestige" -> filter(isAdmin ? List.of("onayla", "ayarla") : List.of("onayla"), args[1], out);
                case "sifirla", "reset", "respec" -> {
                    List<String> opts = new ArrayList<>(List.of("onayla"));
                    if (isAdmin) {
                        opts.addAll(onlineNames());
                    }
                    filter(opts, args[1], out);
                }
                case "puan", "points" -> {
                    if (isAdmin) {
                        filter(List.of("ver", "al", "ayarla"), args[1], out);
                    }
                }
                case "temizle", "cleanup" -> {
                    if (isAdmin) {
                        List<String> opts = new ArrayList<>(List.of("herkes"));
                        opts.addAll(onlineNames());
                        filter(opts, args[1], out);
                    }
                }
                case "debug" -> {
                    if (isAdmin) {
                        filter(List.of("onizleme", "bak", "konum", "varlik"), args[1], out);
                    }
                }
                case "bilgi", "info", "ayarla", "set", "ac", "open" -> {
                    if (isAdmin) {
                        filter(onlineNames(), args[1], out);
                    }
                }
                default -> {
                }
            }
        } else if (args.length == 3 && isAdmin) {
            switch (sub) {
                case "ayarla", "set" -> filter(nodeIds(), args[2], out);
                case "puan", "points", "debug" -> filter(onlineNames(), args[2], out);
                case "prestij", "prestige" -> filter(onlineNames(), args[2], out);
                default -> {
                }
            }
        } else if (args.length == 4 && isAdmin) {
            if (sub.equals("ayarla") || sub.equals("set")) {
                UpgradeNode n = plugin.trees().node(args[2]);
                if (n != null) {
                    List<String> lv = new ArrayList<>();
                    for (int i = 0; i <= n.maxLevel; i++) {
                        lv.add(String.valueOf(i));
                    }
                    filter(lv, args[3], out);
                }
            } else if (sub.equals("debug")) {
                filter(nodeIds(), args[3], out);
            }
        }
        return out;
    }

    private List<String> nodeIds() {
        List<String> ids = new ArrayList<>();
        for (UpgradeNode n : plugin.trees().nodes()) {
            ids.add(n.id);
        }
        return ids;
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
