package li.cil.oc2.client.renderer.blockentity.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MonitorScreenGeometryTest {
    @Test
    void screenStaysFlushWithCubeFaceForEveryFacingAndMultiblockSize() {
        for (final Direction facing : new Direction[] {
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            for (final int width : new int[] {1, 2, 8}) {
                for (final int height : new int[] {1, 3, 8}) {
                    final PoseStack pose = new PoseStack();
                    MonitorScreenGeometry.orient(pose, facing, width, height);
                    MonitorScreenGeometry.translateToSurface(pose, 0, 0);
                    final Vector3f topLeft = point(pose, 0, 0);
                    final Vector3f topRight = point(pose, 16, 0);
                    final Vector3f bottomLeft = point(pose, 0, 16);
                    final Vector3f bottomRight = point(pose, 16, 16);
                    assertEquals(width, topLeft.distance(topRight), 0.00001);
                    assertEquals(height, topLeft.distance(bottomLeft), 0.00001);
                    for (final Vector3f point : new Vector3f[] {topLeft, topRight, bottomLeft, bottomRight}) {
                        final float outwardDistance =
                                (point.x - 0.5f) * facing.getStepX()
                                        + (point.z - 0.5f) * facing.getStepZ() - 0.5f;
                        assertTrue(outwardDistance > 0 && outwardDistance < 0.001f,
                                () -> "Screen floats away from " + facing + " cube face: " + outwardDistance);
                    }
                }
            }
        }
    }

    private static Vector3f point(final PoseStack pose, final float x, final float y) {
        return pose.last().pose().transformPosition(new Vector3f(x, y, 0));
    }
}
