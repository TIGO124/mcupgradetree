package com.upgradetree.command;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.config.Languages;
import com.upgradetree.data.PlayerData;
import com.upgradetree.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tigo language [code|default] (own language) and /tigo serverlanguage &lt;code&gt; (admins). */
final class LanguageCommands {

    private final UpgradeTreePlugin plugin;

    LanguageCommands(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    private String list() {
        return String.join(", ", plugin.languages().codes());
    }

    void player(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.lang().msg("players-only"));
            return;
        }
        if (!p.hasPermission(UpgradeCommand.USE)) {
            p.sendMessage(plugin.lang(p).msg("no-permission"));
            return;
        }
        PlayerData d = plugin.data().get(p);
        if (args.length < 2) {
            Lang lang = plugin.lang(p);
            p.sendMessage(lang.msg("language-current", "language", lang.code(),
                    "server", plugin.lang().code(), "list", list()));
            p.sendMessage(lang.plain("usage-language"));
            return;
        }
        if (CommandAliases.word(args[1]).equals("default")) {
            d.language("");
            changed(p, d);
            p.sendMessage(plugin.lang(p).msg("language-reset", "language", plugin.lang().code()));
            return;
        }
        String code = Languages.normalize(args[1]);
        if (!plugin.languages().has(code)) {
            p.sendMessage(plugin.lang(p).msg("language-unknown", "language", Text.escape(args[1]), "list", list()));
            return;
        }
        d.language(code);
        changed(p, d);
        p.sendMessage(plugin.lang(p).msg("language-set", "language", code));
    }

    /** Saves the choice, closes the menu (drawn again in the new language) and renames handed-out texts. */
    private void changed(Player p, PlayerData d) {
        plugin.data().save(d);
        plugin.menus().close(p, true);
        plugin.languageChanged(p);
    }

    void server(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.lang(sender).msg("language-current", "language", plugin.lang(sender).code(),
                    "server", plugin.lang().code(), "list", list()));
            sender.sendMessage(plugin.lang(sender).plain("usage-serverlanguage"));
            return;
        }
        String code = Languages.normalize(args[1]);
        if (!plugin.languages().has(code)) {
            sender.sendMessage(plugin.lang(sender).msg("language-unknown", "language", Text.escape(args[1]), "list", list()));
            return;
        }
        boolean ok = plugin.setServerLanguage(code);
        sender.sendMessage(ok ? plugin.lang(sender).msg("server-language-set", "language", code)
                : plugin.lang(sender).msg("reload-failed"));
    }
}
