package com.upgradetree.menu;

import com.upgradetree.pack.PackBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutTest {

    /** The background picture is cut to the panel's shape. */
    @Test
    void pictureHasThePanelShape() {
        assertEquals(Layout.PANEL_W / Layout.PANEL_H, PackBuilder.ASPECT, 1e-9);
    }
}
