package com.upgradetree;

import com.upgradetree.command.UpgradeCommand;
import com.upgradetree.config.Settings;
import com.upgradetree.currency.Currency;
import com.upgradetree.currency.PointsCurrency;
import com.upgradetree.currency.VaultCurrency;
import com.upgradetree.currency.XpCurrency;
import com.upgradetree.data.DataStore;
import com.upgradetree.effect.EffectManager;
import com.upgradetree.listener.PlayerListener;
import com.upgradetree.menu.MenuListener;
import com.upgradetree.menu.MenuManager;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.tree.UpgradeTree;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;
import java.util.logging.Level;

/**
 * UpgradeTree – a Roblox-style upgrade tree drawn with display entities, so it never
 * looks like a chest GUI.
 */
public final class UpgradeTreePlugin extends JavaPlugin {

    private Settings settings;
    private UpgradeTree tree;
    private DataStore data;
    private Currency currency;
    private UpgradeService service;
    private EffectManager effects;
    private MenuManager menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "tree.yml").exists()) {
            saveResource("tree.yml", false);
        }
        data = new DataStore(this, () -> settings == null ? 0 : settings.startingPoints);
        if (!loadConfigs()) {
            tree = UpgradeTree.empty();
        }
        service = new UpgradeService(this);
        effects = new EffectManager(this);
        menus = new MenuManager(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(effects, this);
        pm.registerEvents(new MenuListener(this, menus), this);
        pm.registerEvents(new PlayerListener(this), this);

        PluginCommand cmd = getCommand("yukselt");
        if (cmd != null) {
            UpgradeCommand executor = new UpgradeCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getScheduler().runTaskTimer(this, menus::tick, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, effects::tickPotions, 100L, 100L);

        for (Player p : getServer().getOnlinePlayers()) {
            menus.recover(p);
            data.get(p);
            effects.apply(p);
        }
        getLogger().info("Yükseltme ağacı hazır: " + tree.nodes().size() + " düğüm, para birimi: " + settings.currencyType
                + ". Oyunda /yukselt yaz.");
    }

    @Override
    public void onDisable() {
        if (menus != null) {
            menus.closeAll(true);
        }
        if (data != null) {
            data.shutdown();
        }
    }

    /** Loads config.yml and tree.yml. Returns false if tree.yml could not be read. */
    private boolean loadConfigs() {
        reloadConfig();
        Settings s = new Settings(getConfig(), getLogger());
        settings = s;
        currency = createCurrency(s);
        try {
            tree = UpgradeTree.load(new File(getDataFolder(), "tree.yml"), getLogger());
            return true;
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "tree.yml okunamadı: " + ex.getMessage());
            return false;
        }
    }

    /** /yukselt yenile */
    public boolean reloadAll() {
        if (menus != null) {
            menus.closeAll(false);
        }
        UpgradeTree old = tree;
        boolean ok = loadConfigs();
        if (!ok) {
            tree = old != null ? old : UpgradeTree.empty();
        }
        for (Player p : getServer().getOnlinePlayers()) {
            effects.apply(p);
        }
        return ok;
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

    public UpgradeTree tree() {
        return tree;
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

    public EffectManager effects() {
        return effects;
    }

    public MenuManager menus() {
        return menus;
    }
}
