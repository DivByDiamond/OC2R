package li.cil.oc2.common.vm.terminal.fonts;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FontAtlas {
    private static final int PADDING = 2; // Padding between glyphs

    private final ResourceLocation resources;
    private int atlasWidth;
    private int atlasHeight;
    public NativeImage atlasImage; // The current texture
    private final DynamicTexture dynamicTexture;
    private boolean textureIsDirty = true;
    private final List<Glyph> glyphs;

    private int currentX = 0; // X coordinate to place next glyph
    private int currentY = 0; // Y coordinate to place next glyph

    public FontAtlas(int initialWidth, int initialHeight, String fontAtlasName) {
        this.atlasWidth = initialWidth;
        this.atlasHeight = initialHeight;
        this.atlasImage = new NativeImage(atlasWidth, atlasHeight, false);
        this.dynamicTexture = new DynamicTexture(atlasImage);
        this.resources = ResourceLocation.fromNamespaceAndPath("oc2r", fontAtlasName);
        Minecraft.getInstance().getTextureManager().register(resources, dynamicTexture);
        this.glyphs = new ArrayList<>();

        for (int x = 0; x < atlasWidth; x++) {
            for (int y = 0; y < atlasHeight; y++) {
                this.atlasImage.setPixelRGBA(x, y, new Color(0, 0, 0, 0).getRGB());
            }
        }

        BufferedImage f = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = f.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(new Rectangle(0, 0, 16, 32));

        g.dispose();

        Glyph square = new Glyph(f, 16, 32, 0);

        addGlyph(square);

        glyphs.add(square);
    }

    public final void addGlyph(Glyph glyph) {
        if (currentX + glyph.image.getWidth() > atlasWidth) {
            currentX = 0;
            currentY += glyph.image.getHeight() + PADDING;
        }

        // Check if there's enough space in the current atlas
        if (currentY + glyph.image.getHeight() > atlasHeight) {
            resizeAtlas(); // Resize the atlas if there isn't enough space
        }

        // Copy the glyph into the atlas at the correct position
        for (int y = 0; y < glyph.image.getHeight(); y++) {
            for (int x = 0; x < glyph.image.getWidth(); x++) {
                int color = glyph.image.getRGB(x, y);
                atlasImage.setPixelRGBA(currentX + x, currentY + y, color);
            }
        }

        // Calculate the UV coordinates for this glyph
        float uStart = (float) currentX * (1f / atlasWidth);
        float vStart = (float) currentY * (1f / atlasHeight);
        float uEnd = (float) (currentX + glyph.image.getWidth() + 1) * (1f / atlasWidth);
        float vEnd = (float) (currentY + glyph.image.getHeight() + 1) * (1f / atlasHeight);

        glyph.setUV(uStart, vStart, uEnd, vEnd);

        glyphs.add(glyph);

        // Update the position for the next glyph
        currentX += glyph.image.getWidth() + PADDING;
        textureIsDirty = true;
    }

    private void resizeAtlas() {
        int newWidth = atlasWidth * 2;
        int newHeight = atlasHeight * 2;

        final NativeImage oldAtlasImage = atlasImage;
        NativeImage newAtlasImage = new NativeImage(newWidth, newHeight, false);

        for (int y = 0; y < atlasHeight; y++) {
            for (int x = 0; x < atlasWidth; x++) {
                int color = oldAtlasImage.getPixelRGBA(x, y);
                newAtlasImage.setPixelRGBA(x, y, color);
            }
        }
        oldAtlasImage.close();

        for (Glyph glyph : glyphs) {
            glyph.setUV(glyph.uStart / 2f, glyph.vStart / 2f, glyph.uEnd / 2f, glyph.vEnd / 2f);
        }

        this.atlasWidth = newWidth;
        this.atlasHeight = newHeight;
        this.atlasImage = newAtlasImage;

        this.dynamicTexture.setPixels(atlasImage);
        textureIsDirty = true;
    }

    public ResourceLocation getTextureId() {
        if (textureIsDirty) {
            dynamicTexture.upload();
        }
        return this.resources;
    }
}