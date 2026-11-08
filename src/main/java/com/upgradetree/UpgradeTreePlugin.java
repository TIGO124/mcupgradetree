package com.upgradetree;

import com.upgradetree.api.UpgradeTreeAPI;
import com.upgradetree.chest.ChestManager;
import com.upgradetree.chest.ChestRegistry;
import com.upgradetree.command.UpgradeCommand;
import com.upgradetree.config.DefaultFiles;
import com.upgradetree.config.Lang;
import com.upgradetree.config.Languages;
import com.upgradetree.config.Settings;
import com.upgradetree.currency.Currency;
import com.upgradetree.currency.PointsCurrency;
import com.upgradetree.currency.VaultCurrency;
import com.upgradetree.currency.XpCurrency;
import com.upgradetree.data.DataStore;
import com.upgradetree.data.PlayerData;
import com.upgradetree.data.SqlStorage;
import com.upgradetree.data.Storage;
import com.upgradetree.data.YamlStorage;
import com.upgradetree.effect.AbilityManager;
import com.upgradetree.effect.CombatListener;
import com.upgradetree.effect.EffectManager;
import com.upgradetree.effect.GatherListener;
import com.upgradetree.effect.LifeListener;
import com.upgradetree.effect.PlacedBlocks;
import com.upgradetree.gui.ChestListener;
import com.upgradetree.hook.BedrockHook;
import com.upgradetree.hook.PapiHook;
import com.upgradetree.listener.AccessListener;
import com.upgradetree.listener.CombatTracker;
import com.upgradetree.listener.Notifier;
import com.upgradetree.listener.PlayerListener;
import com.upgradetree.listener.PointsListener;
import com.upgradetree.listener.QuickActionListener;
import com.upgradetree.menu.MenuListener;
import com.upgradetree.menu.MenuManager;
import com.upgradetree.service.Leaderboard;
import com.upgradetree.service.ProgressService;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.tree.TreeLoader;
import com.upgradetree.tree.TreeRegistry;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.util.Text;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

/**
 * UpgradeTree – a Roblox-style hexagon upgrade tree drawn with display entities, so it never
 * looks like a chest GUI. Bedrock players get a chest menu instead.
 */
public final class UpgradeTreePlugin extends JavaPlugin {

    public static final String[] DEFAULT_TREES = {"ana", "yetenekler", "toplayici"};

    private Settings settings;
    private Languages languages;
    private TreeRegistry trees = TreeRegistry.empty();
    private Set<Material> trackedBlocks = EnumSet.noneOf(Material.class);
    private DataStore data;
    private Currency currency;
    private UpgradeService service;
    private ProgressService progress;
    private EffectManager effects;
    private AbilityManager abilities;
    private MenuManager menus;
    private Leaderboard leaderboard;
    private BedrockHook bedrock;
    private CombatTracker combat;
    private PointsListener points;
    private ChestRegistry chestTypes = ChestRegistry.empty();
    private ChestManager chests;
    private AccessListener access;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Lang.saveDefaults(this);
        if (!TreeLoader.migrateLegacy(getDataFolder(), getLogger()) && !new File(getDataFolder(), TreeLoader.FOLDER).exists()) {
            for (String t : DEFAULT_TREES) {
                saveResource(TreeLoader.FOLDER + "/" + t + ".yml", false);
            }
        }
        if (!new File(getDataFolder(), ChestRegistry.FILE).exists()) {
            saveResource(ChestRegistry.FILE, false);
        }
        // untouched Turkish-only defaults of older versions get the texts of every language
        for (String t : DEFAULT_TREES) {
            DefaultFiles.upgrade(this, TreeLoader.FOLDER + "/" + t + ".yml");
        }
        DefaultFiles.upgrade(this, ChestRegistry.FILE);
        loadSettings();
        data = new DataStore(createStorage(), getLogger(), () -> settings == null ? 0 : settings.startingPoints);
        currency = createCurrency(settings);
        loadTrees();
        loadChests();

        service = new UpgradeService(this);
        progress = new ProgressService(this);
        effects = new EffectManager(this);
        abilities = new AbilityManager(this);
        menus = new MenuManager(this);
        leaderboard = new Leaderboard(this);
        bedrock = new BedrockHook();
        combat = new CombatTracker(this);
        PlacedBlocks placed = new PlacedBlocks(this);
        points = new PointsListener(this, placed);
        chests = new ChestManager(this);
        Notifier notifier = new Notifier(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(this, menus), this);
        pm.registerEvents(new ChestListener(), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new LifeListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new GatherListener(this, placed), this);
        pm.registerEvents(abilities, this);
        pm.registerEvents(combat, this);
        pm.registerEvents(points, this);
        pm.registerEvents(notifier, this);
        access = new AccessListener(this);
        pm.registerEvents(access, this);
        pm.registerEvents(new QuickActionListener(this), this);
        pm.registerEvents(chests, this);

