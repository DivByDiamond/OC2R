package li.cil.oc2.common.vm.terminal.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.junit.jupiter.api.Test;

class LineScaleTest {
    @Test
    void attributesMapToScales() {
        assertSame(LineScale.SINGLE, LineScale.of(Terminal.LINE_ATTR_SINGLE));
        assertEquals(new LineScale(2, 1, false), LineScale.of(Terminal.LINE_ATTR_DOUBLE_WIDTH));
        assertEquals(new LineScale(2, 2, false), LineScale.of(Terminal.LINE_ATTR_DOUBLE_HEIGHT_TOP));
        assertEquals(new LineScale(2, 2, true), LineScale.of(Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM));
        assertSame(LineScale.SINGLE, LineScale.of((byte) 9));
    }

    @Test
    void missingAttributeArrayOrRowIsSingle() {
        assertSame(LineScale.SINGLE, LineScale.of(null, 0));
        assertSame(LineScale.SINGLE, LineScale.of(new byte[2], -1));
        assertSame(LineScale.SINGLE, LineScale.of(new byte[2], 2));
    }

    @Test
    void doubledRowsKeepHalfTheCellsAndAtLeastOne() {
        assertEquals(80, LineScale.SINGLE.visibleColumns(80));
        assertEquals(40, LineScale.of(Terminal.LINE_ATTR_DOUBLE_WIDTH).visibleColumns(80));
        assertEquals(66, LineScale.of(Terminal.LINE_ATTR_DOUBLE_HEIGHT_TOP).visibleColumns(132));
        assertEquals(2, LineScale.of(Terminal.LINE_ATTR_DOUBLE_WIDTH).visibleColumns(5));
        assertEquals(1, LineScale.of(Terminal.LINE_ATTR_DOUBLE_WIDTH).visibleColumns(1));
    }

    @Test
    void doubleHeightRowsOwnOneHalfOfTheGlyph() {
        final LineScale top = LineScale.of(Terminal.LINE_ATTR_DOUBLE_HEIGHT_TOP);
        final LineScale bottom = LineScale.of(Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM);
        assertEquals(0f, top.visibleStart());
        assertEquals(0.5f, top.visibleEnd());
        assertEquals(0.5f, bottom.visibleStart());
        assertEquals(1f, bottom.visibleEnd());
        // The cell midline lands on the row edge: bottom of the top row, top of the bottom row.
        assertEquals(16f, top.mapY(8f, 16f));
        assertEquals(0f, bottom.mapY(8f, 16f));
        assertEquals(16f, bottom.mapY(16f, 16f));
        // A single or double-width row keeps its vertical geometry.
        assertEquals(8f, LineScale.SINGLE.mapY(8f, 16f));
        assertEquals(8f, LineScale.of(Terminal.LINE_ATTR_DOUBLE_WIDTH).mapY(8f, 16f));
    }
}
