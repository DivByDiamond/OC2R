package li.cil.oc2.client.gui.widget.tablet;

import java.nio.ByteBuffer;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.stage.RenderInfo;
import li.cil.oc2.common.tablet.TabletClientView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
//? if >=26.1 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?}

/** One disposable GPU texture owned by the open tablet screen. */
public final class TabletFramebufferRenderer implements AutoCloseable {
    private final RenderInfo frame = new RenderInfo(256, 160);
    //? if <26.1 {
    private final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "tablet/display");
    //?}
    private int version = -1;

    public TabletFramebufferRenderer() {
        //? if <26.1 {
        Minecraft.getInstance().getTextureManager().register(texture, frame.getTexture());
        //?}
    }

    public void render(final GuiGraphics graphics, final TabletClientView view, final TabletViewport viewport) {
        if (version != view.frameVersion()) {
            frame.processFrame(256, 160, ByteBuffer.wrap(view.framebuffer()));
            version = view.frameVersion();
        }
        //? if >=26.1 {
        /*graphics.blit(RenderPipelines.GUI_TEXTURED, frame.getTextureLocation(), viewport.x(), viewport.y(),
                0, 0, viewport.width(), viewport.height(), 256, 160, 256, 160);
        *///?} else {
        graphics.blit(texture, viewport.x(), viewport.y(), viewport.width(), viewport.height(),
                0, 0, 256, 160, 256, 160);
        //?}
    }

    @Override public void close() {
        //? if >=26.1 {
        /*frame.close();
        *///?} else {
        Minecraft.getInstance().getTextureManager().release(texture);
        //?}
    }
}
