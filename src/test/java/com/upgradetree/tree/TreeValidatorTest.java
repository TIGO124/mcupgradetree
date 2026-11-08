package com.upgradetree.tree;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeValidatorTest {

    private static TreeValidator.Spec node(String id, double x, double y, List<String> requires) {
        return new TreeValidator.Spec(id, "t", x, y, requires, List.of(), Set.of(), List.of());
    }

    @Test
    void validTreeHasNoIssues() {
        TreeValidator.Report r = TreeValidator.validate(List.of(
                node("root", 0, 0, List.of()),
                node("a", 1, 0, List.of("root")),
                node("b", 0.5, 1, List.of("root", "a"))));
        assertTrue(r.issues().isEmpty(), r.issues().toString());
        assertTrue(r.unreachable().isEmpty());
    }

    @Test
    void cycleIsReportedAndNodesAreUnreachable() {
        TreeValidator.Report r = TreeValidator.validate(List.of(
                node("root", 0, 0, List.of()),
                node("a", 1, 0, List.of("b")),
                node("b", 2, 0, List.of("a")),
                node("c", 3, 0, List.of("b"))));
        assertEquals(Set.of("a", "b", "c"), r.unreachable());
        assertTrue(r.issues().stream().anyMatch(s -> s.base().contains("döngüsü") && s.base().contains("a") && s.get("en", "en").contains("cycle")), r.issues().toString());
        assertTrue(r.issues().stream().anyMatch(s -> s.base().contains("'c' hiç açılamaz") && s.get("en", "en").contains("'c' can never be unlocked")), r.issues().toString());
    }

    @Test
    void requiresAnyCanEscapeACycle() {
        TreeValidator.Report r = TreeValidator.validate(List.of(
                new TreeValidator.Spec("root", "t", 0, 0, List.of(), List.of(), Set.of(), List.of()),
                new TreeValidator.Spec("a", "t", 1, 0, List.of(), List.of("root", "b"), Set.of(), List.of()),
                new TreeValidator.Spec("b", "t", 2, 0, List.of("a"), List.of(), Set.of(), List.of())));
        assertTrue(r.unreachable().isEmpty(), r.unreachable().toString());
        assertTrue(r.issues().isEmpty(), r.issues().toString());
    }

    @Test
    void requiresLevelCountsAsHardRequirement() {
        TreeValidator.Report r = TreeValidator.validate(List.of(
                new TreeValidator.Spec("a", "t", 0, 0, List.of(), List.of(), Set.of("b"), List.of()),
                new TreeValidator.Spec("b", "t", 1, 0, List.of(), List.of(), Set.of("a"), List.of())));
        assertEquals(Set.of("a", "b"), r.unreachable());
    }

    @Test
    void impossibleExclusiveRuleIsReported() {
        TreeValidator.Report r = TreeValidator.validate(List.of(
                node("root", 0, 0, List.of()),
                new TreeValidator.Spec("x", "t", 1, 0, List.of("root"), List.of(), Set.of(), List.of("root"))));
        assertTrue(r.unreachable().contains("x"));
        assertTrue(r.issues().stream().anyMatch(s -> s.base().contains("birlikte alınamıyor")), r.issues().toString());
    }

    @Test
    void overlappingNodesInTheSameTreeAreReported() {
        TreeValidator.Report r = TreeValidator.validate(List.of(
                node("a", 0, 0, List.of()),
                node("b", 0, 1, List.of()),
                node("c", 1, 1, List.of()),
                new TreeValidator.Spec("d", "other", 0, 0, List.of(), List.of(), Set.of(), List.of())));
        // (0,0)-(0,1) overlap (one row apart without the half shift); (0,1)-(1,1) are side neighbours
        assertEquals(1, r.issues().stream().filter(s -> s.base().contains("üst üste")).count(), r.issues().toString());
        assertFalse(r.issues().stream().anyMatch(s -> s.base().contains("'d'")), "other trees never overlap");
    }
}
