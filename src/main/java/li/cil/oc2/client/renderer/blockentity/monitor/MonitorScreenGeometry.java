package li.cil.oc2.client.renderer.blockentity.monitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

// Shared placement of framebuffer and status text on the full-cube monitor model.
final class MonitorScreenGeometry {
    // Model pixels: enough separation to avoid z-fighting, without a visible floating panel.
    private static final float SURFACE_EPSILON = 0.01f;

    private MonitorScreenGeometry() {}

    static void orient(final PoseStack stack, final Direction facing, final int width, final int height) {
        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(facing.toYRot() + 180));
        stack.translate(-0.5f, 0, -0.5f);
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);
        stack.scale(width / 16f, height / 16f, 1 / 16f);
        // Origin is the top-right block; move the normalized plane to the multiblock's left edge.
        stack.translate(-16f * (width - 1) / width, 0f, 0f);
    }

    static void translateToSurface(final PoseStack stack, final float x, final float y) {
        stack.translate(x, y, SURFACE_EPSILON);
    }
}
