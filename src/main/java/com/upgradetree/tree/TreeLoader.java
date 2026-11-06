package com.upgradetree.tree;

import com.upgradetree.util.Palette;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Loads every *.yml file of the trees/ folder into a {@link TreeRegistry}. */
public final class TreeLoader {

    public static final String FOLDER = "trees";

    private final Logger log;
    private final List<String> problems = new ArrayList<>();

    private TreeLoader(Logger log) {
        this.log = log;
    }

    /** Moves an old single tree.yml into trees/ana.yml. Returns true if something was moved. */
    public static boolean migrateLegacy(File dataFolder, Logger log) {
        File folder = new File(dataFolder, FOLDER);
        File old = new File(dataFolder, "tree.yml");
        if (folder.exists() || !old.exists()) {
            return false;
        }
        try {
            Files.createDirectories(folder.toPath());
            Files.copy(old.toPath(), new File(folder, "ana.yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(old.toPath(), new File(dataFolder, "tree.yml.eski").toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Eski tree.yml, trees/ana.yml olarak taşındı (yedek: tree.yml.eski).");
            return true;
        } catch (IOException e) {
            log.warning("tree.yml taşınamadı: " + e.getMessage());
            return false;
        }
    }

    public static TreeRegistry load(File dataFolder, Logger log) {
        return new TreeLoader(log).loadAll(new File(dataFolder, FOLDER));
    }

    private void warn(String msg) {
        problems.add(msg);
        log.warning(msg);
    }

    private TreeRegistry loadAll(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            warn("trees klasöründe ağaç dosyası yok.");
            return new TreeRegistry(List.of(), problems, Set.of());
        }
        Arrays.sort(files);
        Map<String, List<UpgradeNode.Builder>> perTree = new LinkedHashMap<>();
        Map<String, YamlConfiguration> configs = new LinkedHashMap<>();
        Map<String, UpgradeNode.Builder> all = new LinkedHashMap<>();
        Set<String> usedKeys = new HashSet<>();
        for (File f : files) {
            String treeId = f.getName().substring(0, f.getName().length() - 4);
            YamlConfiguration y = new YamlConfiguration();
            try {
                y.load(f);
            } catch (Exception ex) {
                warn("trees/" + f.getName() + " okunamadı: " + ex.getMessage());
                continue;
            }
            configs.put(treeId, y);
            List<UpgradeNode.Builder> list = new ArrayList<>();
            ConfigurationSection ns = y.getConfigurationSection("nodes");
            if (ns == null) {
                warn("trees/" + f.getName() + ": 'nodes' bölümü yok.");
            } else {
                for (String id : ns.getKeys(false)) {
                    ConfigurationSection s = ns.getConfigurationSection(id);
                    if (s == null) {
                        continue;
                    }
                    if (all.containsKey(id) || containsIgnoreCase(all.keySet(), id)) {
                        warn("trees/" + f.getName() + ": '" + id + "' başka bir ağaçta da var, atlandı (düğüm adları tekil olmalı).");
                        continue;
                    }
                    try {
                        UpgradeNode.Builder b = parseNode(treeId, id, s, usedKeys);
                        list.add(b);
                        all.put(id, b);
                    } catch (Exception ex) {
                        warn("trees/" + f.getName() + ": '" + id + "' okunamadı: " + ex.getMessage());
                    }
                }
            }
            perTree.put(treeId, list);
        }

        // resolve requirement references (case-insensitive, across trees)
        Map<String, String> lowerIds = new HashMap<>();
        for (String id : all.keySet()) {
            lowerIds.put(id.toLowerCase(Locale.ROOT), id);
        }
        for (UpgradeNode.Builder b : all.values()) {
            b.requires = resolve(b, b.requires, all, lowerIds);
            b.requiresAny = resolve(b, b.requiresAny, all, lowerIds);
            b.exclusiveWith = resolve(b, b.exclusiveWith, all, lowerIds);
            Map<String, Integer> lv = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> e : b.requiresLevel.entrySet()) {
                List<String> r = resolve(b, List.of(e.getKey()), all, lowerIds);
                if (!r.isEmpty()) {
                    lv.merge(r.get(0), Math.max(1, e.getValue()), Math::max);
                }
            }
            b.requiresLevel = lv;
        }

        // validate
        List<TreeValidator.Spec> specs = new ArrayList<>();
        for (UpgradeNode.Builder b : all.values()) {
            specs.add(new TreeValidator.Spec(b.id, b.treeId, b.x, b.y, b.requires, b.requiresAny,
                    new LinkedHashSet<>(b.requiresLevel.keySet()), b.exclusiveWith));
        }
        TreeValidator.Report report = TreeValidator.validate(specs);
        for (String issue : report.issues()) {
            warn("Ağaç kontrolü: " + issue);
        }

