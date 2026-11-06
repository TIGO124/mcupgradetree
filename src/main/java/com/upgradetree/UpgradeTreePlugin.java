package com.upgradetree;

import com.upgradetree.api.UpgradeTreeAPI;
import com.upgradetree.command.UpgradeCommand;
import com.upgradetree.config.Lang;
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
import com.upgradetree.menu.MenuListener;
import com.upgradetree.menu.MenuManager;
import com.upgradetree.service.Leaderboard;
import com.upgradetree.service.ProgressService;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.tree.NodeEffect;
import com.upgradetree.tree.TreeLoader;
import com.upgradetree.tree.TreeRegistry;
import com.upgradetree.tree.UpgradeNode;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
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
    private Lang lang;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Lang.saveDefaults(this);
        if (!TreeLoader.migrateLegacy(getDataFolder(), getLogger()) && !new File(getDataFolder(), TreeLoader.FOLDER).exists()) {
            for (String t : DEFAULT_TREES) {
                saveResource(TreeLoader.FOLDER + "/" + t + ".yml", false);
            }
        }
        loadSettings();
        data = new DataStore(createStorage(), getLogger(), () -> settings == null ? 0 : settings.startingPoints);
        currency = createCurrency(settings);
        loadTrees();

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
        pm.registerEvents(new AccessListener(this), this);

        PluginCommand cmd = getCommand("yukselt");
        if (cmd != null) {
            UpgradeCommand executor = new UpgradeCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, menus::tick, 1L, 1L);
        scheduler.runTaskTimer(this, effects::tickPotions, 100L, 100L);
        scheduler.runTaskTimer(this, notifier::tick, 100L, 100L);
        scheduler.runTaskTimer(this, points::tickPlaytime, 1200L, 1200L);
        scheduler.runTaskTimer(this, () -> data.autosave(), settings.autosaveSeconds * 20L, settings.autosaveSeconds * 20L);
        scheduler.runTaskTimer(this, leaderboard::updateHologram, 100L, 1200L);
        scheduler.runTaskTimer(this, leaderboard::refreshAsync, 20L, 20L * 300);

        if (pm.getPlugin("PlaceholderAPI") != null && PapiHook.register(this)) {
            getLogger().info("PlaceholderAPI bağlandı (%upgradetree_...%).");
        }
        if (bedrock.active()) {
            getLogger().info("Geyser/Floodgate bulundu: Bedrock oyuncular sandık menüsünü kullanacak.");
        }
        UpgradeTreeAPI.init(this);

        for (Player p : getServer().getOnlinePlayers()) {
            menus.recover(p);
            data.get(p);
            effects.apply(p);
        }
        getLogger().info("Yükseltme ağacı hazır: " + trees.trees().size() + " ağaç, " + trees.nodes().size()
                + " düğüm, para birimi: " + settings.currencyType + ", veri: " + data.storage().describe() + ". Oyunda /yukselt yaz.");
        if (settings.cleanupMode) {
            getLogger().warning("cleanup-mode açık: oyunculardaki tüm eklenti bonusları kaldırılıyor, yükseltmeler etkisiz.");
        }
    }

    @Override
    public void onDisable() {
        if (menus != null) {
            menus.closeAll(true);
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
        lang = new Lang(this, settings.language, getConfig());
    }

    private void loadTrees() {
        try {
            trees = TreeLoader.load(getDataFolder(), getLogger());
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Ağaçlar okunamadı", ex);
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

    /** /yukselt yenile */
    public boolean reloadAll() {
        if (menus != null) {
            menus.closeAll(false);
        }
        loadSettings();
        currency = createCurrency(settings);
        TreeRegistry before = trees;
        loadTrees();
        boolean ok = !trees.trees().isEmpty() || before.trees().isEmpty();
        for (Player p : getServer().getOnlinePlayers()) {
            effects.apply(p);
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
            getLogger().log(Level.SEVERE, "Veritabanına bağlanılamadı (" + type + "), YAML kullanılıyor: " + e.getMessage());
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
            getLogger().info(all.size() + " oyuncunun verisi YAML'dan " + sql.describe() + " veritabanına aktarıldı.");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "YAML verileri aktarılamadı", e);
        }
    }

    private Currency createCurrency(Settings s) {
        return switch (s.currencyType) {
            case "VAULT" -> new VaultCurrency(s.currencyName, getLogger());
            case "POINTS" -> new PointsCurrency(data, s.currencyName);
            default -> new XpCurrency(s.currencyName);
        };
    }

    public String namespace() {
        return getName().toLowerCase(Locale.ROOT);
    }

    public Settings settings() {
        return settings;
    }

    public Lang lang() {
        return lang;
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
}
