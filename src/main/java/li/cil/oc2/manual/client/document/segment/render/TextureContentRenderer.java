/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document.segment.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import li.cil.oc2.manual.api.render.ContentRenderer;
import li.cil.oc2.manual.client.util.ManualGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Image dimensions come from the resource; Minecraft owns its normal texture lifecycle. */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "No finalizer; image and stream resources are closed by try-with-resources.")
public class TextureContentRenderer implements ContentRenderer {
    private final ResourceLocation location;
    private final int width;
    private final int height;

    public TextureContentRenderer(final ResourceLocation location) {
        this.location = location;
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location);
             NativeImage image = NativeImage.read(stream)) {
            width = image.getWidth();
            height = image.getHeight();
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot load manual texture " + location, error);
        }
    }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        ManualGraphics.blit(graphics, location, 0, 0, 0, 0, width, height, width, height);
    }
}
