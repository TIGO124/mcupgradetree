package com.upgradetree.gui;

import com.upgradetree.UpgradeTreePlugin;
import com.upgradetree.config.Lang;
import com.upgradetree.currency.Currency;
import com.upgradetree.data.PlayerData;
import com.upgradetree.service.ProgressService;
import com.upgradetree.service.UpgradeService;
import com.upgradetree.service.UpgradeService.State;
import com.upgradetree.tree.UpgradeNode;
import com.upgradetree.tree.UpgradeTree;
import com.upgradetree.util.Icons;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classic 6-row chest menu. Used for Bedrock players (display entities do not render there)
 * and for anyone who picks it with /tigo menu chest. Same rules and progressive reveal.
 */
public final class ChestMenu implements InventoryHolder {

    private static final int PER_PAGE = 45;
    private static final int PREV = 45;
    private static final int INFO = 46;
    private static final int RESPEC = 47;
    private static final int[] TABS = {48, 49, 50, 51};
    private static final int PRESTIGE = 52;
    private static final int NEXT = 53;

    private final UpgradeTreePlugin plugin;
    private final Player player;
    private final List<UpgradeTree> trees;
    private final Map<Integer, String> slots = new HashMap<>();
    private UpgradeTree tree;
    private Inventory inventory;
    private int page;
    private String confirmNode;
    private long confirmUntil;
    private long respecUntil;
    private long prestigeUntil;

    private ChestMenu(UpgradeTreePlugin plugin, Player player, List<UpgradeTree> trees, UpgradeTree start) {
        this.plugin = plugin;
        this.player = player;
        this.trees = trees;
        this.tree = start;
    }

    public static void open(UpgradeTreePlugin plugin, Player p, List<UpgradeTree> trees, UpgradeTree start) {
        ChestMenu m = new ChestMenu(plugin, p, trees, start);
        m.inventory = Bukkit.createInventory(m, 54, m.title());
        m.render();
        p.openInventory(m.inventory);
        p.playSound(p.getLocation(), "minecraft:block.chest.open", SoundCategory.MASTER, 0.5f, 1.4f);
    }

    public static boolean isOpen(Player p) {
        return p.getOpenInventory().getTopInventory().getHolder() instanceof ChestMenu;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private Lang lang() {
        return plugin.lang();
    }

    private Component title() {
        return lang().plain("gui-title", "tree", tree.tabRaw());
    }

    private List<UpgradeNode> visibleNodes() {
        PlayerData d = plugin.data().get(player);
        List<UpgradeNode> list = new ArrayList<>();
        for (UpgradeNode n : tree.nodes()) {
            if (plugin.service().visible(d, n)) {
                list.add(n);
            }
        }
        list.sort(Comparator.comparingInt((UpgradeNode n) -> tree.depth(n)).thenComparingDouble(n -> n.y).thenComparingDouble(n -> n.x));
        return list;
    }

    public void render() {
        inventory.clear();
        slots.clear();
        List<UpgradeNode> nodes = visibleNodes();
        int pages = Math.max(1, (nodes.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= nodes.size()) {
                break;
            }
            UpgradeNode n = nodes.get(idx);
            inventory.setItem(i, nodeItem(n));
            slots.put(i, n.id);
        }
        ItemStack filler = GuiItems.filler();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        if (page > 0) {
            inventory.setItem(PREV, named(new ItemStack(Material.ARROW), lang().plain("gui-prev"), List.of()));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT, named(new ItemStack(Material.ARROW), lang().plain("gui-next"), List.of()));
        }
        inventory.setItem(INFO, infoItem());
        if (plugin.settings().respecEnabled) {
            boolean confirm = System.currentTimeMillis() < respecUntil;
            inventory.setItem(RESPEC, named(new ItemStack(Material.GRINDSTONE),
                    lang().plain(confirm ? "button-respec-confirm" : "button-respec"),
                    List.of(lang().plain("respec-confirm", "refund",
                            Text.escape(plugin.money(plugin.progress().refund(plugin.data().get(player))))))));
        }
        boolean chests = plugin.chestTypes().active();
        if (trees.size() + (chests ? 1 : 0) > 1) {
            int treeTabs = treeTabs();
            for (int i = 0; i < treeTabs; i++) {
                UpgradeTree t = trees.get(i);
                ItemStack tab = named(Icons.of(t.tabIcon), t.tabName(), List.of(lang().plain("gui-tab-hint")));
                if (t == tree) {
                    tab.editMeta(m -> m.setEnchantmentGlintOverride(true));
                }
                inventory.setItem(TABS[i], tab);
            }
            if (chests) {
                inventory.setItem(TABS[treeTabs], named(new ItemStack(Material.CHEST), lang().plain("chests-tab"),
                        List.of(lang().plain("gui-tab-hint"))));
            }
        }
        if (plugin.progress().prestigeReady(plugin.data().get(player), player)) {
            boolean confirm = System.currentTimeMillis() < prestigeUntil;
            ItemStack star = named(new ItemStack(Material.NETHER_STAR),
                    lang().plain(confirm ? "button-prestige-confirm" : "button-prestige"), List.of(lang().plain("prestige-confirm")));
            star.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inventory.setItem(PRESTIGE, star);
        }
    }

