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
    //? if >=26.1 {
/*    private DynamicTexture dynamicTexture;
*///?} else {
    private final DynamicTexture dynamicTexture;
    //?}
    private boolean textureIsDirty = true;
    private final List<Glyph> glyphs;

    private int currentX = 0; // X coordinate to place next glyph
    private int currentY = 0; // Y coordinate to place next glyph

    public FontAtlas(int initialWidth, int initialHeight, String fontAtlasName) {
        this.atlasWidth = initialWidth;
        this.atlasHeight = initialHeight;
        this.atlasImage = new NativeImage(atlasWidth, atlasHeight, false);
        //? if >=26.1 {
/*        this.dynamicTexture = new DynamicTexture(() -> fontAtlasName, atlasImage);
*///?} else {
        this.dynamicTexture = new DynamicTexture(atlasImage);
        //?}
        this.resources = ResourceLocation.fromNamespaceAndPath("oc2r", fontAtlasName);
        Minecraft.getInstance().getTextureManager().register(resources, dynamicTexture);
        this.glyphs = new ArrayList<>();

        for (int x = 0; x < atlasWidth; x++) {
            for (int y = 0; y < atlasHeight; y++) {
                //? if >=26.1 {
/*                this.atlasImage.setPixel(x, y, 0); // transparent, ABGR==ARGB for (0,0,0,0)
*///?} else {
                this.atlasImage.setPixelRGBA(x, y, 0); // transparent, ABGR==ARGB for (0,0,0,0)
                //?}
            }
        }

        BufferedImage f = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = f.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(new Rectangle(0, 0, 16, 32));

        g.dispose();

        Glyph square = new Glyph(f);

        addGlyph(square);
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
                //? if >=26.1 {
/*                // NativeImage.setPixel takes ARGB on 26.x, the same layout BufferedImage.getRGB returns.
                atlasImage.setPixel(currentX + x, currentY + y, glyph.image.getRGB(x, y));
*///?} else {
                int argb = glyph.image.getRGB(x, y);
                // BufferedImage.getRGB() returns ARGB (0xAARRGGBB),
                // but NativeImage.setPixelRGBA() expects ABGR (0xAABBGGRR).
                // Swap red and blue channels.
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                atlasImage.setPixelRGBA(currentX + x, currentY + y, abgr);
                //?}
            }
        }

        // Calculate the UV coordinates for this glyph
        float uStart = currentX * (1f / atlasWidth);
        float vStart = currentY * (1f / atlasHeight);
        float uEnd = (currentX + glyph.image.getWidth() + 1) * (1f / atlasWidth);
        float vEnd = (currentY + glyph.image.getHeight() + 1) * (1f / atlasHeight);

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
                //? if >=26.1 {
/*                newAtlasImage.setPixel(x, y, oldAtlasImage.getPixel(x, y));
*///?} else {
                int color = oldAtlasImage.getPixelRGBA(x, y);
                newAtlasImage.setPixelRGBA(x, y, color); // already ABGR, no swap needed
                //?}
            }
        }
        oldAtlasImage.close();

        for (Glyph glyph : glyphs) {
            glyph.setUV(glyph.uStart / 2f, glyph.vStart / 2f, glyph.uEnd / 2f, glyph.vEnd / 2f);
        }

        this.atlasWidth = newWidth;
        this.atlasHeight = newHeight;
        this.atlasImage = newAtlasImage;

        //? if >=26.1 {
/*        // A 26.x GPU texture has a fixed size: replace it with one of the new size under the same
        // id. Registering closes the previous texture.
        this.dynamicTexture = new DynamicTexture(() -> resources.toString(), atlasImage);
        Minecraft.getInstance().getTextureManager().register(resources, dynamicTexture);
*///?} else {
        this.dynamicTexture.setPixels(atlasImage);
        //?}
        textureIsDirty = true;
    }

    public ResourceLocation getTextureId() {
        if (textureIsDirty) {
            dynamicTexture.upload();
        }
        return this.resources;
    }
}