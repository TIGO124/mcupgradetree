package com.upgradetree.tree;

import com.upgradetree.config.Localized;
import com.upgradetree.config.Problems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds tree mistakes that would otherwise fail silently: requirement cycles, nodes that can never
 * be unlocked, impossible exclusive rules and nodes drawn on top of each other. Pure logic (no Bukkit).
 */
public final class TreeValidator {

    /** What the validator needs to know about a node. */
    public record Spec(String id, String treeId, double x, double y, List<String> requires,
                       List<String> requiresAny, Set<String> requiresLevel, List<String> exclusiveWith) {
    }

    public record Report(List<Localized<String>> issues, Set<String> unreachable) {
    }

    private TreeValidator() {
    }

    public static Report validate(List<Spec> specs) {
        Map<String, Spec> byId = new HashMap<>();
        for (Spec s : specs) {
            byId.put(s.id(), s);
        }
        List<Localized<String>> issues = new ArrayList<>();

        // impossible exclusive rules
        for (Spec s : specs) {
            for (String ex : s.exclusiveWith()) {
                if (s.requires().contains(ex) || s.requiresLevel().contains(ex)) {
                    issues.add(Problems.of("'" + s.id() + "' hem '" + ex + "' düğümünü istiyor hem de onunla birlikte alınamıyor.",
                            "'" + s.id() + "' requires '" + ex + "' but cannot be combined with it."));
                }
            }
        }

        // reachability (fixpoint)
        Set<String> ok = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Spec s : specs) {
                if (ok.contains(s.id()) || impossible(s)) {
                    continue;
                }
                boolean all = ok.containsAll(s.requires()) && ok.containsAll(s.requiresLevel());
                boolean any = s.requiresAny().isEmpty();
                for (String a : s.requiresAny()) {
                    if (ok.contains(a)) {
                        any = true;
                        break;
                    }
                }
                if (all && any) {
                    ok.add(s.id());
                    changed = true;
                }
            }
        }
        Set<String> unreachable = new LinkedHashSet<>();
        for (Spec s : specs) {
            if (!ok.contains(s.id())) {
                unreachable.add(s.id());
            }
        }

        // report cycles among the unreachable nodes, then the rest
        Set<String> inCycle = new HashSet<>();
        for (List<String> cycle : cycles(specs, unreachable)) {
            inCycle.addAll(cycle);
            String path = String.join(" → ", cycle) + " → " + cycle.get(0);
            issues.add(Problems.of("Gereksinim döngüsü: " + path + " (bu düğümler hiç açılamaz)",
                    "Requirement cycle: " + path + " (these upgrades can never be unlocked)"));
        }
        for (String id : unreachable) {
            if (!inCycle.contains(id) && !impossible(byId.get(id))) {
                issues.add(Problems.of("'" + id + "' hiç açılamaz: gerektirdiği düğümler açılamıyor.",
                        "'" + id + "' can never be unlocked: the upgrades it requires cannot be unlocked."));
            }
        }

        // overlapping nodes (same tree, closer than 0.9 hex widths)
        for (int i = 0; i < specs.size(); i++) {
            Spec a = specs.get(i);
            for (int j = i + 1; j < specs.size(); j++) {
                Spec b = specs.get(j);
                if (!a.treeId().equals(b.treeId())) {
                    continue;
                }
                double dx = a.x() - b.x();
                double dy = (a.y() - b.y()) * HexGrid.ROW_STEP;
                if (dx * dx + dy * dy < 0.81) {
                    String where = "(" + a.treeId() + ": x=" + fmt(a.x()) + " y=" + fmt(a.y()) + " / x=" + fmt(b.x())
                            + " y=" + fmt(b.y()) + ")";
                    issues.add(Problems.of("'" + a.id() + "' ve '" + b.id() + "' üst üste çiziliyor " + where,
                            "'" + a.id() + "' and '" + b.id() + "' are drawn on top of each other " + where));
                }
            }
        }
        return new Report(issues, unreachable);
    }

    private static boolean impossible(Spec s) {
        for (String ex : s.exclusiveWith()) {
            if (s.requires().contains(ex) || s.requiresLevel().contains(ex)) {
                return true;
            }
        }
        return false;
    }

    /** Strongly connected components (size > 1 or self loops) restricted to the given nodes. */
    private static List<List<String>> cycles(List<Spec> specs, Set<String> within) {
        Map<String, List<String>> graph = new HashMap<>();
        for (Spec s : specs) {
            if (!within.contains(s.id())) {
                continue;
            }
            Set<String> out = new LinkedHashSet<>(s.requires());
            out.addAll(s.requiresLevel());
            out.addAll(s.requiresAny());
            out.retainAll(within);
            graph.put(s.id(), new ArrayList<>(out));
        }
        List<List<String>> result = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> low = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        int[] counter = {0};
        for (String v : graph.keySet()) {
            if (!index.containsKey(v)) {
                strong(v, graph, index, low, stack, onStack, counter, result);
            }
        }
        return result;
    }

    private static void strong(String v, Map<String, List<String>> g, Map<String, Integer> index, Map<String, Integer> low,
                               Deque<String> stack, Set<String> onStack, int[] counter, List<List<String>> out) {
        index.put(v, counter[0]);
        low.put(v, counter[0]);
        counter[0]++;
        stack.push(v);
        onStack.add(v);
        for (String w : g.getOrDefault(v, List.of())) {
            if (!index.containsKey(w)) {
                strong(w, g, index, low, stack, onStack, counter, out);
                low.put(v, Math.min(low.get(v), low.get(w)));
            } else if (onStack.contains(w)) {
                low.put(v, Math.min(low.get(v), index.get(w)));
            }
        }
        if (low.get(v).equals(index.get(v))) {
            List<String> comp = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                comp.add(w);
            } while (!w.equals(v));
            boolean selfLoop = comp.size() == 1 && g.getOrDefault(v, List.of()).contains(v);
            if (comp.size() > 1 || selfLoop) {
                java.util.Collections.reverse(comp);
                out.add(comp);
            }
        }
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
