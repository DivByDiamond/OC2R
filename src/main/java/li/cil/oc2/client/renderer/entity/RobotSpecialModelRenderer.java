package li.cil.oc2.client.renderer.entity;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3fc;

// Item model renderer for the robot item on 26.x, the counterpart of RobotWithoutLevelRenderer on 1.21.1.
// Registered as oc2r:robot and referenced from items/robot.json through minecraft:special.
public final class RobotSpecialModelRenderer implements NoDataSpecialModelRenderer {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot");

    private final RobotModel model;

    public RobotSpecialModelRenderer(final RobotModel model) {
        this.model = model;
    }

    @Override
    public void submit(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final int overlayCoords, final boolean hasFoil, final int outlineColor) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        // The 6-argument overload exists on 26.1 and 26.2; the foil and outline arguments of the longer
        // overloads differ between them, and an item model needs neither.
        submitNodeCollector.submitModelPart(
            this.model.root(), poseStack, this.model.renderType(RobotModel.ROBOT_ENTITY_TEXTURE), lightCoords, overlayCoords, null
        );
        poseStack.popPose();
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        final PoseStack poseStack = new PoseStack();
        this.model.root().getExtentsForGui(poseStack, output);
    }

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(final SpecialModelRenderer.BakingContext context) {
            return new RobotSpecialModelRenderer(
                    new RobotModel(context.entityModelSet().bakeLayer(RobotModel.ROBOT_MODEL_LAYER)));
        }
    }
}
*///?}
