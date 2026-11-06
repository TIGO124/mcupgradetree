package com.upgradetree.hook;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.Leaderboard;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI placeholders. Only loaded when PlaceholderAPI is installed.
 * <pre>
 * %upgradetree_points%            %upgradetree_balance%         %upgradetree_prestige%
 * %upgradetree_owned%             %upgradetree_total%           %upgradetree_levels%
 * %upgradetree_progress%          %upgradetree_level_&lt;node&gt;%     %upgradetree_maxlevel_&lt;node&gt;%
 * %upgradetree_owned_&lt;tree&gt;%      %upgradetree_completed_&lt;tree&gt;% %upgradetree_affordable%
 * %upgradetree_top_&lt;n&gt;_name%      %upgradetree_top_&lt;n&gt;_prestige% %upgradetree_top_&lt;n&gt;_levels%
 * </pre>
 */
public final class PapiExpansion extends PlaceholderExpansion {

    private final UpgradeTreePlugin plugin;

    public PapiExpansion(UpgradeTreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "upgradetree";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TIGO124";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String p = params.toLowerCase(Locale.ROOT);
        if (p.startsWith("top_")) {
            return top(p);
        }
        if (player == null) {
            return "";
        }
        Player online = player.getPlayer();
        PlayerData d = online != null ? plugin.data().get(online)
                : plugin.data().peek(player.getUniqueId(), player.getName() == null ? "" : player.getName());
        switch (p) {
            case "points":
                return Text.num(d.points());
            case "balance":
                return online == null || !plugin.currency().available() ? "0"
                        : plugin.currency().formatShort(plugin.currency().balance(online));
            case "prestige":
                return String.valueOf(d.prestige());
            case "owned":
                return String.valueOf(plugin.service().ownedAll(d));
            case "total":
                return String.valueOf(plugin.trees().nodes().size());
            case "levels":
                return String.valueOf(plugin.service().totalLevels(d));
            case "progress":
                return String.valueOf(Math.round(plugin.service().progress(d, online) * 100));
            case "affordable":
                return online == null ? "0" : String.valueOf(plugin.service().affordable(online));
            default:
                break;
        }
        if (p.startsWith("level_") || p.startsWith("maxlevel_")) {
            UpgradeNode n = plugin.trees().node(params.substring(params.indexOf('_') + 1));
            if (n == null) {
                return "0";
            }
            return p.startsWith("level_") ? String.valueOf(Math.min(n.maxLevel, d.level(n.id))) : String.valueOf(n.maxLevel);
        }
        if (p.startsWith("owned_") || p.startsWith("completed_")) {
            UpgradeTree t = plugin.trees().tree(params.substring(params.indexOf('_') + 1));
            if (t == null) {
                return "0";
            }
            return String.valueOf(p.startsWith("owned_") ? plugin.service().owned(d, t) : plugin.service().maxed(d, t));
        }
        return null;
    }

    private String top(String p) {
        String[] parts = p.split("_");
        if (parts.length < 3) {
            return null;
        }
        int rank;
        try {
            rank = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        List<Leaderboard.Entry> list = plugin.leaderboard().top(Leaderboard.Type.PRESTIGE, Math.max(rank, 1));
        if (rank < 1 || rank > list.size()) {
            return "";
        }
        Leaderboard.Entry e = list.get(rank - 1);
        return switch (parts[2]) {
            case "name" -> e.name() == null ? "" : e.name();
            case "prestige" -> String.valueOf(e.prestige());
            case "levels" -> String.valueOf(e.levels());
            default -> null;
        };
    }
}
