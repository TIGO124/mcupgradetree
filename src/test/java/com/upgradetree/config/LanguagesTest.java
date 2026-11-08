package com.upgradetree.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LanguagesTest {

    @Test
    void validCodesAreLowerCased() {
        assertEquals("en", Languages.normalize("EN"));
        assertEquals("tr", Languages.normalize(" tr "));
        assertEquals("pt_br", Languages.normalize("pt_BR"));
    }

    @Test
    void unsafeOrOddCodesAreRejected() {
        assertNull(Languages.normalize(null));
        assertNull(Languages.normalize("e"));
        assertNull(Languages.normalize("../config"));
        assertNull(Languages.normalize("en.yml"));
        assertNull(Languages.normalize("averyverylongcode"));
    }
}
