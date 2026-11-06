package com.upgradetree.currency;

import com.upgradetree.util.Text;
import org.bukkit.entity.Player;

/** Pays with vanilla experience levels. */
public final class XpCurrency implements Currency {

    private final String name;

    public XpCurrency(String name) {
        this.name = name;
    }

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
    public String format(double amount) {
        return formatShort(amount) + " " + name;
    }

    @Override
    public String formatShort(double amount) {
        return Text.num(Math.ceil(amount - 1e-9));
    }
}
