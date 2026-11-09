package li.cil.oc2.client.gui.widget.tablet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TabletViewportTest {
    @Test void mapsScaledCanvasExactlyAndRejectsEdgesOutsideIt() {
        final TabletViewport view = new TabletViewport(20, 40, 512, 320);
        assertEquals(new TabletViewport.Pixel(0, 0), view.pixel(20, 40).orElseThrow());
        assertEquals(new TabletViewport.Pixel(255, 159), view.pixel(531.99, 359.99).orElseThrow());
        assertEquals(new TabletViewport.Pixel(30, 98), view.pixel(80, 236).orElseThrow());
        assertTrue(view.pixel(532, 100).isEmpty());
        assertTrue(view.pixel(20, 360).isEmpty());
        assertTrue(view.pixel(19.9, 40).isEmpty());
    }

    @Test void releasingOutsideCanvasClampsWithoutWraparound() {
        final TabletViewport view = new TabletViewport(12, 34, 256, 160);
        assertEquals(new TabletViewport.Pixel(0, 0), view.clamp(-100, -100));
        assertEquals(new TabletViewport.Pixel(255, 159), view.clamp(1000, 1000));
    }
}
