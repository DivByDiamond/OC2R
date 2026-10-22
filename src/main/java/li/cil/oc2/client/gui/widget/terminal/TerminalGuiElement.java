package li.cil.oc2.client.gui.widget.terminal;

//? if >=26.1 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import li.cil.oc2.common.vm.terminal.render.TerminalRenderer.GuiSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

// The terminal as GUI render state (26.x GUIs are extracted first and drawn later): one element
// each for solid backgrounds, textured glyphs and the cursor, replayed from an extraction snapshot.
final class TerminalGuiElement {
    private TerminalGuiElement() {}

    static TextureSetup fontTexture() {
        return TextureSetup.singleTexture(
                Minecraft.getInstance()
                        .getTextureManager()
                        .getTexture(FontHandling.getAtlas())
                        .getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }

    record Background(
            GuiSnapshot snapshot,
            Matrix3x2f pose,
            @Nullable ScreenRectangle bounds,
            @Nullable ScreenRectangle scissorArea)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer consumer) {
            snapshot.replayBackground(consumer, pose);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }

    record Text(
            GuiSnapshot snapshot,
            Matrix3x2f pose,
            TextureSetup textureSetup,
            @Nullable ScreenRectangle bounds,
            @Nullable ScreenRectangle scissorArea)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer consumer) {
            snapshot.replayText(consumer, pose);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }
    }

    record Cursor(
            GuiSnapshot snapshot,
            Matrix3x2f pose,
            @Nullable ScreenRectangle bounds,
            @Nullable ScreenRectangle scissorArea)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer consumer) {
            snapshot.replayCursor(consumer, pose);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
*///?}
