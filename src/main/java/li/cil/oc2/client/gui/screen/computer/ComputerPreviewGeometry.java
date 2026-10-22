package li.cil.oc2.client.gui.screen.computer;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** The shared orientation of the NIC preview model, its overlays and mouse picking. */
public final class ComputerPreviewGeometry {
    private static final AABB BOUNDS = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);

    private ComputerPreviewGeometry() {}

    public static Quaternionf rotation(final Vector3fc degrees) {
        final float radians = org.joml.Math.toRadians(1f);
        return new Quaternionf().rotationXYZ(
                degrees.x() * radians, degrees.y() * radians, degrees.z() * radians);
    }

    @Nullable
    public static Direction focusedSide(final float mouseX, final float mouseY, final Vector3fc degrees) {
        final Quaternionf inverse = rotation(degrees).conjugate();
        final float scale = ComputerBlockItemRenderer.BLOCK_RENDER_SIZE;
        final Vector3f source = new Vector3f(-mouseX / scale, mouseY / scale, 2).rotate(inverse);
        final Vector3f target = new Vector3f(-mouseX / scale, mouseY / scale, -2).rotate(inverse);
        return BOUNDS.clip(new Vec3(source), new Vec3(target))
                //? if >=26.1 {
                /*.map(hit -> Direction.getApproximateNearest(hit.x, hit.y, hit.z))
                *///?} else {
                .map(hit -> Direction.getNearest(hit.x, hit.y, hit.z))
                //?}
                .filter(side -> side != Direction.SOUTH)
                .orElse(null);
    }
}
