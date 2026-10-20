package li.cil.oc2.common.vm.terminal.fonts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnicodeFontRendererTest {
    @Test
    void compressesCapitalsAndDigitsToKeepThemInsideTerminalRows() {
        assertEquals(0.82f, UnicodeFontRenderer.verticalScaleFor('A'));
        assertEquals(0.82f, UnicodeFontRenderer.verticalScaleFor('7'));
        assertEquals(1f, UnicodeFontRenderer.verticalScaleFor('a'));
        assertEquals(1f, UnicodeFontRenderer.verticalScaleFor('g'));
        assertEquals(1f, UnicodeFontRenderer.verticalScaleFor(' '));
    }
}