        List<UpgradeTree> trees = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, List<UpgradeNode.Builder>> e : perTree.entrySet()) {
            String treeId = e.getKey();
            YamlConfiguration y = configs.get(treeId);
            Map<String, UpgradeNode> nodes = new LinkedHashMap<>();
            for (UpgradeNode.Builder b : e.getValue()) {
                nodes.put(b.id, b.build());
            }
            String title = y.getString("title", UpgradeTree.DEFAULT_TITLE);
            String tab = y.getString("tab.name", capitalize(treeId));
            Material tabIcon = material(y.getString("tab.icon", "BOOK"), Material.BOOK, "trees/" + treeId + ".yml tab.icon");
            int order = y.getInt("tab.order", ++index * 10);
            trees.add(new UpgradeTree(treeId, title, tab, tabIcon, order, y.getString("tab.permission", ""), nodes));
        }
        return new TreeRegistry(trees, problems, report.unreachable());
    }

    private List<String> resolve(UpgradeNode.Builder owner, List<String> refs, Map<String, UpgradeNode.Builder> all,
                                 Map<String, String> lowerIds) {
        List<String> out = new ArrayList<>();
        for (String r : refs) {
            String real = all.containsKey(r) ? r : lowerIds.get(r.toLowerCase(Locale.ROOT));
            if (real == null) {
                warn("'" + owner.id + "' bilinmeyen '" + r + "' düğümünü gösteriyor, yok sayıldı.");
            } else if (real.equals(owner.id)) {
                warn("'" + owner.id + "' kendini gösteremez.");
            } else if (!out.contains(real)) {
                out.add(real);
            }
        }
        return out;
    }

    private UpgradeNode.Builder parseNode(String treeId, String id, ConfigurationSection s, Set<String> usedKeys) {
        UpgradeNode.Builder b = new UpgradeNode.Builder(id, treeId);
        String key = sanitize(id);
        String base = key;
        int i = 2;
        while (!usedKeys.add(key)) {
            key = base + "_" + i++;
        }
        b.key = key;
        b.nameRaw = s.getString("name", id);
        String ctx = "trees/" + treeId + ".yml '" + id + "'";
        b.icon = material(s.getString("icon", "PAPER"), Material.PAPER, ctx + " icon");
        b.iconModel = s.getString("icon-model");
        b.headTexture = s.getString("icon-texture");
        b.x = s.getDouble("x", 0);
        b.y = s.getDouble("y", 0);
        b.maxLevel = s.getInt("max-level", 1);
        b.cost = s.getDouble("cost", 1);
        b.costIncrease = s.getDouble("cost-increase", 0);
        b.costMultiplier = s.getDouble("cost-multiplier", 1);
        b.costs = new ArrayList<>(s.getDoubleList("costs"));
        b.requires = stringList(s, "requires");
        b.requiresAny = stringList(s, "requires-any");
        b.exclusiveWith = stringList(s, "exclusive-with");
        ConfigurationSection lv = s.getConfigurationSection("requires-level");
        if (lv != null) {
            for (String k : lv.getKeys(false)) {
                b.requiresLevel.put(k, lv.getInt(k, 1));
            }
        }
        b.permission = s.getString("permission");
        b.minPrestige = s.getInt("min-prestige", 0);
        b.alwaysVisible = s.getBoolean("always-visible", false);
        b.color = Palette.parse(s.getString("color"), UpgradeTree.DEFAULT_COLOR);
        b.description = stringList(s, "description");
        b.bonus = s.getString("bonus", "");
        b.bonusMultiplier = s.getDouble("bonus-multiplier", 1);
        b.bonusPerLevel = s.contains("bonus-per-level") ? s.getDouble("bonus-per-level") : null;
        b.effects = EffectParser.parseAll(ctx, s.getMapList("effects"), new ForwardingLogger(this));
        return b;
    }

    private Material material(String name, Material def, String ctx) {
        Material m = Material.matchMaterial(name == null ? "" : name.trim());
        if (m == null || !m.isItem() || m.isAir()) {
            warn(ctx + ": geçersiz eşya '" + name + "', " + def + " kullanıldı.");
            return def;
        }
        return m;
    }

    private static List<String> stringList(ConfigurationSection s, String path) {
        List<String> list = new ArrayList<>(s.getStringList(path));
        if (list.isEmpty() && s.isString(path)) {
            String v = s.getString(path);
            if (v != null && !v.isBlank()) {
                list.add(v);
            }
        }
        return list;
    }

    private static boolean containsIgnoreCase(Set<String> set, String id) {
        for (String s : set) {
            if (s.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    public static String sanitize(String id) {
        StringBuilder sb = new StringBuilder();
        String lower = id.toLowerCase(Locale.ROOT)
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u').replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
        for (char c : lower.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.isEmpty() ? "node" : sb.toString();
    }

    /** Sends effect parser warnings into the loader's problem list too. */
    private static final class ForwardingLogger extends Logger {
        private final TreeLoader loader;

        ForwardingLogger(TreeLoader loader) {
            super("UpgradeTree-Loader", null);
            this.loader = loader;
        }

        @Override
        public void warning(String msg) {
            loader.warn(msg);
        }
    }
}
