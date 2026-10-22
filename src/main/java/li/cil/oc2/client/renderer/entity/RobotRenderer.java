package li.cil.oc2.client.renderer.entity;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.state.RobotAnimationState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public final class RobotRenderer extends EntityRenderer<Robot, RobotRenderState> {
    private final RobotModel model;

    public RobotRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new RobotModel(context.bakeLayer(RobotModel.ROBOT_MODEL_LAYER));
    }

    @Override
    public RobotRenderState createRenderState() {
        return new RobotRenderState();
    }

    @Override
    public void extractRenderState(
            final Robot entity, final RobotRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        final RobotAnimationState animation = entity.getAnimationState();
        animation.update(partialTicks, entity.getRandom());
        state.baseOffsetY = animation.baseRenderOffsetY;
        state.topOffsetY = animation.topRenderOffsetY;
        state.topRotationY = animation.topRenderRotationY;

        // NB: we don't entityYaw given to use because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation). Haven't figured out where to
        //     alternatively prevent this wrapping or patch the prev value instead.
        final float partialRotation =
                Mth.degreesDifferenceAbs(entity.yRotO, entity.getYRot()) * partialTicks;
        state.rotation = Mth.approachDegrees(entity.yRotO, entity.getYRot(), partialRotation);
    }

    @Override
    public void submit(
            final RobotRenderState state,
            final PoseStack stack,
            final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        stack.pushPose();
        stack.mulPose(Axis.YN.rotationDegrees(state.rotation));

        model.setupAnim(state);

        final RenderType renderType = model.renderType(RobotModel.ROBOT_ENTITY_TEXTURE);
        collector.submitModelPart(
                model.top(), stack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        collector.submitModelPart(
                model.base(),
                stack,
                renderType,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                null);
        collector.submitModelPart(
                model.core(),
                stack,
                renderType,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                null);

        stack.popPose();

        super.submit(state, stack, collector, camera);
    }
}
*///?} else {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.state.RobotAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class RobotRenderer extends EntityRenderer<Robot> {
    private final RobotModel model;

    public RobotRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new RobotModel(context.bakeLayer(RobotModel.ROBOT_MODEL_LAYER));
    }

    @Override
    public ResourceLocation getTextureLocation(final Robot entity) {
        return RobotModel.ROBOT_ENTITY_TEXTURE;
    }

    @Override
    public void render(
            final Robot entity,
            final float entityYaw,
            final float partialTicks,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final int packedLight) {
        final RobotAnimationState state = entity.getAnimationState();
        state.update(partialTicks, entity.level().random);

        stack.pushPose();
        // NB: we don't entityYaw given to use because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation). Haven't figured out where to
        //     alternatively prevent this wrapping or patch the prev value instead.
        final float partialRotation =
                Mth.degreesDifferenceAbs(entity.yRotO, entity.getYRot()) * partialTicks;
        final float rotation = Mth.approachDegrees(entity.yRotO, entity.getYRot(), partialRotation);
        stack.mulPose(Axis.YN.rotationDegrees(rotation));

        model.setupAnim(entity, 0, 0, 0, 0, 0);

        final VertexConsumer consumer =
                bufferSource.getBuffer(model.renderType(getTextureLocation(entity)));
        model.renderToBuffer(stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        stack.popPose();
    }
}
//?}
