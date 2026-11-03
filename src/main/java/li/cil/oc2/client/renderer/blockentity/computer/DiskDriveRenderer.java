package li.cil.oc2.client.renderer.blockentity.computer;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import li.cil.oc2.client.RenderCompat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public final class DiskDriveRenderer<T extends net.minecraft.world.level.block.entity.BlockEntity
        & li.cil.oc2.common.blockentity.disk.FloppyVisualSource>
        implements BlockEntityRenderer<T, DiskDriveRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState floppy = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        int neighborLight;
    }

    public static DiskDriveRenderer<li.cil.oc2.common.blockentity.media.MediaStationBlockEntity> forMediaStation(
            final BlockEntityRendererProvider.Context context) {
        return new DiskDriveRenderer<>(context);
    }

    public DiskDriveRenderer(final BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            final T diskDrive,
            final State state,
            final float partialTicks,
            final Vec3 cameraPosition,
            @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                diskDrive, state, partialTicks, cameraPosition, breakProgress);
        state.facing = diskDrive.getBlockState().getValue(
                li.cil.oc2.common.block.media.MediaStationBlock.FACING);
        state.neighborLight =
                diskDrive.getLevel() != null
                        ? RenderCompat.lightCoords(
                                diskDrive.getLevel(),
                                diskDrive.getBlockPos().relative(state.facing))
                        : state.lightCoords;
        itemModelResolver.updateForTopItem(
                state.floppy,
                diskDrive.getRenderedFloppy(),
                ItemDisplayContext.FIXED,
                diskDrive.getLevel(),
                null,
                (int) diskDrive.getBlockPos().asLong());
    }

    @Override
    public void submit(
            final State state,
            final PoseStack stack,
            final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        if (state.floppy.isEmpty()) {
            return;
        }

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(state.facing.toYRot()));
        stack.translate(0.0f, 0.0f, 0.5f);
        stack.mulPose(Axis.XN.rotationDegrees(90));
        stack.translate(0.0f, 0.2375f, 2.5f / 16f);
        stack.scale(0.55f, 0.55f, 0.55f);

        state.floppy.submit(stack, collector, state.neighborLight, OverlayTexture.NO_OVERLAY, 0);

        stack.popPose();
    }
}
*///?} else {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DiskDriveRenderer<T extends net.minecraft.world.level.block.entity.BlockEntity
        & li.cil.oc2.common.blockentity.disk.FloppyVisualSource> implements BlockEntityRenderer<T> {
    private final BlockEntityRenderDispatcher renderer;

    public static DiskDriveRenderer<li.cil.oc2.common.blockentity.media.MediaStationBlockEntity> forMediaStation(
            final BlockEntityRendererProvider.Context context) {
        return new DiskDriveRenderer<>(context);
    }

    public DiskDriveRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(
            final T diskDrive,
            final float partialTicks,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final int light,
            final int overlay) {
        final ItemStack floppy = diskDrive.getRenderedFloppy();
        final Direction blockFacing = diskDrive.getBlockState().getValue(
                li.cil.oc2.common.block.media.MediaStationBlock.FACING);
        final int neighborLight =
                LevelRenderer.getLightColor(
                        renderer.level, diskDrive.getBlockPos().relative(blockFacing));
        final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot()));
        stack.translate(0.0f, 0.0f, 0.5f);
        stack.mulPose(Axis.XN.rotationDegrees(90));
        stack.translate(0.0f, 0.2375f, 2.5f / 16f);
        stack.scale(0.55f, 0.55f, 0.55f);

        itemRenderer.renderStatic(
                floppy,
                ItemDisplayContext.FIXED,
                neighborLight,
                overlay,
                stack,
                bufferSource,
                null,
                (int) diskDrive.getBlockPos().asLong());

        stack.popPose();
    }
}
//?}
