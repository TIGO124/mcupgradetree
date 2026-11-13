package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.tree.UpgradeNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab completion of /tigo (main names only, see {@link CommandAliases}). */
final class UpgradeTabCompleter {

    private static final List<String> PLAYER_SUBS = List.of("help", "info", "open", "chests", "respec", "prestige", "top",
            "menu", "daily");
    private static final List<String> ADMIN_SUBS = List.of("reload", "check", "reset", "set", "points", "cleanup", "npc",
            "debug", "admin");

    private final UpgradeTreePlugin plugin;

    UpgradeTabCompleter(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    List<String> complete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        boolean isAdmin = sender.hasPermission(UpgradeCommand.ADMIN);
        boolean isOp = sender instanceof Player op && op.isOp();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(PLAYER_SUBS);
            if (isAdmin) {
                subs.addAll(ADMIN_SUBS);
            }
            if (isOp) {
                subs.add("admin");
            }
            filter(subs, args[0], out);
            return out;
        }
        String sub = CommandAliases.sub(args[0]);
        String last = args[args.length - 1];
        if (args.length == 2) {
            switch (sub) {
                case "menu" -> filter(List.of("panel", "chest", "auto"), last, out);
                case "top" -> filter(isAdmin ? List.of("prestige", "levels", "place", "remove") : List.of("prestige", "levels"), last, out);
                case "prestige" -> filter(isAdmin ? List.of("confirm", "set") : List.of("confirm"), last, out);
                case "respec" -> {
                    List<String> opts = new ArrayList<>(List.of("confirm"));
                    if (isAdmin) {
                        opts.addAll(onlineNames());
                    }
                    filter(opts, last, out);
                }
                case "points" -> {
                    if (isAdmin) {
                        filter(List.of("give", "take", "set"), last, out);
                    }
                }
                case "cleanup" -> {
                    if (isAdmin) {
                        List<String> opts = new ArrayList<>(List.of("all"));
                        opts.addAll(onlineNames());
                        filter(opts, last, out);
                    }
                }
                case "debug" -> {
                    if (isAdmin) {
                        filter(List.of("preview", "look", "pos", "entities", "target"), last, out);
                    }
                }
                case "admin" -> {
                    if (isOp) {
                        filter(onlineNames(), last, out);
                    }
                }
                case "info", "set", "open" -> {
                    if (isAdmin) {
                        filter(onlineNames(), last, out);
                    }
                }
                case "chests" -> {
                    if (isAdmin) {
                        filter(List.of("give"), last, out);
                    }
                }
                default -> {
                }
            }
            return out;
        }
        if (!isAdmin) {
            return out;
        }
        String opt = CommandAliases.word(args[1]);
        if (args.length == 3) {
            switch (sub) {
                case "set" -> filter(nodeIds(), last, out);
                case "points", "debug" -> filter(onlineNames(), last, out);
                case "prestige", "chests" -> {
                    if (opt.equals("set") || opt.equals("give")) {
                        filter(onlineNames(), last, out);
                    }
                }
                default -> {
                }
            }
        } else if (args.length == 5 && sub.equals("chests") && opt.equals("give")) {
            filter(List.of("1", "5", "10"), last, out);
        } else if (args.length == 4) {
            if (sub.equals("chests") && opt.equals("give")) {
                List<String> ids = new ArrayList<>();
                for (var c : plugin.chestTypes().chests()) {
                    ids.add(c.id);
                }
                filter(ids, last, out);
            } else if (sub.equals("set")) {
                UpgradeNode n = plugin.trees().node(args[2]);
                if (n != null) {
                    List<String> lv = new ArrayList<>();
                    for (int i = 0; i <= n.maxLevel; i++) {
                        lv.add(String.valueOf(i));
                    }
                    filter(lv, last, out);
                }
            } else if (sub.equals("debug") && (opt.equals("look") || opt.equals("pos"))) {
                filter(nodeIds(), last, out);
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
