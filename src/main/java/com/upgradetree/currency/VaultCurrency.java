package com.upgradetree.currency;

import com.upgradetree.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Pays with a Vault economy. Vault is accessed through reflection so the plugin
 * has no hard dependency on it.
 */
public final class VaultCurrency implements Currency {

    private final Logger log;
    private Object economy;
    private Method getBalance;
    private Method withdraw;
    private Method deposit;
    private Method format;
    private Method transactionSuccess;
    private long lastLookup;
    private boolean warned;

    public VaultCurrency(Logger log) {
        this.log = log;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean resolve() {
        if (economy != null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastLookup < 5000) {
            return false;
        }
        lastLookup = now;
        try {
            Class<?> eco = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration((Class) eco);
            if (rsp == null || rsp.getProvider() == null) {
                warnOnce("Vault bulundu ama kayıtlı bir ekonomi eklentisi yok (EssentialsX, CMI vb.).");
                return false;
            }
            Object provider = rsp.getProvider();
            getBalance = eco.getMethod("getBalance", OfflinePlayer.class);
            withdraw = eco.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            deposit = eco.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            try {
                format = eco.getMethod("format", double.class);
            } catch (NoSuchMethodException ignored) {
                format = null;
            }
            economy = provider;
            return true;
        } catch (ClassNotFoundException e) {
            warnOnce("currency.type VAULT seçili ama Vault yüklü değil.");
        } catch (Exception e) {
            warnOnce("Vault ekonomisine bağlanılamadı: " + e);
        }
        return false;
    }

    private void warnOnce(String msg) {
        if (!warned) {
            warned = true;
            log.warning(msg);
        }
    }

    @Override
    public boolean available() {
        return resolve();
    }

    @Override
    public double balance(Player player) {
        if (!resolve()) {
            return 0;
        }
        try {
            return ((Number) getBalance.invoke(economy, player)).doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (!resolve()) {
            return false;
        }
        try {
            if (balance(player) + 1e-9 < amount) {
                return false;
            }
            Object response = withdraw.invoke(economy, player, amount);
            if (response == null) {
                return false;
            }
            if (transactionSuccess == null) {
                transactionSuccess = response.getClass().getMethod("transactionSuccess");
            }
            return Boolean.TRUE.equals(transactionSuccess.invoke(response));
        } catch (Exception e) {
            log.warning("Vault ödeme hatası: " + e);
            return false;
        }
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (!resolve()) {
            return false;
        }
        try {
            Object response = deposit.invoke(economy, player, amount);
            if (response == null) {
                return false;
            }
            if (transactionSuccess == null) {
                transactionSuccess = response.getClass().getMethod("transactionSuccess");
            }
            return Boolean.TRUE.equals(transactionSuccess.invoke(response));
        } catch (Exception e) {
            log.warning("Vault iade hatası: " + e);
            return false;
        }
    }

    @Override
    public String format(double amount, String name) {
        String eco = economyFormat(amount);
        return eco != null ? eco : Text.num(amount) + " " + name;
    }

    @Override
    public String formatShort(double amount) {
        String eco = economyFormat(amount);
        return eco != null ? eco : Text.num(amount);
    }

    /** The economy plugin's own format ("$1,250"), or null. */
    private String economyFormat(double amount) {
        if (resolve() && format != null) {
            try {
                return String.valueOf(format.invoke(economy, amount));
            } catch (Exception ignored) {
                // fall back
            }
        }
        return null;
    }
}
