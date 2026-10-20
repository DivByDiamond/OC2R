package li.cil.oc2.client.gui.screen.computer;

//? if >=26.1 {
/*import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.gui.Textures;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
*///?}
//? if >=26.1 && <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}
//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
*///?}

//? if >=26.1 {
/*// Render the real computer item and its selectable face overlays into a GUI picture.
public final class ComputerPreviewRenderer extends PictureInPictureRenderer<ComputerPreviewRenderState> {
*///?}
    //? if >=26.1 && <26.2 {
    /*public ComputerPreviewRenderer(final MultiBufferSource.BufferSource buffers) {
        super(buffers);
    }
    *///?}

    //? if >=26.1 {
    /*@Override
    public Class<ComputerPreviewRenderState> getRenderStateClass() {
        return ComputerPreviewRenderState.class;
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale) {
        return height / 2f;
    }

    @Override
    protected String getTextureLabel() {
        return "oc2r:network_card_preview";
    }
    *///?}

    //? if >=26.2 {
    /*@Override
    protected void renderToTexture(final ComputerPreviewRenderState state, final PoseStack pose,
                                   final SubmitNodeCollector collector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        submit(state, pose, collector);
    }
    *///?} else if >=26.1 {
    /*@Override
    protected void renderToTexture(final ComputerPreviewRenderState state, final PoseStack pose) {
        final var renderer = Minecraft.getInstance().gameRenderer;
        renderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        final var features = renderer.getFeatureRenderDispatcher();
        submit(state, pose, features.getSubmitNodeStorage());
        features.renderAllFeatures();
    }
    *///?}

    //? if >=26.1 {
    /*private static void submit(final ComputerPreviewRenderState state, final PoseStack pose,
                               final SubmitNodeCollector collector) {
        // PiP supplies screen-space axes. Match the item renderer's model-space axes.
        pose.scale(1, -1, -1);
        final Quaternionf rotation = ComputerPreviewGeometry.rotation(
                new Vector3f(state.pitch(), state.yaw(), state.roll()));
        pose.mulPose(rotation);
        pose.pushPose();
        // The computer's front is NORTH; SOUTH is the unavailable face in card-local space.
        pose.mulPose(new Quaternionf().rotationY((float) Math.PI));
        state.item().submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        final Vector3f normal = new Vector3f();
        final Quaternionf faceRotation = new Quaternionf();
        for (final Direction side : Direction.values()) {
            normal.set(side.getStepX(), side.getStepY(), side.getStepZ()).rotate(rotation);
            if (side == Direction.SOUTH || normal.z <= 0.0001f) {
                continue;
            }
            pose.pushPose();
            pose.translate(side.getStepX() * 0.505f, side.getStepY() * 0.505f, side.getStepZ() * 0.505f);
            pose.mulPose(faceRotation.rotationTo(0, 0, 1,
                    side.getStepX(), side.getStepY(), side.getStepZ()));
            pose.translate(-0.5f, -0.5f, 0);
            final boolean enabled = (state.enabledFaces() & (1 << side.get3DDataValue())) != 0;
            overlay(pose, collector, enabled ? Textures.BLOCK_FACE_ENABLED_TEXTURE.location
                    : Textures.BLOCK_FACE_DISABLED_TEXTURE.location);
            if (side == state.focusedSide()) {
                pose.translate(0, 0, 0.001f);
                overlay(pose, collector, Textures.BLOCK_FACE_FOCUSED_TEXTURE.location);
            }
            pose.popPose();
        }
    }

    private static void overlay(final PoseStack pose, final SubmitNodeCollector collector,
                                final ResourceLocation texture) {
        collector.submitCustomGeometry(pose, ModRenderType.getFramebuffer(texture), (matrix, vertices) -> {
            vertices.addVertex(matrix, 0, 0, 0).setUv(0, 1);
            vertices.addVertex(matrix, 0, 1, 0).setUv(0, 0);
            vertices.addVertex(matrix, 1, 1, 0).setUv(1, 0);
            vertices.addVertex(matrix, 1, 0, 0).setUv(1, 1);
        });
    }
}
*///?}
