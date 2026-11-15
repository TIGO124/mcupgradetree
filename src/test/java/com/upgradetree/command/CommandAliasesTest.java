package com.upgradetree.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandAliasesTest {

    @Test
    void shortSpellingsMapToTheMainNames() {
        assertEquals("help", CommandAliases.sub("?"));
        assertEquals("respec", CommandAliases.sub("reset"));
        assertEquals("top", CommandAliases.sub("leaderboard"));
        assertEquals("chests", CommandAliases.sub("chest"));
    }

    @Test
    void mainNamesStayAndCaseIsIgnored() {
        assertEquals("info", CommandAliases.sub("INFO"));
        assertEquals("prestige", CommandAliases.sub("Prestige"));
        assertEquals("whatever", CommandAliases.sub("Whatever"));
    }

    @Test
    void optionWords() {
        assertEquals("confirm", CommandAliases.word("CONFIRM"));
        assertEquals("give", CommandAliases.word("add"));
        assertEquals("levels", CommandAliases.word("level"));
        assertEquals("chest", CommandAliases.word("gui"));
        assertEquals("panel", CommandAliases.word("3D"));
        assertEquals("all", CommandAliases.word("*"));
        assertEquals("steve", CommandAliases.word("Steve"));
    }

    /** /chest ... runs as /tigo chests ... */
    @Test
    void rootShortcutBecomesTheSubCommand() {
        assertArrayEquals(new String[] {"chests"}, CommandAliases.under("chests", new String[0]));
        assertArrayEquals(new String[] {"chests", "give", "Steve", "iron", "5"},
                CommandAliases.under("chests", new String[] {"give", "Steve", "iron", "5"}));
    }
}
