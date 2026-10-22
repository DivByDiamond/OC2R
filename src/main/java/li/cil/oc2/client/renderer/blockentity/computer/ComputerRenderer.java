package li.cil.oc2.client.renderer.blockentity.computer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.time.Duration;
//? if >=26.1 {
/*import javax.annotation.Nullable;
*///?}
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.blockentity.OverlayRenderer;
import li.cil.oc2.common.block.computer.ComputerBlock;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.render.RendererView;
import li.cil.oc2.platform.event.ClientEvents;
//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if >=26.1 {
/*import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
*///?}
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

@SuppressWarnings("unused")
//? if >=26.1 {
/*public final class ComputerRenderer
        implements BlockEntityRenderer<ComputerBlockEntity, ComputerRenderer.State> {
*///?} else {
public final class ComputerRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
//?}
    public static final ResourceLocation OVERLAY_POWER_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/computer/computer_overlay_power");
    public static final ResourceLocation OVERLAY_STATUS_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/computer/computer_overlay_status");
    public static final ResourceLocation OVERLAY_TERMINAL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/computer/computer_overlay_terminal");
    /** Blink period of the status light while the bus runs but left elements out. */
    private static final int OVERFLOW_BLINK_PERIOD_MS = 1000;

    static final Cache<Terminal, RendererView> rendererViews =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(Duration.ofSeconds(5))
                    .removalListener(ComputerRenderer::handleNoLongerRendering)
                    .build();

    private final BlockEntityRenderDispatcher renderer;
    private final TerminalTextRenderer terminalTextRenderer;

    public ComputerRenderer(final BlockEntityRendererProvider.Context context) {
        //? if >=26.1 {
/*        this.renderer = context.blockEntityRenderDispatcher();
        this.terminalTextRenderer = new TerminalTextRenderer(context.font(), rendererViews);
*///?} else {
        this.renderer = context.getBlockEntityRenderDispatcher();
        this.terminalTextRenderer = new TerminalTextRenderer(context.getFont(), rendererViews);
        //?}
    }

    //? if >=26.1 {
/*    public static final class State extends BlockEntityRenderState {
        ComputerBlockEntity computer;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            final ComputerBlockEntity computer,
            final State state,
            final float partialTicks,
            final Vec3 cameraPosition,
            @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                computer, state, partialTicks, cameraPosition, breakProgress);
        state.computer = computer;
    }

    @Override
    public void submit(
            final State state,
            final PoseStack stack,
            final SubmitNodeCollector bufferSource,
            final CameraRenderState camera) {
        final ComputerBlockEntity computer = state.computer;
*///?} else {
    @Override
    public void render(
            final ComputerBlockEntity computer,
            final float partialTicks,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final int light,
            final int overlay) {
    //?}
        final ComputerBlockEntity terminalSource = computer.terminalManager.getPrimaryForContraptionRendering();

        final Direction blockFacing = computer.getBlockState().getValue(ComputerBlock.FACING);

        final Matrix4f poseMatrix = stack.last().pose();
        final Vec3 blockWorldPosRelativeToCamera =
                new Vec3(poseMatrix.m30(), poseMatrix.m31(), poseMatrix.m32());
        final Vec3 blockCenterRelativeToCamera = blockWorldPosRelativeToCamera.add(0.5, 0.5, 0.5);

        final Vec3 blockCenterToCamera = blockCenterRelativeToCamera.scale(-1);
        final double projectedCameraPosition =
                //? if >=26.1 {
/*                blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getUnitVec3i()));
*///?} else {
                blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
                //?}
        if (!computer.terminalManager.isContraptionVirtualClone() && projectedCameraPosition <= 0) {
            return;
        }

        final double distanceToCamera = blockCenterRelativeToCamera.length();

        stack.pushPose();

        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot() + 180));
        stack.translate(-0.5f, 0, -0.5f);

        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        if (terminalSource.getVirtualMachine().isRunning()) {
            terminalTextRenderer.renderTerminal(
                    stack, bufferSource, terminalSource.terminalManager.getTerminal(), distanceToCamera);
        } else {
            terminalTextRenderer.renderStatusText(
                    stack,
                    bufferSource,
                    distanceToCamera,
                    terminalSource.getVirtualMachine().getError());
        }

        stack.translate(0, 0, -0.1f);
        //? if >=26.1 {
/*        renderOverlays(terminalSource, stack, bufferSource);
*///?} else {
        final Matrix4f matrix = stack.last().pose();

        renderOverlays(terminalSource, matrix, bufferSource);
        //?}

        stack.popPose();
    }

    private void renderOverlays(
            final ComputerBlockEntity terminalSource,
            //? if >=26.1 {
/*            final PoseStack matrix,
            final SubmitNodeCollector bufferSource) {
*///?} else {
            final Matrix4f matrix,
            final MultiBufferSource bufferSource) {
        //?}
        switch (terminalSource.getVirtualMachine().getBusState()) {
            case SCAN_PENDING:
            case INCOMPLETE:
                OverlayRenderer.renderStatus(matrix, bufferSource);
                break;
            case TOO_COMPLEX:
                OverlayRenderer.renderStatus(matrix, bufferSource, 1000);
                break;
            case MULTIPLE_CONTROLLERS:
                OverlayRenderer.renderStatus(matrix, bufferSource, 250);
                break;
            case READY:
                renderReadyOverlay(terminalSource, matrix, bufferSource);
                break;
            default:
                throw new AssertionError(terminalSource.getVirtualMachine().getBusState());
        }
    }

    private void renderReadyOverlay(
            final ComputerBlockEntity terminalSource,
            //? if >=26.1 {
/*            final PoseStack matrix,
            final SubmitNodeCollector bufferSource) {
*///?} else {
            final Matrix4f matrix,
            final MultiBufferSource bufferSource) {
        //?}
        final VirtualMachine vm = terminalSource.getVirtualMachine();
        // The bus runs with the elements that fit; blink the status light (same period the old
        // TOO_COMPLEX state used) so a partial bus is never silent, on top of the normal overlay.
        final boolean overflowed = vm.getBusOverflow() > 0;
        switch (vm.getRunState()) {
            case STOPPED:
                if (overflowed) {
                    OverlayRenderer.renderStatus(matrix, bufferSource, OVERFLOW_BLINK_PERIOD_MS);
                }
                break;
            case LOADING_DEVICES:
                OverlayRenderer.renderStatus(
                        matrix, bufferSource, overflowed ? OVERFLOW_BLINK_PERIOD_MS : 0);
                break;
            case RUNNING:
                OverlayRenderer.renderPower(matrix, bufferSource);
                if (overflowed) {
                    OverlayRenderer.renderStatus(matrix, bufferSource, OVERFLOW_BLINK_PERIOD_MS);
                }
                break;
            default:
                throw new AssertionError(vm.getRunState());
        }
    }

    public static void registerEvents() {
        ClientEvents.CLIENT_TICK_START.register(() -> {
            rendererViews.cleanUp();
        });
    }

    static void handleNoLongerRendering(
            final RemovalNotification<Terminal, RendererView> notification) {
        final Terminal key = notification.getKey();
        final RendererView value = notification.getValue();
        if (key != null && value != null) {
            key.releaseRenderer(value);
        }
    }
}