package li.cil.oc2.common.vm.terminal.fonts;

import java.awt.image.BufferedImage;

public class Glyph {
    public final BufferedImage image;
    public final int width;
    public final int height;
    public final int advance;
    public float uStart = 0;
    public float vStart = 0;
    public float uEnd = 0;
    public float vEnd = 0;

    public Glyph(BufferedImage image, int width, int height, int advance) {
        this.image = image;
        this.width = width;
        this.height = height;
        this.advance = advance;
    }

    public void setUV(float u, float v, float u2, float v2) {
        uStart = u;
        vStart = v;
        uEnd = u2;
        vEnd = v2;
    }
}