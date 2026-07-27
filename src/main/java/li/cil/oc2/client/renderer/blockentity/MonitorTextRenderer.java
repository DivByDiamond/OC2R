package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

final class MonitorTextRenderer {
    static final ResourceLocation OVERLAY_POWER_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/monitor/monitor_overlay_power");
    static final ResourceLocation OVERLAY_TERMINAL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/computer/computer_overlay_terminal");

    private static final Material TEXTURE_POWER =
            new Material(InventoryMenu.BLOCK_ATLAS, OVERLAY_POWER_LOCATION);
    private static final Material TEXTURE_TERMINAL =
            new Material(InventoryMenu.BLOCK_ATLAS, OVERLAY_TERMINAL_LOCATION);

    static void renderTerminal(
            final MonitorBlockEntity monitor,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final double distanceToCamera,
            final Font font) {
        // Always render the monitor's DynamicTexture content at all distances.
        // Same texture at any LOD – just smaller via perspective projection.
        // NEAREST filtering on the monitor's DynamicTexture keeps text crisp.
        stack.pushPose();
        stack.translate(2, 2, -0.9f);

        final MonitorGUIRenderer terminal = monitor.getMonitor();
        final float textScaleX = 12f / MonitorDevice.WIDTH;
        final float textScaleY = 9f / MonitorDevice.HEIGHT;
        final float scale = Math.min(textScaleX, textScaleY) * 0.95f;

        final float scaleDeltaX = textScaleX - scale;
        final float scaleDeltaY = textScaleY - scale;
        stack.translate(
                MonitorDevice.WIDTH * scaleDeltaX * 0.5f,
                MonitorDevice.HEIGHT * scaleDeltaY * 0.5f,
                0f);

        stack.scale(scale, scale, 1f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        MonitorRendererCache.getRendererView(terminal, monitor)
                .render(
                        stack,
                        RenderSystem.getProjectionMatrix(),
                        MonitorDevice.WIDTH,
                        MonitorDevice.HEIGHT,
                        true);

        stack.popPose();
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    static void renderStatusText(
            final MultiBufferSource bufferSource,
            final MonitorBlockEntity monitor,
            final PoseStack stack,
            final double distanceToCamera,
            final Font font) {
        if (distanceToCamera > 12f) {
            return;
        }

        final Component bootError =
                Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY);

        stack.pushPose();
        stack.translate(3, 3, -0.9f);

        RendererQuadHelper.drawText(bufferSource, stack, bootError, font);

        stack.popPose();
    }

    static void renderPowerOverlay(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        RendererQuadHelper.renderQuad(matrix, TEXTURE_POWER.buffer(bufferSource, ModRenderType::getUnlitBlock));
    }

    static void cleanUp() {
        MonitorRendererCache.cleanUp();
    }
}