        UpgradeCommand executor = new UpgradeCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                UpgradeCommand.LABEL, "UpgradeTree: upgrade tree and chests (/tigo help)", UpgradeCommand.ALIASES, executor));
        logQuickAction();

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, menus::tick, 1L, 1L);
        scheduler.runTaskTimer(this, chests::tick, 1L, 1L);
        scheduler.runTaskTimer(this, effects::tickPotions, 100L, 100L);
        scheduler.runTaskTimer(this, notifier::tick, 100L, 100L);
        scheduler.runTaskTimer(this, points::tickPlaytime, 1200L, 1200L);
        scheduler.runTaskTimer(this, () -> data.autosave(), settings.autosaveSeconds * 20L, settings.autosaveSeconds * 20L);
        scheduler.runTaskTimer(this, leaderboard::updateHologram, 100L, 1200L);
        scheduler.runTaskTimer(this, leaderboard::refreshAsync, 20L, 20L * 300);

        if (pm.getPlugin("PlaceholderAPI") != null && PapiHook.register(this)) {
            getLogger().info(log("log-papi"));
        }
        if (bedrock.active()) {
            getLogger().info(log("log-bedrock"));
        }
        UpgradeTreeAPI.init(this);

        for (Player p : getServer().getOnlinePlayers()) {
            menus.recover(p);
            data.get(p);
            effects.apply(p);
        }
        getLogger().info(log("log-ready", "trees", String.valueOf(trees.trees().size()), "nodes", String.valueOf(trees.nodes().size()),
                "chests", String.valueOf(chestTypes.chests().size()), "currency", settings.currencyType,
                "storage", data.storage().describe()));
        if (settings.cleanupMode) {
            getLogger().warning(log("log-cleanup"));
        }
    }

    @Override
    public void onDisable() {
        if (menus != null) {
            menus.closeAll(true);
        }
        if (chests != null) {
            chests.finishAll();
        }
        if (leaderboard != null) {
            leaderboard.shutdown();
        }
        if (data != null) {
            data.shutdown();
        }
    }

    private void loadSettings() {
        reloadConfig();
        settings = new Settings(getConfig(), getLogger());
        languages = new Languages(this, settings.language, getConfig());
    }

    /** Tells whether the Quick Actions key (registered by the bootstrapper) is ready. */
    private void logQuickAction() {
        try {
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG);
            boolean registered = registry.get(UpgradeTreeBootstrap.OPEN_KEY) != null;
            boolean tagged = registered && registry.hasTag(DialogTagKeys.QUICK_ACTIONS)
                    && registry.getTag(DialogTagKeys.QUICK_ACTIONS).contains(UpgradeTreeBootstrap.OPEN_DIALOG);
            if (tagged) {
                getLogger().info(log("log-quick-ready"));
            } else if (registered) {
                getLogger().info(log("log-quick-off"));
            } else {
                getLogger().warning(log("log-quick-failed"));
            }
        } catch (RuntimeException ex) {
            getLogger().warning(log("log-quick-error", "error", String.valueOf(ex)));
        }
    }

    private void loadTrees() {
        try {
            trees = TreeLoader.load(getDataFolder(), getLogger(), lang().code());
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, log("log-trees-failed"), ex);
            trees = TreeRegistry.empty();
        }
        Set<Material> tracked = EnumSet.noneOf(Material.class);
        for (UpgradeNode n : trees.nodes()) {
            for (NodeEffect e : n.effects) {
                if (e instanceof NodeEffect.DoubleDrop dd) {
                    tracked.addAll(dd.blocks());
                } else if (e instanceof NodeEffect.VeinMiner vm) {
                    tracked.addAll(vm.blocks());
                }
            }
        }
        trackedBlocks = tracked;
    }

    private void loadChests() {
        try {
            chestTypes = ChestRegistry.load(getDataFolder(), getLogger(), lang().code());
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, log("log-chests-failed"), ex);
            chestTypes = ChestRegistry.empty();
        }
    }

    /** /tigo reload */
    public boolean reloadAll() {
        if (menus != null) {
            menus.closeAll(false);
        }
        if (chests != null) {
            chests.finishAll();
        }
        loadSettings();
        currency = createCurrency(settings);
        TreeRegistry before = trees;
        loadTrees();
        loadChests();
        boolean ok = !trees.trees().isEmpty() || before.trees().isEmpty();
        for (Player p : getServer().getOnlinePlayers()) {
            effects.apply(p);
            languageChanged(p);
        }
        leaderboard.updateHologram();
        return ok;
    }

    private Storage createStorage() {
        String type = settings.storageType.toUpperCase(Locale.ROOT);
        File playersFolder = new File(getDataFolder(), "players");
        try {
            if (type.equals("SQLITE") || type.equals("MYSQL") || type.equals("MARIADB")) {
                SqlStorage sql = type.equals("SQLITE")
                        ? SqlStorage.sqlite(new File(getDataFolder(), "data.db"), settings.table, getLogger())
                        : SqlStorage.mysql(settings.mysqlHost, settings.mysqlPort, settings.mysqlDatabase, settings.mysqlUser,
                        settings.mysqlPassword, settings.mysqlParams, settings.table, getLogger());
                migrateYaml(playersFolder, sql);
                return sql;
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, log("log-db-failed", "type", type, "error", String.valueOf(e.getMessage())));
        }
        return new YamlStorage(playersFolder, getLogger());
    }

    /** Copies old YAML player files into an empty database once. */
    private void migrateYaml(File folder, SqlStorage sql) {
        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }
        try {
            if (!sql.isEmpty()) {
                return;
            }
            List<PlayerData.Snapshot> all = new YamlStorage(folder, getLogger()).loadAll();
            for (PlayerData.Snapshot s : all) {
                sql.save(s);
            }
            getLogger().info(log("log-migrated", "count", String.valueOf(all.size()), "storage", sql.describe()));
        } catch (Exception e) {
            getLogger().log(Level.WARNING, log("log-migrate-failed"), e);
        }
    }

    private Currency createCurrency(Settings s) {
        return switch (s.currencyType) {
            case "VAULT" -> new VaultCurrency(getLogger());
            case "POINTS" -> new PointsCurrency(data);
            default -> new XpCurrency();
        };
    }

    /** A console line in the server language (lang file "log-..." keys, tags removed). */
    private String log(String key, String... kv) {
        return languages == null ? key : Text.plain(lang().format(key, kv));
    }

    public String namespace() {
        return getName().toLowerCase(Locale.ROOT);
    }

    public Settings settings() {
        return settings;
    }

    /** Server language (console, broadcasts, hologram). */
    public Lang lang() {
        return languages.server();
    }

    /** The language of whoever reads the message: a player's own choice, else the server language. */
    public Lang lang(CommandSender viewer) {
        if (viewer instanceof Player p && data != null) {
            PlayerData d = data.cachedOrNull(p.getUniqueId());
            if (d != null) {
                return languages.get(d.language());
            }
        }
        return languages.server();
    }

    public Languages languages() {
        return languages;
    }

    /** /tigo serverlanguage: writes config.yml "language" and reloads everything. */
    public boolean setServerLanguage(String code) {
        if (!languages.has(code)) {
            return false;
        }
        reloadConfig(); // keep edits made to config.yml on disk since the last reload
        getConfig().set("language", code);
        saveConfig();
        return reloadAll();
    }

    public TreeRegistry trees() {
        return trees;
    }

    /** Blocks named by single effects (placed-block tracking must cover them too). */
    public Set<Material> trackedBlocks() {
        return trackedBlocks;
    }

    public DataStore data() {
        return data;
    }

    public Currency currency() {
        return currency;
    }

    /** An amount with the currency name in the reader's language ("5 Levels", "5 Seviye", "$5"). */
    public String money(Lang lang, double amount) {
        return currency.format(amount, settings.currencyName(lang));
    }

    /** The currency name in the reader's language. */
    public String currencyName(Lang lang) {
        return settings.currencyName(lang);
    }

    public UpgradeService service() {
        return service;
    }

    public ProgressService progress() {
        return progress;
    }

    public EffectManager effects() {
        return effects;
    }

    public AbilityManager abilities() {
        return abilities;
    }

    public MenuManager menus() {
        return menus;
    }

    public Leaderboard leaderboard() {
        return leaderboard;
    }

    public BedrockHook bedrock() {
        return bedrock;
    }

    public CombatTracker combat() {
        return combat;
    }

    public PointsListener points() {
        return points;
    }

    /** Chests sold in the Chests section (chests.yml). */
    public ChestRegistry chestTypes() {
        return chestTypes;
    }

    public ChestManager chests() {
        return chests;
    }

    /**
     * A player's language changed (own choice or the server language): texts that were handed out
     * once (menu item, leaderboard hologram) follow it. Open menus are closed by the caller.
     */
    public void languageChanged(Player p) {
        if (access != null) {
            access.giveItem(p);
        }
        if (leaderboard != null) {
            leaderboard.refreshViewer(p);
        }
    }
}
