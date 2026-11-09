package li.cil.oc2.client.gui.widget.tablet;

import java.util.Optional;

/** Pure screen-to-guest pixel mapping, shared by rendering and hit testing. */
public record TabletViewport(int x, int y, int width, int height) {
    public record Pixel(int x, int y) {}

    public Optional<Pixel> pixel(final double mouseX, final double mouseY) {
        if (width <= 0 || height <= 0 || mouseX < x || mouseY < y || mouseX >= x + width || mouseY >= y + height) {
            return Optional.empty();
        }
        return Optional.of(clamp(mouseX, mouseY));
    }

    public Pixel clamp(final double mouseX, final double mouseY) {
        final int px = (int) Math.floor((mouseX - x) * 256 / Math.max(1, width));
        final int py = (int) Math.floor((mouseY - y) * 160 / Math.max(1, height));
        return new Pixel(Math.clamp(px, 0, 255), Math.clamp(py, 0, 159));
    }
}
