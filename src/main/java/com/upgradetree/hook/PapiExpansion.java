package com.upgradetree.hook;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.chest.ChestType;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.effect.EffectManager;
import com.upgradetree.service.Leaderboard;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Text;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI placeholders ({@code %upgradetree_<name>%}). Only loaded when PlaceholderAPI is
 * installed. Ids are the keys of the tree and chest files ({@code sharp_sword}, {@code main},
 * {@code iron}); triggers are {@code sneak_swap}, {@code double_sneak} and {@code sneak_jump}.
 * <pre>
 * Player      points  balance  balance_formatted  currency  levels  max_levels  owned  maxed  total
 *             progress  affordable  chests  rank  rank_levels  respec_ready  respec_cooldown  daily_ready
 * Prestige    prestige  prestige_roman  prestige_max  prestige_ready
 * Abilities   ability_&lt;trigger&gt;  ability_&lt;trigger&gt;_cooldown
 * Upgrades    level_&lt;id&gt;  maxlevel_&lt;id&gt;  cost_&lt;id&gt;  state_&lt;id&gt;
 * Trees       owned_&lt;tree&gt;  completed_&lt;tree&gt;  total_&lt;tree&gt;  progress_&lt;tree&gt;
 * Chests      chests_&lt;chest&gt;  chest_price_&lt;chest&gt;
 * Leaderboard top_&lt;n&gt;_name|prestige|levels (by prestige)  toplevels_&lt;n&gt;_name|prestige|levels (by levels)
 * </pre>
 */
public final class PapiExpansion extends PlaceholderExpansion {

    /** Shown by /papi info upgradetree. */
    private static final List<String> PLACEHOLDERS = List.of(
            "%upgradetree_points%", "%upgradetree_balance%", "%upgradetree_balance_formatted%", "%upgradetree_currency%",
            "%upgradetree_levels%", "%upgradetree_max_levels%", "%upgradetree_owned%", "%upgradetree_maxed%",
            "%upgradetree_total%", "%upgradetree_progress%", "%upgradetree_affordable%", "%upgradetree_chests%",
            "%upgradetree_rank%", "%upgradetree_rank_levels%", "%upgradetree_respec_ready%",
            "%upgradetree_respec_cooldown%", "%upgradetree_daily_ready%",
            "%upgradetree_prestige%", "%upgradetree_prestige_roman%", "%upgradetree_prestige_max%",
            "%upgradetree_prestige_ready%",
            "%upgradetree_ability_<trigger>%", "%upgradetree_ability_<trigger>_cooldown%",
            "%upgradetree_level_<upgrade>%", "%upgradetree_maxlevel_<upgrade>%", "%upgradetree_cost_<upgrade>%",
            "%upgradetree_state_<upgrade>%",
            "%upgradetree_owned_<tree>%", "%upgradetree_completed_<tree>%", "%upgradetree_total_<tree>%",
            "%upgradetree_progress_<tree>%",
            "%upgradetree_chests_<chest>%", "%upgradetree_chest_price_<chest>%",
            "%upgradetree_top_<n>_name%", "%upgradetree_top_<n>_prestige%", "%upgradetree_top_<n>_levels%",
            "%upgradetree_toplevels_<n>_name%", "%upgradetree_toplevels_<n>_prestige%", "%upgradetree_toplevels_<n>_levels%");

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
    public @NotNull List<String> getPlaceholders() {
        return PLACEHOLDERS;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String p = params.toLowerCase(Locale.ROOT);
        // placeholders that need no player
        if (p.startsWith("top_")) {
            return top(Leaderboard.Type.PRESTIGE, p.substring(4));
        }
        if (p.startsWith("toplevels_")) {
            return top(Leaderboard.Type.LEVELS, p.substring(10));
        }
        switch (p) {
            case "currency" -> {
                return plugin.currencyName();
            }
            case "total" -> {
                return String.valueOf(plugin.trees().nodes().size());
            }
            case "max_levels" -> {
                return String.valueOf(plugin.trees().totalMaxLevels());
            }
            case "prestige_max" -> {
                return String.valueOf(plugin.settings().prestigeMax);
            }
            default -> {
            }
        }
        if (p.startsWith("total_")) {
            UpgradeTree t = plugin.trees().tree(p.substring(6));
            return t == null ? null : String.valueOf(t.nodes().size());
        }
        if (p.startsWith("chest_price_")) {
            ChestType c = plugin.chestTypes().chest(p.substring(12));
            return c == null ? null : plugin.money(c.price);
        }
        if (player == null) {
            return "";
        }
        Player online = player.getPlayer();
        PlayerData d = online != null ? plugin.data().get(online)
                : plugin.data().peek(player.getUniqueId(), player.getName() == null ? "" : player.getName());
        String value = stat(p, d, online);
        if (value != null) {
            return value;
        }
        if (p.startsWith("ability_")) {
            return ability(p.substring(8), online);
        }
        int cut = p.indexOf('_');
        return cut < 0 ? null : byId(p.substring(0, cut), p.substring(cut + 1), d, online);
    }

