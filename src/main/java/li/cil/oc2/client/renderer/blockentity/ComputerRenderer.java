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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.terminal.RendererView;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.bus.CommonDeviceBusController;
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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID)
public final class ComputerRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation OVERLAY_POWER_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/computer/computer_overlay_power");
    public static final ResourceLocation OVERLAY_STATUS_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/computer/computer_overlay_status");
    public static final ResourceLocation OVERLAY_TERMINAL_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/computer/computer_overlay_terminal");

    private static final Material TEXTURE_POWER = new Material(InventoryMenu.BLOCK_ATLAS, OVERLAY_POWER_LOCATION);
    private static final Material TEXTURE_STATUS = new Material(InventoryMenu.BLOCK_ATLAS, OVERLAY_STATUS_LOCATION);
    private static final Material TEXTURE_TERMINAL = new Material(InventoryMenu.BLOCK_ATLAS, OVERLAY_TERMINAL_LOCATION);

    private static final Cache<Terminal, RendererView> rendererViews = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(ComputerRenderer::handleNoLongerRendering)
        .build();

    ///////////////////////////////////////////////////////////////////

    private final BlockEntityRenderDispatcher renderer;
    private final Font font;

    ///////////////////////////////////////////////////////////////////

    public ComputerRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
        this.font = context.getFont();
    }

    ///////////////////////////////////////////////////////////////////

    private static long lastDiagnosticLog = 0;

    @Override
    public void render(final ComputerBlockEntity computer, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        // When rendering a Sable / Create: Aeronautics contraption, the
        // BlockEntity passed to us is a "virtual clone" living at a far-away
        // virtual BlockPos (~20M blocks from origin). Its terminal never
        // receives ComputerTerminalOutputMessage updates because the server
        // sends those with the original BlockPos. Look up the primary
        // BlockEntity (same persistent device id, real BlockPos) and render
        // its terminal instead. Falls back to the passed-in BlockEntity when
        // not on a contraption.
        final ComputerBlockEntity terminalSource = computer.getPrimaryForContraptionRendering();

        // Diagnostic logging: log once per second per computer to figure out
        // whether BER is called at all, what VM state the client sees, and
        // whether the terminal has any content. This is critical for
        // debugging the "text not visible on block face" issue with
        // Sable/Sodium/Flywheel.
        final long now = System.currentTimeMillis();
        if (now - lastDiagnosticLog > 1000) {
            lastDiagnosticLog = now;
            final VMRunState runState = terminalSource.getVirtualMachine().getRunState();
            final CommonDeviceBusController.BusState busState = terminalSource.getVirtualMachine().getBusState();
            final Terminal terminal = terminalSource.getTerminal();
            // Count non-space characters in the visible portion of the
            // terminal buffer. The visible window is
            // [(lastRowToDisplayMax - HEIGHT) * WIDTH, lastRowToDisplayMax * WIDTH).
            // Checking indices [0, WIDTH) was misleading because after any
            // scroll those indices hold overwritten history, not the current
            // first row — which made a healthy terminal look empty in the log.
            int nonSpaceCount = 0;
            final int visibleStart = Math.max(0, (terminal.lastRowToDisplayMax - Terminal.HEIGHT) * Terminal.WIDTH);
            final int visibleEnd = Math.min(terminal.buffer.length, visibleStart + Terminal.WIDTH * Terminal.HEIGHT);
            for (int i = visibleStart; i < visibleEnd; i++) {
                if (terminal.buffer[i] != ' ') nonSpaceCount++;
            }
            // Also log what Minecraft.getInstance().level is vs what
            // computer.getLevel() is — if they differ, the computer is in a
            // Sable/VS2 ship sub-level, and network messages that look up
            // by pos in Minecraft.getInstance().level won't find it.
            final net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            final net.minecraft.client.multiplayer.ClientLevel mainLevel = mc.level;
            final net.minecraft.world.level.Level computerLevel = computer.getLevel();
            // Log the PoseStack translation so we can see where the block is
            // actually being rendered (vs. the raw BlockPos which may be a
            // Sable virtual position).
            final Matrix4f poseMatrix = stack.last().pose();
            LOGGER.info("[ComputerRenderer] BER called for computer at {} (virtualClone={}, terminalSourcePos={}). runState={}, busState={}, terminal visible nonSpace={}, computerLevel={}, mainMcLevel={}, sameLevel={}, pos={}, poseTranslation=({},{},{})",
                computer.getBlockPos(),
                computer.isContraptionVirtualClone(),
                terminalSource.getBlockPos(),
                runState, busState, nonSpaceCount,
                computerLevel != null ? computerLevel.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(computerLevel)) : "null",
                mainLevel != null ? mainLevel.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(mainLevel)) : "null",
                computerLevel == mainLevel,
                computer.getBlockPos(),
                poseMatrix.m30(), poseMatrix.m31(), poseMatrix.m32());
        }

        final Direction blockFacing = computer.getBlockState().getValue(ComputerBlock.FACING);

        // Compute the block's actual world position from the PoseStack.
        //
        // For a regular block, the PoseStack at the start of render() has
        // been translated by (blockPos - cameraPos), so its translation
        // column (m30, m31, m32) is the block's world position relative to
        // the camera. For a Sable / Create: Aeronautics contraption block,
        // the contraption renderer sets up the PoseStack to point at the
        // block's actual rendered position (contraption pos + local offset),
        // NOT at the raw BlockPos (which is a virtual coordinate ~20M blocks
        // away). Using the PoseStack translation here makes the front-face
        // cull test and the terminal-text-vs-texture distance test both work
        // correctly on contraptions.
        final Matrix4f poseMatrix = stack.last().pose();
        final Vec3 blockWorldPosRelativeToCamera = new Vec3(poseMatrix.m30(), poseMatrix.m31(), poseMatrix.m32());
        // Block corner is at blockWorldPosRelativeToCamera; center is +0.5 on each axis.
        final Vec3 blockCenterRelativeToCamera = blockWorldPosRelativeToCamera.add(0.5, 0.5, 0.5);

        // If viewer is not in front of the block we can skip the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        // For contraptions, the block may be rotated by the contraption transform; the
        // raw blockFacing from the block state may not match the rendered orientation.
        // To stay safe, only cull when we're definitely behind the block (strictly negative
        // projection) AND not on a contraption (where orientation is uncertain).
        //
        // blockCenterRelativeToCamera is the vector from the camera (origin in view space)
        // to the block's center. The vector from the block's center to the camera is its
        // negation. Projecting that onto the block's facing normal tells us whether the
        // camera is in front of (positive) or behind (negative) the block.
        final Vec3 blockCenterToCamera = blockCenterRelativeToCamera.scale(-1);
        final double projectedCameraPosition = blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
        if (!computer.isContraptionVirtualClone() && projectedCameraPosition <= 0) {
            return;
        }

        // Distance from the camera (eye) to the block's center, in world units.
        // Uses the PoseStack-derived position so it works for contraption blocks
        // whose raw BlockPos is a far-away virtual coordinate.
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

        if (terminalSource.getVirtualMachine().isRunning()) {
            renderTerminal(terminalSource, stack, bufferSource, distanceToCamera);
        } else {
            renderStatusText(terminalSource, stack, bufferSource, distanceToCamera);
        }

        stack.translate(0, 0, -0.1f);
        final Matrix4f matrix = stack.last().pose();

        switch (terminalSource.getVirtualMachine().getBusState()) {
            case SCAN_PENDING:
            case INCOMPLETE:
                renderStatus(matrix, bufferSource);
                break;
            case TOO_COMPLEX:
                renderStatus(matrix, bufferSource, 1000);
                break;
            case MULTIPLE_CONTROLLERS:
                renderStatus(matrix, bufferSource, 250);
                break;
            case READY:
                switch (terminalSource.getVirtualMachine().getRunState()) {
                    case STOPPED:
                        break;
                    case LOADING_DEVICES:
                        renderStatus(matrix, bufferSource);
                        break;
                    case RUNNING:
                        renderPower(matrix, bufferSource);
                        break;
                }
                break;
        }

        stack.popPose();
    }

    ///////////////////////////////////////////////////////////////////

    private void renderTerminal(final ComputerBlockEntity computer, final PoseStack stack, final MultiBufferSource bufferSource, final double distanceToCamera) {
        // Render terminal content if close enough.
        if (distanceToCamera < 6f) {
            stack.pushPose();
            stack.translate(2, 2, -0.9f);

            // Scale to make terminal fit fully.
            final Terminal terminal = computer.getTerminal();
            final float textScaleX = 12f / terminal.getWidth();
            final float textScaleY = 7f / terminal.getHeight();
            final float scale = Math.min(textScaleX, textScaleY) * 0.95f;

            // Center it on both axes.
            final float scaleDeltaX = textScaleX - scale;
            final float scaleDeltaY = textScaleY - scale;
            stack.translate(
                terminal.getWidth() * scaleDeltaX * 0.5f,
                terminal.getHeight() * scaleDeltaY * 0.5f,
                0f);

            stack.scale(scale, scale, 1f);

            // TODO Make terminal renderer use buffer+rendertype.
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            try {
                rendererViews.get(terminal, terminal::getRenderer).render(stack, RenderSystem.getProjectionMatrix(), true);
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

    private void renderStatusText(final ComputerBlockEntity computer, final PoseStack stack, MultiBufferSource bufferSource, final double distanceToCamera) {
        if (distanceToCamera > 12f) {
            return;
        }

        final Component bootError = computer.getVirtualMachine().getError();
        if (bootError == null) {
            return;
        }

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

    private void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderStatus(matrix, bufferSource, 0);
    }

    private void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource, final int frequency) {
        if (frequency <= 0 || (((System.currentTimeMillis() + hashCode()) / frequency) % 2) == 1) {
            renderQuad(matrix, TEXTURE_STATUS.buffer(bufferSource, ModRenderType::getUnlitBlock));
        }
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

    private static void handleNoLongerRendering(final RemovalNotification<Terminal, RendererView> notification) {
        final Terminal key = notification.getKey();
        final RendererView value = notification.getValue();
        if (key != null && value != null) {
            key.releaseRenderer(value);
        }
    }
}
