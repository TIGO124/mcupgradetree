package com.upgradetree.menu;

/** Panel geometry in layout units (1 unit = panel distance) and depth layers. */
final class Layout {

    private Layout() {
    }

    static final double PANEL_CX = 0.0;
    static final double PANEL_CY = 0.01;
    static final double PANEL_W = 1.92;
    static final double PANEL_H = 0.92;
    static final double PANEL_STROKE = 0.012;
    static final double[][] EXTENT = {{-0.98, 0.98}, {-0.46, 0.48}};

    static final double HEADER_CY = 0.412;
    static final double HEADER_W = 1.89;
    static final double HEADER_H = 0.074;

    static final double CANVAS_X0 = -0.935;
    static final double CANVAS_X1 = 0.395;
    static final double CANVAS_Y0 = -0.372;
    static final double CANVAS_Y1 = 0.36;
    static final double CANVAS_CX = (CANVAS_X0 + CANVAS_X1) / 2.0;
    static final double PROGRESS_Y = -0.413;

    static final double CARD_X0 = 0.43;
    static final double CARD_X1 = 0.935;
    static final double CARD_Y0 = -0.385;
    static final double CARD_Y1 = 0.36;
    static final double CARD_CX = (CARD_X0 + CARD_X1) / 2.0;
    static final double CARD_W = CARD_X1 - CARD_X0;
    static final double FOOTER_Y = -0.418;

    static final double CLOSE_X = 0.903;
    static final double CLOSE_SIZE = 0.054;
    static final double PILL_X = 0.70;
    static final double PILL_W = 0.27;
    static final double PILL_H = 0.054;
    static final double RESPEC_X = 0.485;
    static final double RESPEC_W = 0.14;
    static final double PRESTIGE_X = 0.325;
    static final double PRESTIGE_W = 0.15;
    static final double BUTTON_H = 0.05;

    static final double TABS_X0 = -0.925;
    static final double TABS_X1 = 0.235;
    static final double TAB_H = 0.054;
    static final double TAB_GAP = 0.012;
    static final double TAB_MAX_W = 0.27;

    static final double INFO_HEX_Y = 0.262;
    static final double INFO_HEX_R = 0.072;
    static final double INFO_NAME_Y = 0.158;
    static final double INFO_LEVEL_Y = 0.118;
    static final double INFO_BAR_Y = 0.089;
    static final double INFO_BAR_W = 0.42;
    static final double INFO_BAR_H = 0.014;
    static final double INFO_DESC_TOP = 0.066;
    static final double INFO_DESC_LH = 0.026;
    static final int INFO_DESC_MAX_LINES = 4;
    static final int INFO_DESC_WRAP_PX = 158;
    static final double INFO_EXTRA_Y = -0.072;
    static final double INFO_BONUS_Y = -0.112;
    static final double INFO_COST_Y = -0.158;
    static final double BTN_Y = -0.29;
    static final double BTN_W = 0.42;
    static final double BTN_H = 0.068;

    /** Maximum and minimum distance between hexagon centres. */
    static final double MAX_STEP = 0.15;
    static final double MIN_STEP = 0.045;

    // ------------------------------------------------------------------ layers (fraction of distance)
    static final double Z_PANEL_STROKE = 0.000;
    static final double Z_PANEL = 0.002;
    static final double Z_STARS = 0.003;
    static final double Z_BOX = 0.004;
    static final double Z_BOX_FILL = 0.006;
    static final double Z_LINE = 0.006;
    static final double Z_HALO = 0.008;
    static final double Z_BORDER = 0.010;
    static final double Z_FILL = 0.012;
    static final double Z_SHINE = 0.014;
    static final double Z_TEXT = 0.020;
    static final double Z_ICON = 0.060;
}
