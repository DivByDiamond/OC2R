package li.cil.oc2.client.gui.screen.computer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ComputerPreviewGeometryTest {
    @Test
    void dragAnglesUseDegreesAndFullTurnRestoresTheFront() {
        final var normal = new Vector3f(-1, 0, 0)
                .rotate(ComputerPreviewGeometry.rotation(new Vector3f(0, 90, 0)));
        assertEquals(0, normal.x, 0.00001f);
        assertEquals(0, normal.y, 0.00001f);
        assertEquals(1, normal.z, 0.00001f);
        assertNull(ComputerPreviewGeometry.focusedSide(0, 0, new Vector3f(0, 360, 0)));
    }

    @Test
    void centerRaySelectsTheRotatedFaceAndLeavesTheFrontUnavailable() {
        assertNull(ComputerPreviewGeometry.focusedSide(0, 0, new Vector3f()));
        assertEquals(Direction.WEST, ComputerPreviewGeometry.focusedSide(0, 0, new Vector3f(0, 90, 0)));
        assertEquals(Direction.EAST, ComputerPreviewGeometry.focusedSide(0, 0, new Vector3f(0, -90, 0)));
        assertEquals(Direction.NORTH, ComputerPreviewGeometry.focusedSide(0, 0, new Vector3f(0, 180, 0)));
        assertNull(ComputerPreviewGeometry.focusedSide(500, 500, new Vector3f(0, 90, 0)));
    }

    @Test
    void topAndBottomPickingFollowTheirProjectedFaceCenters() {
        final var angles = new Vector3f();
        final var center = new Vector3f();
        for (final var side : new Direction[] {Direction.UP, Direction.DOWN}) {
            angles.set(side == Direction.UP ? 30 : -30, 0, 0);
            center.set(0, side.getStepY() * 0.5f, 0)
                    .rotate(ComputerPreviewGeometry.rotation(angles));
            assertEquals(side, ComputerPreviewGeometry.focusedSide(
                    -center.x * 48, center.y * 48, angles));
        }
    }
}
