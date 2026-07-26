/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.MonitorBlock;
import li.cil.oc2.common.blockentity.MonitorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("unused")
@EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID)
public final class MonitorRenderer implements BlockEntityRenderer<MonitorBlockEntity> {
    public static final ResourceLocation OVERLAY_POWER_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/monitor/monitor_overlay_power");
    public static final ResourceLocation OVERLAY_TERMINAL_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/computer/computer_overlay_terminal");

    private static final Material TEXTURE_POWER = new Material(InventoryMenu.BLOCK_ATLAS, OVERLAY_POWER_LOCATION);
    private static final Material TEXTURE_TERMINAL = new Material(InventoryMenu.BLOCK_ATLAS, OVERLAY_TERMINAL_LOCATION);

    private static final Cache<MonitorGUIRenderer, MonitorGUIRenderer.RendererView> rendererViews = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(MonitorRenderer::handleNoLongerRendering)
        .build();

    ///////////////////////////////////////////////////////////////////

    private final BlockEntityRenderDispatcher renderer;
    private final Font font;

    ///////////////////////////////////////////////////////////////////

    public MonitorRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
        this.font = context.getFont();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final MonitorBlockEntity monitor, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        // When rendering a Sable / Create: Aeronautics contraption, the
        // BlockEntity passed to us is a "virtual clone" at a far-away virtual
        // BlockPos. Its framebuffer never receives MonitorFramebufferMessage
        // updates because the server sends those with the original BlockPos.
        // Look up the primary monitor (same persistent device id, real
        // BlockPos) and render its framebuffer instead. Falls back to the
        // passed-in BlockEntity when not on a contraption.
        final MonitorBlockEntity framebufferSource = monitor.getPrimaryForContraptionRendering();

        final Direction blockFacing = monitor.getBlockState().getValue(MonitorBlock.FACING);

        // Compute the block's actual world position from the PoseStack so
        // the front-face cull and the terminal-vs-texture distance test
        // work for contraption-mounted monitors (whose raw BlockPos is a
        // Sable virtual coordinate ~20M blocks away).
        final Matrix4f poseMatrix = stack.last().pose();
        final Vec3 blockWorldPosRelativeToCamera = new Vec3(poseMatrix.m30(), poseMatrix.m31(), poseMatrix.m32());
        final Vec3 blockCenterRelativeToCamera = blockWorldPosRelativeToCamera.add(0.5, 0.5, 0.5);

        // If viewer is not in front of the block we can skip the rest, it cannot be visible.
        // Skip this cull for contraption virtual clones — their facing may be
        // rotated by the contraption transform and the raw blockFacing may
        // not match the rendered orientation.
        final Vec3 blockCenterToCamera = blockCenterRelativeToCamera.scale(-1);
        final double projectedCameraPosition = blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
        if (!monitor.isContraptionVirtualClone() && projectedCameraPosition <= 0) {
            return;
        }

        final double distanceToCamera = blockCenterRelativeToCamera.length();

        stack.pushPose();

        // Align with front face of block.
        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot() + 180));
        stack.translate(-0.5f, 0, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        if (framebufferSource.getPowerState() && framebufferSource.isMounted() && framebufferSource.hasPower()) {
            renderTerminal(framebufferSource, stack, bufferSource, distanceToCamera);
        } else if (framebufferSource.getPowerState()) {
            renderStatusText(bufferSource, framebufferSource, stack, distanceToCamera);
        }

        stack.translate(0, 0, -0.1f);
        final Matrix4f matrix = stack.last().pose();

        if(framebufferSource.getPowerState() && framebufferSource.hasPower())
            renderPower(matrix, bufferSource);

        stack.popPose();
    }

    ///////////////////////////////////////////////////////////////////

    private void renderTerminal(final MonitorBlockEntity monitor, final PoseStack stack, final MultiBufferSource bufferSource, final double distanceToCamera) {
        // Render terminal content if close enough.
        if (distanceToCamera < 6f) {
            stack.pushPose();
            stack.translate(2, 2, -0.9f);

            // Scale to make terminal fit fully.
            final MonitorGUIRenderer terminal = monitor.getMonitor();
            final float textScaleX = 12f / MonitorDevice.WIDTH;
            final float textScaleY = 9f / MonitorDevice.HEIGHT;
            final float scale = Math.min(textScaleX, textScaleY) * 0.95f;

            // Center it on both axes.
            final float scaleDeltaX = textScaleX - scale;
            final float scaleDeltaY = textScaleY - scale;
            stack.translate(
                MonitorDevice.WIDTH * scaleDeltaX * 0.5f,
                MonitorDevice.HEIGHT * scaleDeltaY * 0.5f,
                0f);

            stack.scale(scale, scale, 1f);

            // TODO Make terminal renderer use buffer+rendertype.
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            try {
                rendererViews.get(terminal, () -> terminal.getRenderer(monitor)).render(stack, RenderSystem.getProjectionMatrix(), MonitorDevice.WIDTH, MonitorDevice.HEIGHT, true);
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

    private void renderStatusText(MultiBufferSource bufferSource, final MonitorBlockEntity monitor, final PoseStack stack, final double distanceToCamera) {
        if (distanceToCamera > 12f) {
            return;
        }

        final Component bootError = Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY);

        stack.pushPose();
        stack.translate(3, 3, -0.9f);

        drawText(bufferSource, stack, bootError);

        stack.popPose();
    }

    private void drawText(MultiBufferSource bufferSource, final PoseStack stack, final Component text) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final List<FormattedText> wrappedText = font.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = font.width(text);
            draw(bufferSource, font, stack, text, (maxWidth - textWidth) * 0.5f);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                draw(bufferSource, font, stack, wrappedText.get(i).getString(), i * font.lineHeight);
            }
        }

        stack.popPose();
    }

    private void draw(MultiBufferSource bufferSource, Font font, PoseStack stack, Component text, float x) {
        font.drawInBatch(text, x, (float) 0, 15610658, false, stack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }

    private void draw(MultiBufferSource bufferSource, Font font, PoseStack stack, String text, float y) {
        font.drawInBatch(text, (float) 0, y, 15610658, false, stack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880, false);
    }

    private void renderPower(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderQuad(matrix, TEXTURE_POWER.buffer(bufferSource, ModRenderType::getUnlitBlock));
    }

    private static void renderQuad(final Matrix4f matrix, final VertexConsumer consumer) {
        consumer.addVertex(matrix, 0, 0, 0)
            .setUv(0, 0);

        consumer.addVertex(matrix, 0, 16, 0)
            .setUv(0, 1);

        consumer.addVertex(matrix, 16, 16, 0)
            .setUv(1, 1);

        consumer.addVertex(matrix, 16, 0, 0)
            .setUv(1, 0);
    }

    @SubscribeEvent
    public static void updateCache(final ClientTickEvent.Pre event) {
        rendererViews.cleanUp();
    }

    private static void handleNoLongerRendering(final RemovalNotification<MonitorGUIRenderer, MonitorGUIRenderer.RendererView> notification) {
        final MonitorGUIRenderer key = notification.getKey();
        final MonitorGUIRenderer.RendererView value = notification.getValue();
        if (key != null && value != null) {
            key.releaseRenderer(value);
        }
    }
}
