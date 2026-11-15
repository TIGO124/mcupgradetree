package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.lootbag.LootBagType;
import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tigo lootbag give &lt;player&gt; &lt;rarity&gt; [amount] (admins): loot bags into an online player's inventory. */
final class LootBagCommands {

    private static final int MAX_AMOUNT = 640;

    private final UpgradeTreePlugin plugin;

    LootBagCommands(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    void run(CommandSender sender, String[] args) {
        Lang lang = plugin.lang();
        if (!sender.hasPermission(UpgradeCommand.ADMIN)) {
            sender.sendMessage(lang.msg("no-permission"));
            return;
        }
        if (args.length < 4 || !CommandAliases.word(args[1]).equals("give")) {
            sender.sendMessage(lang.plain("usage-lootbag-give"));
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(lang.msg("player-not-found", "player", Text.escape(args[2])));
            return;
        }
        LootBagType t = plugin.lootBagTypes().type(args[3]);
        if (t == null) {
            sender.sendMessage(lang.msg("lootbag-not-found", "rarity", Text.escape(args[3])));
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
        if (amount < 1 || amount > MAX_AMOUNT) {
            sender.sendMessage(lang.msg("invalid-number", "value", String.valueOf(amount)));
            return;
        }
        plugin.lootBags().give(target, t, amount);
        sender.sendMessage(lang.msg("lootbag-given", "player", Text.escape(target.getName()), "amount", String.valueOf(amount),
                "bag", t.name));
        if (target != sender) {
            target.sendMessage(lang.msg("lootbag-received", "amount", String.valueOf(amount), "bag", t.name));
        }
    }
}
