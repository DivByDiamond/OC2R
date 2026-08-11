package li.cil.oc2.client.renderer.blockentity.computer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.time.Duration;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.blockentity.OverlayRenderer;
import li.cil.oc2.common.block.computer.ComputerBlock;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.vm.terminal.RendererView;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;

@SuppressWarnings("unused")
@EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID)
public final class ComputerRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    public static final ResourceLocation OVERLAY_POWER_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/computer/computer_overlay_power");
    public static final ResourceLocation OVERLAY_STATUS_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/computer/computer_overlay_status");
    public static final ResourceLocation OVERLAY_TERMINAL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    API.MOD_ID, "block/computer/computer_overlay_terminal");

    static final Cache<Terminal, RendererView> rendererViews =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(Duration.ofSeconds(5))
                    .removalListener(ComputerRenderer::handleNoLongerRendering)
                    .build();

    private final BlockEntityRenderDispatcher renderer;
    private final TerminalTextRenderer terminalTextRenderer;

    public ComputerRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
        this.terminalTextRenderer = new TerminalTextRenderer(context.getFont(), rendererViews);
    }

    @Override
    public void render(
            final ComputerBlockEntity computer,
            final float partialTicks,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final int light,
            final int overlay) {
        final ComputerBlockEntity terminalSource = computer.terminalManager.getPrimaryForContraptionRendering();

        final Direction blockFacing = computer.getBlockState().getValue(ComputerBlock.FACING);

        final Matrix4f poseMatrix = stack.last().pose();
        final Vec3 blockWorldPosRelativeToCamera =
                new Vec3(poseMatrix.m30(), poseMatrix.m31(), poseMatrix.m32());
        final Vec3 blockCenterRelativeToCamera = blockWorldPosRelativeToCamera.add(0.5, 0.5, 0.5);

        final Vec3 blockCenterToCamera = blockCenterRelativeToCamera.scale(-1);
        final double projectedCameraPosition =
                blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
        if (!computer.terminalManager.isContraptionVirtualClone() && projectedCameraPosition <= 0) {
            return;
        }

        final double distanceToCamera = blockCenterRelativeToCamera.length();

        stack.pushPose();

        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot() + 180));
        stack.translate(-0.5f, 0, -0.5f);

        stack.translate(1, 1, 1);
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
        final Matrix4f matrix = stack.last().pose();

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
                switch (terminalSource.getVirtualMachine().getRunState()) {
                    case STOPPED:
                        break;
                    case LOADING_DEVICES:
                        OverlayRenderer.renderStatus(matrix, bufferSource);
                        break;
                    case RUNNING:
                        OverlayRenderer.renderPower(matrix, bufferSource);
                        break;
                    default:
                        throw new AssertionError(terminalSource.getVirtualMachine().getRunState());
                }
                break;
            default:
                throw new AssertionError(terminalSource.getVirtualMachine().getBusState());
        }

        stack.popPose();
    }

    @SubscribeEvent
    public static void updateCache(final ClientTickEvent.Pre event) {
        rendererViews.cleanUp();
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