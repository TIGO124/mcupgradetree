package com.upgradetree.currency;

import com.upgradetree.util.Text;
import org.bukkit.entity.Player;

/** Pays with vanilla experience levels. */
public final class XpCurrency implements Currency {

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public double balance(Player player) {
        return player.getLevel();
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        int cost = (int) Math.ceil(amount - 1e-9);
        if (cost <= 0) {
            return true;
        }
        if (player.getLevel() < cost) {
            return false;
        }
        player.setLevel(player.getLevel() - cost);
        return true;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        int levels = (int) Math.floor(amount + 1e-9);
        if (levels > 0) {
            player.setLevel(player.getLevel() + levels);
        }
        return true;
    }

    @Override
    public String format(double amount, String name) {
        return formatShort(amount) + " " + name;
    }

    @Override
    public String formatShort(double amount) {
        return Text.num(Math.ceil(amount - 1e-9));
    }
}
