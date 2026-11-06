package com.upgradetree.config;

import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Values read from config.yml (with safe defaults for every key). */
public final class Settings {

    private static final Map<String, Object> DEFAULT_MESSAGES = new LinkedHashMap<>();

    static {
        m("prefix", "<dark_gray>[<gradient:#FFE259:#FFA751>Yükseltme</gradient><dark_gray>]</dark_gray> ");
        m("no-permission", "<red>Bunu yapmak için yetkin yok.");
        m("players-only", "<red>Bu komutu sadece oyuncular kullanabilir.");
        m("cannot-open", "<red>Menüyü şu an açamazsın. <gray>(Yere bas; eğilirken, binekte, suda veya süzülürken açılmaz.)");
        m("opened-hint", "<gray>Nişangâhla bir yeteneğe bak, <white>sol tıkla</white> satın al. <white>Shift</white> ile kapat.");
        m("reloaded", "<green>Yapılandırma yeniden yüklendi. <gray>({nodes} düğüm)");
        m("reload-failed", "<red>Yeniden yükleme başarısız oldu, konsola bak.");
        m("player-not-found", "<red>Oyuncu bulunamadı: <white>{player}");
        m("node-not-found", "<red>Böyle bir düğüm yok: <white>{node}");
        m("invalid-number", "<red>Geçersiz sayı: <white>{value}");
        m("reset-done", "<green><white>{player}</white> adlı oyuncunun tüm yükseltmeleri sıfırlandı.");
        m("set-done", "<green><white>{player}</white>: <white>{node}</white> → seviye <yellow>{level}</yellow>");
        m("points-done", "<green><white>{player}</white> artık <yellow>{points}</yellow> puana sahip.");
        m("points-not-used", "<yellow>Not: Para birimi POINTS değil, puanlar şu an kullanılmıyor.");
        m("purchase-success", "<green>✔ <white>{node}</white> seviye <yellow>{level}</yellow> oldu!");
        m("purchase-max", "<gold>★ <white>{node}</white> maksimum seviyeye ulaştı!");
        m("purchase-maxed", "<yellow>Bu yükseltme zaten maksimum seviyede.");
        m("purchase-locked", "<red>Kilitli! Önce şunları aç: <white>{requires}");
        m("purchase-not-enough", "<red>Yetersiz {currency}! <gray>Gerekli: <white>{cost}</white> • Sende: <white>{balance}");
        m("currency-unavailable", "<red>Para birimi kullanılamıyor. <gray>(Vault ve bir ekonomi eklentisi gerekli.)");
        m("menu-footer", "<gray>Sol Tık <dark_gray>»</dark_gray> <white>Satın Al</white>     <dark_gray>•</dark_gray>     Shift <dark_gray>»</dark_gray> <white>Kapat</white>");
        m("menu-welcome-title", "<white><b>Yükseltme Ağacı");
        m("menu-welcome-desc", List.of("<gray>Bir yeteneğe nişan al,", "<gray>bilgileri burada görünür.", "<gray>Sol tıklayarak satın al!"));
        m("menu-progress", "<gray>Açılan: <white>{owned}</white><dark_gray>/</dark_gray><white>{total}</white>");
        m("menu-currency", "<gold>{symbol}</gold> <white><b>{amount}</b></white> <gray>{currency}");
        m("info-level", "<gray>Seviye <white>{level}</white><dark_gray>/</dark_gray><white>{max}");
        m("info-cost", "<gray>Bedel: <yellow>{cost}");
        m("info-cost-poor", "<gray>Bedel: <red>{cost}");
        m("info-free", "<gray>Bedel: <green>Ücretsiz");
        m("info-completed", "<gold>★ Tamamlandı");
        m("info-requires", "<red>Gerekli: <white>{requires}");
        m("button-buy", "<white><b>SATIN AL");
        m("button-poor", "<white><b>YETERSİZ");
        m("button-locked", "<white><b>KİLİTLİ");
        m("button-max", "<white><b>MAKSİMUM");
        m("badge-max", "<gold><b>MAX");
        m("info-header", "<gold><b>{player}</b> <gray>yükseltmeleri <dark_gray>({owned}/{total})");
        m("info-line", " <dark_gray>•</dark_gray> {node} <gray>{level}/{max}");
        m("info-empty", "<gray>Henüz hiçbir yükseltme alınmamış.");
        m("help", List.of(
                "<gold><b>Yükseltme Ağacı</b> <gray>komutları",
                "<yellow>/yukselt <gray>- Yükseltme ağacını aç/kapat",
                "<yellow>/yukselt bilgi [oyuncu] <gray>- Yükseltmeleri listele",
                "<yellow>/yukselt yenile <gray>- Yapılandırmayı yeniden yükle",
                "<yellow>/yukselt sifirla <oyuncu> <gray>- Tüm yükseltmeleri sıfırla",
                "<yellow>/yukselt ayarla <oyuncu> <düğüm> <seviye> <gray>- Seviye ayarla",
                "<yellow>/yukselt puan <ver|al|ayarla> <oyuncu> <miktar> <gray>- Puan yönet"));
    }

