package com.upgradetree.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandAliasesTest {

    @Test
    void oldTurkishSubCommandsMapToEnglish() {
        assertEquals("info", CommandAliases.sub("bilgi"));
        assertEquals("respec", CommandAliases.sub("sifirla"));
        assertEquals("respec", CommandAliases.sub("sıfırla"));
        assertEquals("respec", CommandAliases.sub("reset"));
        assertEquals("points", CommandAliases.sub("puan"));
        assertEquals("prestige", CommandAliases.sub("prestij"));
        assertEquals("daily", CommandAliases.sub("gunluk"));
        assertEquals("cleanup", CommandAliases.sub("temizle"));
        assertEquals("admin", CommandAliases.sub("panel"));
        assertEquals("language", CommandAliases.sub("dil"));
        assertEquals("help", CommandAliases.sub("?"));
    }

    @Test
    void englishNamesStayAndCaseIsIgnored() {
        assertEquals("info", CommandAliases.sub("INFO"));
        assertEquals("serverlanguage", CommandAliases.sub("ServerLanguage"));
        assertEquals("whatever", CommandAliases.sub("Whatever"));
    }

    @Test
    void optionWordsMapToEnglish() {
        assertEquals("confirm", CommandAliases.word("onayla"));
        assertEquals("confirm", CommandAliases.word("confirm"));
        assertEquals("give", CommandAliases.word("ver"));
        assertEquals("take", CommandAliases.word("al"));
        assertEquals("set", CommandAliases.word("ayarla"));
        assertEquals("all", CommandAliases.word("Herkes"));
        assertEquals("chest", CommandAliases.word("sandik"));
        assertEquals("levels", CommandAliases.word("seviye"));
        assertEquals("place", CommandAliases.word("koy"));
        assertEquals("target", CommandAliases.word("hedef"));
        assertEquals("default", CommandAliases.word("varsayilan"));
        assertEquals("Steve".toLowerCase(), CommandAliases.word("Steve"));
    }
}
