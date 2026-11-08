package com.upgradetree.tree;

import com.upgradetree.config.Localized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Every loaded tree (menu tab) and a global index of their nodes. Node ids are unique across trees. */
public final class TreeRegistry {

    private final List<UpgradeTree> trees;
    private final Map<String, UpgradeTree> byId = new LinkedHashMap<>();
    private final Map<String, UpgradeNode> nodes = new LinkedHashMap<>();
    private final Map<String, UpgradeNode> lower = new HashMap<>();
    private final Map<String, List<UpgradeNode>> children = new HashMap<>();
    private final List<Localized<String>> issues;
    private final Set<String> unreachable;

    public TreeRegistry(List<UpgradeTree> trees, List<Localized<String>> issues, Set<String> unreachable) {
        List<UpgradeTree> sorted = new ArrayList<>(trees);
        sorted.sort((a, b) -> Integer.compare(a.order, b.order));
        this.trees = Collections.unmodifiableList(sorted);
        for (UpgradeTree t : sorted) {
            byId.put(t.id, t);
            for (UpgradeNode n : t.nodes()) {
                nodes.put(n.id, n);
                lower.put(n.id.toLowerCase(Locale.ROOT), n);
            }
        }
        for (UpgradeNode n : nodes.values()) {
            for (String p : n.links) {
                children.computeIfAbsent(p, k -> new ArrayList<>()).add(n);
            }
        }
        this.issues = List.copyOf(issues);
        this.unreachable = Set.copyOf(unreachable);
    }

    public static TreeRegistry empty() {
        return new TreeRegistry(List.of(), List.of(), Set.of());
    }

    public List<UpgradeTree> trees() {
        return trees;
    }

    public UpgradeTree tree(String id) {
        if (id == null) {
            return null;
        }
        UpgradeTree t = byId.get(id);
        if (t != null) {
            return t;
        }
        for (UpgradeTree x : trees) {
            if (x.id.equalsIgnoreCase(id)) {
                return x;
            }
        }
        return null;
    }

    public UpgradeTree first() {
        return trees.isEmpty() ? null : trees.get(0);
    }

    public Collection<UpgradeNode> nodes() {
        return nodes.values();
    }

    public UpgradeNode node(String id) {
        if (id == null) {
            return null;
        }
        UpgradeNode n = nodes.get(id);
        return n != null ? n : lower.get(id.toLowerCase(Locale.ROOT));
    }

    /** Nodes that mention {@code id} in one of their requirements. */
    public List<UpgradeNode> children(String id) {
        return children.getOrDefault(id, List.of());
    }

    public int totalMaxLevels() {
        int t = 0;
        for (UpgradeNode n : nodes.values()) {
            t += n.maxLevel;
        }
        return t;
    }

    /** Problems found while loading, in Turkish and English (see {@link com.upgradetree.config.Problems}). */
    public List<Localized<String>> issues() {
        return issues;
    }

    public boolean unreachable(String id) {
        return unreachable.contains(id);
    }
}
