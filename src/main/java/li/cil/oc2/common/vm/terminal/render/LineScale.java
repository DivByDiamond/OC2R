package li.cil.oc2.common.vm.terminal.render;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * Geometry of one terminal row under its DEC line attribute (ESC #3/#4/#5/#6).
 *
 * <p>A doubled row keeps the physical buffer stride, so only its first
 * {@code max(1, width / 2)} cells are visible, each drawn at twice the width. A
 * double-height row additionally draws every glyph at twice the height and keeps only
 * its own half: the top row shows the upper half, the bottom row the lower half.
 *
 * <p>Pure arithmetic with no Minecraft types, so every render path (GUI, world mesh,
 * cursor) shares it and it is unit-testable.
 */
public record LineScale(int xScale, int yScale, boolean bottomHalf) {
    public static final LineScale SINGLE = new LineScale(1, 1, false);
    private static final LineScale DOUBLE_WIDTH = new LineScale(2, 1, false);
    private static final LineScale DOUBLE_HEIGHT_TOP = new LineScale(2, 2, false);
    private static final LineScale DOUBLE_HEIGHT_BOTTOM = new LineScale(2, 2, true);

    /** Unknown attribute values render as a normal row. */
    public static LineScale of(final byte attribute) {
        return switch (attribute) {
            case Terminal.LINE_ATTR_DOUBLE_WIDTH -> DOUBLE_WIDTH;
            case Terminal.LINE_ATTR_DOUBLE_HEIGHT_TOP -> DOUBLE_HEIGHT_TOP;
            case Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM -> DOUBLE_HEIGHT_BOTTOM;
            default -> SINGLE;
        };
    }

    /** Attribute of {@code row} in {@code attributes}, tolerating a missing or short array. */
    public static LineScale of(final byte[] attributes, final int row) {
        return attributes != null && row >= 0 && row < attributes.length
                ? of(attributes[row])
                : SINGLE;
    }

    /** Number of logical cells that stay visible in a row of {@code width} cells. */
    public int visibleColumns(final int width) {
        return xScale == 1 ? width : Math.max(1, width / xScale);
    }

    /** Map a cell-space y coordinate to row space (before clipping to the row). */
    public float mapY(final float y, final float cellHeight) {
        return y * yScale - (bottomHalf ? cellHeight : 0f);
    }

    /** Fraction of the cell-space height at which the visible part of the row begins. */
    public float visibleStart() {
        return bottomHalf ? 1f / yScale : 0f;
    }

    /** Fraction of the cell-space height at which the visible part of the row ends. */
    public float visibleEnd() {
        return bottomHalf ? 1f : 1f / yScale;
    }
}