    /** Placeholders of the player as a whole, or null when {@code p} is not one of them. */
    private String stat(String p, PlayerData d, Player online) {
        UpgradeService svc = plugin.service();
        return switch (p) {
            case "points" -> Text.num(d.points());
            case "balance" -> online == null ? "0" : balance(online, false);
            case "balance_formatted" -> online == null ? plugin.money(0) : balance(online, true);
            case "levels" -> String.valueOf(svc.totalLevels(d));
            case "owned" -> String.valueOf(svc.ownedAll(d));
            case "maxed" -> String.valueOf(maxedAll(d));
            case "progress" -> String.valueOf(Math.round(svc.progress(d, online) * 100));
            case "affordable" -> online == null ? "0" : String.valueOf(svc.affordable(online));
            case "chests" -> String.valueOf(d.totalChests());
            case "rank" -> String.valueOf(plugin.leaderboard().rank(Leaderboard.Type.PRESTIGE, d.uuid));
            case "rank_levels" -> String.valueOf(plugin.leaderboard().rank(Leaderboard.Type.LEVELS, d.uuid));
            case "respec_ready" -> bool(plugin.settings().respecEnabled && plugin.progress().respecCooldownLeft(d) <= 0);
            case "respec_cooldown" -> Text.duration(plugin.progress().respecCooldownLeft(d));
            case "daily_ready" -> bool(plugin.points().dailyReady(d));
            case "prestige" -> String.valueOf(d.prestige());
            case "prestige_roman" -> Text.roman(d.prestige());
            case "prestige_ready" -> bool(plugin.progress().prestigeReady(d, online));
            default -> null;
        };
    }

    private String balance(Player online, boolean withName) {
        Currency c = plugin.currency();
        if (!c.available()) {
            return withName ? plugin.money(0) : "0";
        }
        double b = c.balance(online);
        return withName ? plugin.money(b) : c.formatShort(b);
    }

    private int maxedAll(PlayerData d) {
        int count = 0;
        for (UpgradeTree t : plugin.trees().trees()) {
            count += plugin.service().maxed(d, t);
        }
        return count;
    }

    /** level_, maxlevel_, cost_, state_ (upgrades), owned_, completed_, progress_ (trees), chests_ (chests). */
    private String byId(String kind, String id, PlayerData d, Player online) {
        UpgradeService svc = plugin.service();
        switch (kind) {
            case "level", "maxlevel", "cost", "state" -> {
                UpgradeNode n = plugin.trees().node(id);
                if (n == null) {
                    return kind.equals("state") ? "" : "0";
                }
                int level = Math.min(n.maxLevel, d.level(n.id));
                return switch (kind) {
                    case "level" -> String.valueOf(level);
                    case "maxlevel" -> String.valueOf(n.maxLevel);
                    case "cost" -> level >= n.maxLevel ? "" : Text.num(svc.cost(d, n));
                    default -> svc.state(d, online, n).name().toLowerCase(Locale.ROOT);
                };
            }
            case "owned", "completed", "progress" -> {
                UpgradeTree t = plugin.trees().tree(id);
                if (t == null) {
                    return "0";
                }
                return switch (kind) {
                    case "owned" -> String.valueOf(svc.owned(d, t));
                    case "completed" -> String.valueOf(svc.maxed(d, t));
                    default -> String.valueOf(Math.round(treeProgress(d, t) * 100));
                };
            }
            case "chests" -> {
                ChestType c = plugin.chestTypes().chest(id);
                return c == null ? "0" : String.valueOf(d.chests(c.id));
            }
            default -> {
                return null;
            }
        }
    }

    /** Share (0..1) of the levels of one tree the player owns. */
    private static double treeProgress(PlayerData d, UpgradeTree t) {
        int have = 0;
        for (UpgradeNode n : t.nodes()) {
            have += Math.min(n.maxLevel, d.level(n.id));
        }
        int total = t.totalMaxLevels();
        return total <= 0 ? 0 : (double) have / total;
    }

    /** ability_&lt;trigger&gt; (the ability's name) and ability_&lt;trigger&gt;_cooldown (seconds left). */
    private String ability(String rest, Player online) {
        boolean cooldown = rest.endsWith("_cooldown");
        String raw = cooldown ? rest.substring(0, rest.length() - 9) : rest;
        NodeEffect.Trigger trigger;
        try {
            trigger = NodeEffect.Trigger.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (online == null) {
            return cooldown ? "0" : "";
        }
        if (cooldown) {
            return String.valueOf(plugin.abilities().cooldownLeft(online, trigger));
        }
        EffectManager.Typed<NodeEffect.Ability> t = plugin.abilities().bound(online, trigger);
        return t == null ? "" : plugin.lang().raw("ability-" + t.effect().type().name());
    }

    /** "&lt;n&gt;_name", "&lt;n&gt;_prestige" or "&lt;n&gt;_levels" of a leaderboard. */
    private String top(Leaderboard.Type type, String rest) {
        int cut = rest.indexOf('_');
        if (cut < 0) {
            return null;
        }
        int rank;
        try {
            rank = Integer.parseInt(rest.substring(0, cut));
        } catch (NumberFormatException e) {
            return null;
        }
        String field = rest.substring(cut + 1);
        if (!field.equals("name") && !field.equals("prestige") && !field.equals("levels")) {
            return null;
        }
        List<Leaderboard.Entry> list = plugin.leaderboard().top(type, Math.max(rank, 1));
        if (rank < 1 || rank > list.size()) {
            return "";
        }
        Leaderboard.Entry e = list.get(rank - 1);
        return switch (field) {
            case "name" -> e.name() == null ? "" : e.name();
            case "prestige" -> String.valueOf(e.prestige());
            default -> String.valueOf(e.levels());
        };
    }

    private static String bool(boolean b) {
        return b ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }
}
