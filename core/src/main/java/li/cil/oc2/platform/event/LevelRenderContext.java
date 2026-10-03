package li.cil.oc2.platform.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;

/**
 * Everything a level render listener needs, independent of the loader's render event type.
 *
 * @param stage the point in the level render the listener runs at
 * @param poseStack the pose stack of the level render
 * @param camera the camera the level is rendered from
 * @param deltaTracker the frame timing, for the partial tick
 * @param modelViewMatrix the model-view matrix of the level render
 * @param projectionMatrix the projection matrix of the level render
 */
public record LevelRenderContext(
        Stage stage,
        PoseStack poseStack,
        Camera camera,
        DeltaTracker deltaTracker,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix) {
    /** The level render stages mod code hooks into. */
    public enum Stage {
        AFTER_CUTOUT_BLOCKS,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_PARTICLES,
    }
}
