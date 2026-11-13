package com.upgradetree.tree;

import com.upgradetree.config.Lang;
import com.upgradetree.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One upgrade tree (a tab of the menu), loaded from one file in trees/. */
public final class UpgradeTree {

    public static final int DEFAULT_COLOR = 0xFF6B7A8F;

    public final String id;
    /** Title (shown when there is only one tree); null = the default title of messages.yml. */
    public final String title;
    /** Tab name (MiniMessage). */
    public final String tab;
    public final Material tabIcon;
    public final int order;
    public final String permission;
    private final Map<String, UpgradeNode> nodes;
    private final List<UpgradeNode[]> edges;
    private final Map<String, Integer> depth;

    public UpgradeTree(String id, String title, String tab, Material tabIcon, int order,
                       String permission, Map<String, UpgradeNode> nodes) {
        this.id = id;
        this.title = title == null || title.isBlank() ? null : title;
        this.tab = tab == null || tab.isBlank() ? id : tab;
        this.tabIcon = tabIcon;
        this.order = order;
        this.permission = permission == null || permission.isBlank() ? null : permission.trim();
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        List<UpgradeNode[]> e = new ArrayList<>();
        for (UpgradeNode child : nodes.values()) {
            for (String p : child.links) {
                UpgradeNode parent = nodes.get(p);
                if (parent != null) {
                    e.add(new UpgradeNode[]{parent, child});
                }
            }
        }
        this.edges = Collections.unmodifiableList(e);
        this.depth = computeDepth();
    }

    /** Title (MiniMessage); the default title of messages.yml when the file has none. */
    public String titleRaw(Lang lang) {
        return title != null ? title : lang == null ? id : lang.raw("tree-title-default");
    }

    public Component title(Lang lang) {
        return Text.mm(titleRaw(lang));
    }

    /** Tab name (MiniMessage). */
    public String tabRaw() {
        return tab;
    }

    public Component tabName() {
        return Text.mm(tab);
    }

    public Collection<UpgradeNode> nodes() {
        return nodes.values();
    }

    public UpgradeNode node(String id) {
        return nodes.get(id);
    }

    /** Parent → child pairs inside this tree (one per requirement). */
    public List<UpgradeNode[]> edges() {
        return edges;
    }

    public int depth(UpgradeNode n) {
        return depth.getOrDefault(n.id, 0);
    }

    public int totalMaxLevels() {
        int t = 0;
        for (UpgradeNode n : nodes.values()) {
            t += n.maxLevel;
        }
        return t;
    }

    private Map<String, Integer> computeDepth() {
        Map<String, Integer> d = new HashMap<>();
        Deque<UpgradeNode> queue = new ArrayDeque<>();
        for (UpgradeNode n : nodes.values()) {
            boolean rootHere = true;
            for (String p : n.links) {
                if (nodes.containsKey(p)) {
                    rootHere = false;
                    break;
                }
            }
            if (rootHere) {
                d.put(n.id, 0);
                queue.add(n);
            }
        }
        while (!queue.isEmpty()) {
            UpgradeNode cur = queue.poll();
            int cd = d.get(cur.id);
            for (UpgradeNode[] e : edges) {
                if (e[0] == cur) {
                    int nd = cd + 1;
                    Integer old = d.get(e[1].id);
                    if ((old == null || old < nd) && nd <= nodes.size()) {
                        d.put(e[1].id, nd);
                        queue.add(e[1]);
                    }
                }
            }
        }
        for (UpgradeNode n : nodes.values()) {
            d.putIfAbsent(n.id, 0);
        }
        return d;
    }
}
