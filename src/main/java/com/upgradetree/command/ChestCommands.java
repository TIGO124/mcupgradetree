package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.chest.ChestType;
import com.upgradetree.config.Lang;
import com.upgradetree.data.PlayerData;
import com.upgradetree.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tigo chests (opens the Chests section) and /tigo chests give &lt;player&gt; &lt;chest&gt; [amount] (admins). */
final class ChestCommands {

    private final UpgradeTreePlugin plugin;
    private final AdminCommands admin;

    ChestCommands(UpgradeTreePlugin plugin, AdminCommands admin) {
        this.plugin = plugin;
        this.admin = admin;
    }

    void run(CommandSender sender, String[] args) {
        if (args.length >= 2 && CommandAliases.word(args[1]).equals("give")) {
            give(sender, args);
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.lang(sender).msg("players-only"));
            return;
        }
        if (!p.hasPermission(UpgradeCommand.USE)) {
            p.sendMessage(plugin.lang(p).msg("no-permission"));
            return;
        }
        plugin.menus().openChests(p);
    }

    private void give(CommandSender sender, String[] args) {
        Lang lang = plugin.lang(sender);
        if (!sender.hasPermission(UpgradeCommand.ADMIN)) {
            sender.sendMessage(lang.msg("no-permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(lang.plain("usage-chests-give"));
            return;
        }
        AdminCommands.Target t = admin.require(sender, args[2]);
        if (t == null) {
            return;
        }
        ChestType c = plugin.chestTypes().chest(args[3]);
        if (c == null) {
            sender.sendMessage(lang.msg("chest-not-found", "chest", Text.escape(args[3])));
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(lang.msg("invalid-number", "value", Text.escape(args[4])));
                return;
            }
        }
        if (amount < 1 || amount > 1000) {
            sender.sendMessage(lang.msg("invalid-number", "value", String.valueOf(amount)));
            return;
        }
        PlayerData d = admin.data(t);
        plugin.chests().give(d, c, amount);
        sender.sendMessage(lang.msg("chest-given", "player", Text.escape(t.name()), "amount", String.valueOf(amount),
                "chest", c.name(lang)));
        if (t.online() != null && t.online() != sender) {
            Lang theirs = plugin.lang(t.online());
            t.online().sendMessage(theirs.msg("chest-received", "amount", String.valueOf(amount), "chest", c.name(theirs)));
        }
    }
}