    private ItemStack infoItem() {
        PlayerData d = plugin.data().get(player);
        Currency c = plugin.currency();
        List<Component> lore = new ArrayList<>();
        lore.add(lang().plain("menu-currency", "symbol", Text.symbol(plugin.settings().currencySymbol),
                "amount", Text.escape(c.available() ? c.formatShort(c.balance(player)) : "-"),
                "currency", Text.escape(plugin.currencyName())));
        lore.add(lang().plain("menu-progress", "done", String.valueOf(plugin.service().maxed(d, tree)),
                "total", String.valueOf(tree.nodes().size())));
        if (d.prestige() > 0) {
            lore.add(lang().plain("info-prestige", "level", String.valueOf(d.prestige())));
        }
        return named(new ItemStack(Material.SUNFLOWER), lang().plain("menu-welcome-title"), lore);
    }

    private ItemStack nodeItem(UpgradeNode n) {
        PlayerData d = plugin.data().get(player);
        UpgradeService svc = plugin.service();
        State st = svc.state(d, player, n);
        int lvl = Math.min(d.level(n.id), n.maxLevel);
        List<Component> lore = new ArrayList<>();
        lore.add(lang().plain("info-level", "level", String.valueOf(lvl), "max", String.valueOf(n.maxLevel)));
        for (String line : n.description()) {
            lore.add(Text.mm(line));
        }
        String bonus = n.bonus();
        if (st != State.LOCKED && st != State.BLOCKED && !bonus.isBlank()) {
            String next = lvl < n.maxLevel ? " <dim>→</dim> " + Text.apply(bonus, "value", Text.num(n.bonusValue(lvl + 1))) : "";
            lore.add(Text.mm("<info>" + Text.apply(bonus, "value", Text.num(n.bonusValue(lvl))) + next));
        }
        switch (st) {
            case MAXED -> lore.add(lang().plain("info-completed"));
            case LOCKED -> lore.add(lang().plain("info-requires", "requires",
                    String.join("<body>, </body>", svc.missing(d, player, n))));
            case BLOCKED -> {
                List<String> names = new ArrayList<>();
                for (UpgradeNode b : svc.blockers(d, n)) {
                    names.add(b.name());
                }
                lore.add(lang().plain("info-blocked", "nodes", String.join("<body>, </body>", names)));
            }
            default -> {
                double cost = svc.cost(d, n);
                Currency c = plugin.currency();
                boolean afford = cost <= 0 || (c.available() && c.balance(player) + 1e-9 >= cost);
                lore.add(cost <= 0 ? lang().plain("info-free")
                        : lang().plain(afford ? "info-cost" : "info-cost-poor", "cost", Text.escape(plugin.money(cost))));
                lore.add(Component.empty());
                lore.add(n.id.equals(confirmNode) && System.currentTimeMillis() < confirmUntil
                        ? lang().plain("button-confirm") : lang().plain("gui-buy-hint"));
            }
        }
        ItemStack it = named(Icons.of(n), Text.mm("<b>" + n.name()), lore);
        it.setAmount(Math.max(1, Math.min(64, lvl)));
        if (st == State.OWNED || st == State.MAXED) {
            it.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        return it;
    }

    private static ItemStack named(ItemStack it, Component name, List<Component> lore) {
        it.editMeta(m -> {
            m.itemName(name);
            List<Component> clean = new ArrayList<>();
            for (Component c : lore) {
                clean.add(c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            m.lore(clean);
        });
        return it;
    }

    // ------------------------------------------------------------------ clicks

    void click(int slot) {
        long now = System.currentTimeMillis();
        if (slot < PER_PAGE) {
            String id = slots.get(slot);
            UpgradeNode n = id == null ? null : plugin.trees().node(id);
            if (n != null) {
                buy(n, now);
            }
            return;
        }
        switch (slot) {
            case PREV -> page--;
            case NEXT -> page++;
            case RESPEC -> respec(now);
            case PRESTIGE -> prestige(now);
            default -> {
                int treeTabs = treeTabs();
                if (plugin.chestTypes().active() && treeTabs < TABS.length && TABS[treeTabs] == slot) {
                    ChestShopMenu.open(plugin, player);
                    return;
                }
                for (int i = 0; i < treeTabs; i++) {
                    if (TABS[i] == slot && trees.get(i) != tree) {
                        tree = trees.get(i);
                        page = 0;
                        plugin.menus().remember(player, tree);
                        ChestMenu.open(plugin, player, trees, tree);
                        return;
                    }
                }
            }
        }
        render();
    }

    /** Tab slots used by trees; with chests on, the slot after them is the Chests tab. */
    private int treeTabs() {
        boolean chests = plugin.chestTypes().active();
        if (trees.size() + (chests ? 1 : 0) <= 1) {
            return 0;
        }
        return Math.min(trees.size(), chests ? TABS.length - 1 : TABS.length);
    }

    private void buy(UpgradeNode n, long now) {
        UpgradeService svc = plugin.service();
        double cost = svc.cost(plugin.data().get(player), n);
        double confirmCost = plugin.settings().confirmCost;
        if (confirmCost > 0 && cost >= confirmCost && svc.check(player, n) == UpgradeService.Result.SUCCESS
                && !(n.id.equals(confirmNode) && now < confirmUntil)) {
            confirmNode = n.id;
            confirmUntil = now + 3000;
            sound("minecraft:block.note_block.pling", 1.2f);
            render();
            return;
        }
        confirmNode = null;
        UpgradeService.Result r = svc.purchase(player, n);
        if (r == UpgradeService.Result.SUCCESS) {
            int lvl = plugin.data().get(player).level(n.id);
            sound("minecraft:entity.player.levelup", lvl >= n.maxLevel ? 2.0f : 1.5f);
            player.sendActionBar(lang().plain(lvl >= n.maxLevel ? "purchase-max" : "purchase-success",
                    "node", n.name(), "level", String.valueOf(lvl), "roman", Text.roman(lvl)));
            plugin.leaderboard().touch(plugin.data().get(player));
        } else {
            sound("minecraft:block.note_block.bass", 0.7f);
            PlayerData d = plugin.data().get(player);
            Currency c = plugin.currency();
            List<String> blockers = new ArrayList<>();
            for (UpgradeNode b : svc.blockers(d, n)) {
                blockers.add(b.name());
            }
            player.sendActionBar(switch (r) {
                case MAXED -> lang().plain("purchase-maxed");
                case LOCKED -> lang().plain("purchase-locked", "requires", String.join("<body>, </body>", svc.missing(d, player, n)));
                case BLOCKED -> lang().plain("purchase-blocked", "nodes", String.join("<body>, </body>", blockers));
                case UNAVAILABLE -> lang().plain("currency-unavailable");
                case CANCELLED -> lang().plain("purchase-cancelled");
                default -> lang().plain("purchase-not-enough", "currency", Text.escape(plugin.currencyName()),
                        "cost", Text.escape(plugin.money(cost)),
                        "balance", Text.escape(plugin.money(c.available() ? c.balance(player) : 0)));
            });
        }
        render();
    }

    private void respec(long now) {
        if (now >= respecUntil) {
            respecUntil = now + 3000;
            sound("minecraft:block.note_block.pling", 0.9f);
            return;
        }
        respecUntil = 0;
        ProgressService ps = plugin.progress();
        double refund = ps.refund(plugin.data().get(player));
        ProgressService.RespecResult r = ps.respec(player);
        player.sendActionBar(switch (r) {
            case DONE -> lang().plain("respec-done", "refund", Text.escape(plugin.money(refund)));
            case COOLDOWN -> lang().plain("respec-cooldown", "time", Text.duration(ps.respecCooldownLeft(plugin.data().get(player))));
            case NOTHING -> lang().plain("respec-nothing");
            case DISABLED -> lang().plain("respec-disabled");
            case UNAVAILABLE -> lang().plain("currency-unavailable");
            case CANCELLED -> lang().plain("purchase-cancelled");
        });
    }

    private void prestige(long now) {
        if (!plugin.progress().prestigeReady(plugin.data().get(player), player)) {
            return;
        }
        if (now >= prestigeUntil) {
            prestigeUntil = now + 3000;
            sound("minecraft:block.note_block.pling", 1.5f);
            return;
        }
        prestigeUntil = 0;
        if (plugin.progress().prestige(player) == ProgressService.PrestigeResult.DONE) {
            player.sendActionBar(lang().plain("prestige-done", "level", String.valueOf(plugin.data().get(player).prestige())));
        }
    }

    private void sound(String key, float pitch) {
        if (plugin.settings().sounds) {
            player.playSound(player.getLocation(), key, SoundCategory.MASTER, 0.7f, pitch);
        }
    }
}
