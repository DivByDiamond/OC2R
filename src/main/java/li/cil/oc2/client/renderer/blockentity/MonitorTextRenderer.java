package li.cil.oc2.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix4f;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    private static final Cache<MonitorGUIRenderer, MonitorGUIRenderer.RendererView> rendererViews =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(Duration.ofSeconds(5))
                    .removalListener(MonitorTextRenderer::handleNoLongerRendering)
                    .build();

    static void renderTerminal(
            final MonitorBlockEntity monitor,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final double distanceToCamera,
            final Font font) {
        if (distanceToCamera < 6f) {
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

            try {
                rendererViews
                        .get(terminal, () -> terminal.getRenderer(monitor))
                        .render(
                                stack,
                                RenderSystem.getProjectionMatrix(),
                                MonitorDevice.WIDTH,
                                MonitorDevice.HEIGHT,
                                true);
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }

            stack.popPose();
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            stack.pushPose();
            stack.translate(0, 0, -0.9f);

            final Matrix4f matrix = stack.last().pose();
            renderQuad(matrix, TEXTURE_TERMINAL.buffer(bufferSource, ModRenderType::getUnlitBlock));

            stack.popPose();
        }
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

        drawText(bufferSource, stack, bootError, font);

        stack.popPose();
    }

    static void renderPowerOverlay(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderQuad(matrix, TEXTURE_POWER.buffer(bufferSource, ModRenderType::getUnlitBlock));
    }

    static void cleanUp() {
        rendererViews.cleanUp();
    }

    private static void drawText(
            final MultiBufferSource bufferSource,
            final PoseStack stack,
            final Component text,
            final Font font) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final List<FormattedText> wrappedText =
                font.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = font.width(text);
            draw(bufferSource, font, stack, text, (maxWidth - textWidth) * 0.5f);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                draw(
                        bufferSource,
                        font,
                        stack,
                        wrappedText.get(i).getString(),
                        i * font.lineHeight);
            }
        }

        stack.popPose();
    }

    private static void draw(
            final MultiBufferSource bufferSource,
            final Font font,
            final PoseStack stack,
            final Component text,
            final float x) {
        font.drawInBatch(
                text,
                x,
                (float) 0,
                15610658,
                false,
                stack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880);
    }

    private static void draw(
            final MultiBufferSource bufferSource,
            final Font font,
            final PoseStack stack,
            final String text,
            final float y) {
        font.drawInBatch(
                text,
                (float) 0,
                y,
                15610658,
                false,
                stack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880,
                false);
    }

    static void renderQuad(final Matrix4f matrix, final VertexConsumer consumer) {
        consumer.addVertex(matrix, 0, 0, 0).setUv(0, 0);

        consumer.addVertex(matrix, 0, 16, 0).setUv(0, 1);

        consumer.addVertex(matrix, 16, 16, 0).setUv(1, 1);

        consumer.addVertex(matrix, 16, 0, 0).setUv(1, 0);
    }

    private static void handleNoLongerRendering(
            final RemovalNotification<MonitorGUIRenderer, MonitorGUIRenderer.RendererView>
                    notification) {
        final MonitorGUIRenderer key = notification.getKey();
        final MonitorGUIRenderer.RendererView value = notification.getValue();
        if (key != null && value != null) {
            key.releaseRenderer(value);
        }
    }
}
