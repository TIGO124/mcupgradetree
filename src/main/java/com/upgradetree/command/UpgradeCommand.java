package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.data.PlayerData;
import com.upgradetree.gui.AdminPanel;
import com.upgradetree.gui.AdminPlayerPage;
import com.upgradetree.service.Leaderboard;
import com.upgradetree.service.ProgressService;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Text;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * /tigo – the only root command: opens the menu; every feature is a sub command
 * (/tigo info, /tigo chests, /tigo reload ...). Old Turkish sub command spellings still work.
 */
public final class UpgradeCommand implements BasicCommand {

    public static final String LABEL = "tigo";
    public static final List<String> ALIASES = List.of();
    static final String ADMIN = "upgradetree.admin";
    static final String USE = "upgradetree.use";

    private final UpgradeTreePlugin plugin;
    private final AdminCommands admin;
    private final LanguageCommands language;
    private final ChestCommands chests;
    private final UpgradeTabCompleter tab;

    public UpgradeCommand(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
        this.admin = new AdminCommands(plugin);
        this.language = new LanguageCommands(plugin);
        this.chests = new ChestCommands(plugin, admin);
        this.tab = new UpgradeTabCompleter(plugin);
    }

    /** "/execute as &lt;player&gt; run tigo" (command blocks, NPC plugins) acts for that player. */
    private static CommandSender sender(CommandSourceStack stack) {
        return stack.getExecutor() instanceof Player p ? p : stack.getSender();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        run(sender(stack), args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return tab.complete(sender(stack), args.length == 0 ? new String[] {""} : args);
    }

    private Lang lang(CommandSender sender) {
        return plugin.lang(sender);
    }

    void run(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(lang(sender).msg("players-only"));
            } else if (!p.hasPermission(USE)) {
                p.sendMessage(lang(p).msg("no-permission"));
            } else {
                plugin.menus().toggle(p);
            }
            return;
        }
        switch (CommandAliases.sub(args[0])) {
            case "help" -> help(sender);
            case "info" -> info(sender, args);
            case "open" -> open(sender, args);
            case "reload" -> {
                if (isAdmin(sender)) {
                    admin.reload(sender);
                }
            }
            case "check" -> {
                if (isAdmin(sender)) {
                    admin.check(sender);
                }
            }
            case "respec" -> respec(sender, args);
            case "set" -> {
                if (isAdmin(sender)) {
                    admin.set(sender, args);
                }
            }
            case "points" -> {
                if (isAdmin(sender)) {
                    admin.points(sender, args);
                }
            }
            case "prestige" -> prestige(sender, args);
            case "top" -> top(sender, args);
            case "menu" -> menuMode(sender, args);
            case "daily" -> {
                if (sender instanceof Player p) {
                    plugin.points().claimDaily(p, true);
                } else {
                    sender.sendMessage(lang(sender).msg("players-only"));
                }
            }
            case "cleanup" -> {
                if (isAdmin(sender)) {
                    admin.cleanup(sender, args.length >= 2 ? args[1] : (sender instanceof Player p ? p.getName() : "all"));
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
            case "admin" -> adminPanel(sender, args);
            case "chests" -> chests.run(sender, args);
            case "language" -> language.player(sender, args);
            case "serverlanguage" -> {
                if (isAdmin(sender)) {
                    language.server(sender, args);
                }
            }
            default -> help(sender);
        }
    }

    /** /tigo admin [player] – operators only. */
    private void adminPanel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p) || !p.isOp()) {
            sender.sendMessage(lang(sender).msg("admin-only-op"));
            return;
        }
        if (args.length >= 2) {
            AdminCommands.Target t = admin.require(sender, args[1]);
            if (t != null) {
                AdminPlayerPage.open(plugin, p, t.uuid(), t.name(), t.online() == null ? admin.data(t) : null);
            }
            return;
        }
        AdminPanel.open(plugin, p);
    }

    private boolean isAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN)) {
            return true;
        }
        sender.sendMessage(lang(sender).msg("no-permission"));
        return false;
    }

    private void help(CommandSender sender) {
        Lang lang = lang(sender);
        for (String line : lang.rawList("help")) {
            sender.sendMessage(Text.mm(line));
        }
        if (sender.hasPermission(ADMIN)) {
            for (String line : lang.rawList("help-admin")) {
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
                sender.sendMessage(lang(sender).msg("player-not-found", "player", Text.escape(args[1])));
                return;
            }
            plugin.menus().open(p);
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang(sender).msg("players-only"));
        } else if (!p.hasPermission(USE)) {
            p.sendMessage(lang(p).msg("no-permission"));
        } else {
            plugin.menus().open(p);
        }
    }

    /** /tigo info [player]: the upgrades, then the command list. */
    private void info(CommandSender sender, String[] args) {
        Lang lang = lang(sender);
        AdminCommands.Target t;
        if (args.length >= 2) {
            if (!sender.hasPermission(ADMIN) && !(sender instanceof Player self && self.getName().equalsIgnoreCase(args[1]))) {
                sender.sendMessage(lang.msg("no-permission"));
                return;
            }
            t = admin.require(sender, args[1]);
            if (t == null) {
                return;
            }
        } else if (sender instanceof Player p) {
            t = new AdminCommands.Target(p.getUniqueId(), p.getName(), p);
        } else {
            help(sender);
            return;
        }
        PlayerData d = admin.data(t);
        sender.sendMessage(lang.msg("info-header", "player", Text.escape(t.name()),
                "owned", String.valueOf(plugin.service().ownedAll(d)), "total", String.valueOf(plugin.trees().nodes().size())));
        boolean any = false;
        for (UpgradeNode n : plugin.trees().nodes()) {
            int lvl = Math.min(n.maxLevel, d.level(n.id));
            if (lvl > 0) {
                any = true;
                sender.sendMessage(lang.plain("info-line", "node", n.name(lang), "level", String.valueOf(lvl), "max", String.valueOf(n.maxLevel)));
            }
        }
        if (!any) {
            sender.sendMessage(lang.plain("info-empty"));
        }
        if ("POINTS".equals(plugin.settings().currencyType)) {
            sender.sendMessage(lang.plain("info-points", "currency", Text.escape(plugin.currencyName(lang)),
                    "amount", Text.num(d.points())));
        }
        if (d.prestige() > 0) {
            sender.sendMessage(lang.plain("info-prestige", "level", String.valueOf(d.prestige())));
        }
        sender.sendMessage(Text.mm(" "));
        help(sender);
    }

    /** /tigo respec [confirm] for players, /tigo reset &lt;player&gt; for admins. */
    private void respec(CommandSender sender, String[] args) {
        boolean confirm = args.length >= 2 && CommandAliases.word(args[1]).equals("confirm");
        if (args.length >= 2 && !confirm) {
            if (isAdmin(sender)) {
                admin.reset(sender, args[1]);
            }
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang(sender).msg("players-only"));
            return;
        }
        Lang lang = lang(p);
        ProgressService ps = plugin.progress();
        PlayerData d = plugin.data().get(p);
        if (!confirm) {
            if (!plugin.settings().respecEnabled) {
                p.sendMessage(lang.msg("respec-disabled"));
            } else {
                p.sendMessage(lang.msg("respec-confirm", "refund", Text.escape(plugin.money(lang, ps.refund(d)))));
                p.sendMessage(lang.plain("respec-confirm-hint"));
            }
            return;
        }
        double refund = ps.refund(d);
        switch (ps.respec(p)) {
            case DONE -> {
                plugin.menus().refresh(p);
                p.sendMessage(lang.msg("respec-done", "refund", Text.escape(plugin.money(lang, refund))));
            }
            case COOLDOWN -> p.sendMessage(lang.msg("respec-cooldown", "time", Text.duration(ps.respecCooldownLeft(d))));
            case NOTHING -> p.sendMessage(lang.msg("respec-nothing"));
            case DISABLED -> p.sendMessage(lang.msg("respec-disabled"));
            case CANCELLED -> p.sendMessage(lang.msg("purchase-cancelled"));
        }
    }

    private void prestige(CommandSender sender, String[] args) {
        String opt = args.length >= 2 ? CommandAliases.word(args[1]) : "";
        if (opt.equals("set")) {
            if (isAdmin(sender)) {
                admin.prestigeSet(sender, args);
            }
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang(sender).msg("players-only"));
            return;
        }
        Lang lang = lang(p);
        PlayerData d = plugin.data().get(p);
        if (!opt.equals("confirm") && plugin.progress().prestigeReady(d, p)) {
            p.sendMessage(lang.msg("prestige-confirm"));
            p.sendMessage(lang.plain("prestige-confirm-hint"));
            return;
        }
        switch (plugin.progress().prestige(p)) {
            case DONE -> {
                plugin.menus().refresh(p);
                p.sendMessage(lang.msg("prestige-done", "level", String.valueOf(d.prestige())));
            }
            case MAX -> p.sendMessage(lang.msg("prestige-max"));
            case DISABLED -> p.sendMessage(lang.msg("prestige-disabled"));
            case NOT_READY -> p.sendMessage(lang.msg("prestige-not-ready",
                    "percent", String.valueOf(Math.round(plugin.service().progress(d, p) * 100)),
                    "required", Text.num(plugin.settings().prestigeRequiredPercent)));
            case CANCELLED -> p.sendMessage(lang.msg("purchase-cancelled"));
        }
    }

    private void top(CommandSender sender, String[] args) {
        String mode = args.length >= 2 ? CommandAliases.word(args[1]) : "prestige";
        if (mode.equals("place") || mode.equals("remove")) {
            if (isAdmin(sender)) {
                admin.hologram(sender, mode.equals("place"));
            }
            return;
        }
        Lang lang = lang(sender);
        Leaderboard.Type type = mode.equals("levels") ? Leaderboard.Type.LEVELS : Leaderboard.Type.PRESTIGE;
        int size = plugin.settings().leaderboardSize;
        sender.sendMessage(lang.msg("top-header", "size", String.valueOf(size),
                "type", lang.raw(type == Leaderboard.Type.PRESTIGE ? "top-type-prestige" : "top-type-levels")));
        List<Leaderboard.Entry> list = plugin.leaderboard().top(type, size);
        if (list.isEmpty()) {
            sender.sendMessage(lang.plain("top-empty"));
        }
        int rank = 1;
        for (Leaderboard.Entry e : list) {
            sender.sendMessage(lang.plain("top-line", "rank", String.valueOf(rank++),
                    "player", Text.escape(e.name() == null ? "?" : e.name()), "value", plugin.leaderboard().value(lang, type, e)));
        }
    }

    private void menuMode(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang(sender).msg("players-only"));
            return;
        }
        String mode = switch (args.length >= 2 ? CommandAliases.word(args[1]) : "") {
            case "panel" -> "PANEL";
            case "chest" -> "CHEST";
            default -> "";
        };
        PlayerData d = plugin.data().get(p);
        d.menuMode(mode);
        plugin.menus().close(p, true);
        Lang lang = lang(p);
        String shown = mode.equals("CHEST") ? lang.raw("menu-mode-chest")
                : mode.equals("PANEL") ? lang.raw("menu-mode-panel") : "AUTO";
        p.sendMessage(lang.msg("menu-mode-set", "mode", shown));
    }
}