    private static void m(String k, Object v) {
        DEFAULT_MESSAGES.put(k, v);
    }

    public final String currencyType;
    public final String currencyName;
    public final String currencySymbol;
    public final double pointsPerMob;
    public final double pointsPerPlayer;
    public final double startingPoints;
    public final double menuDistance;
    public final double menuScale;
    public final boolean closeOnDamage;
    public final boolean hideHand;
    public final boolean sounds;
    public final boolean particles;
    public final boolean animations;
    public final boolean glowOnHover;
    public final Set<Material> doubleDropBlocks;
    private final Map<String, Object> messages = new LinkedHashMap<>(DEFAULT_MESSAGES);

    public Settings(FileConfiguration c, Logger log) {
        String type = c.getString("currency.type", "XP_LEVELS").trim().toUpperCase(Locale.ROOT);
        currencyType = switch (type) {
            case "VAULT", "MONEY", "PARA", "ECONOMY" -> "VAULT";
            case "POINTS", "POINT", "PUAN" -> "POINTS";
            default -> "XP_LEVELS";
        };
        currencyName = c.getString("currency.name", "Seviye");
        currencySymbol = c.getString("currency.symbol", "✦");
        pointsPerMob = c.getDouble("points.per-mob-kill", 1);
        pointsPerPlayer = c.getDouble("points.per-player-kill", 5);
        startingPoints = c.getDouble("points.starting-points", 0);
        menuDistance = clamp(c.getDouble("menu.distance", 1.6), 0.5, 4.0);
        menuScale = clamp(c.getDouble("menu.scale", 1.0), 0.4, 2.5);
        closeOnDamage = c.getBoolean("menu.close-on-damage", true);
        hideHand = c.getBoolean("menu.hide-hand", c.getBoolean("menu.hide-held-item", true));
        sounds = c.getBoolean("menu.sounds", true);
        particles = c.getBoolean("menu.particles", true);
        animations = c.getBoolean("menu.animations", true);
        glowOnHover = c.getBoolean("menu.glow-on-hover", true);

        Set<Material> blocks = EnumSet.noneOf(Material.class);
        for (String s : c.getStringList("effects.double-drop-blocks")) {
            Material mat = Material.matchMaterial(s.trim());
            if (mat == null) {
                log.warning("config.yml: bilinmeyen blok '" + s + "' (effects.double-drop-blocks)");
            } else {
                blocks.add(mat);
            }
        }
        doubleDropBlocks = blocks;

        ConfigurationSection sec = c.getConfigurationSection("messages");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                Object v = sec.get(key);
                if (v instanceof List<?> list) {
                    List<String> lines = new ArrayList<>();
                    for (Object o : list) {
                        lines.add(String.valueOf(o));
                    }
                    messages.put(key, lines);
                } else if (v != null) {
                    messages.put(key, String.valueOf(v));
                }
            }
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Raw MiniMessage string for a message key. */
    public String raw(String key) {
        Object v = messages.get(key);
        if (v instanceof List<?> list) {
            return String.join("\n", list.stream().map(String::valueOf).toList());
        }
        return v == null ? key : String.valueOf(v);
    }

    public List<String> rawList(String key) {
        Object v = messages.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return v == null ? List.of() : List.of(String.valueOf(v));
    }

    /** A chat message with prefix. */
    public Component msg(String key, String... kv) {
        return Text.mm(raw("prefix") + Text.apply(raw(key), kv));
    }

    /** A message without prefix (action bar, menu labels). */
    public Component plain(String key, String... kv) {
        return Text.mm(Text.apply(raw(key), kv));
    }
}
