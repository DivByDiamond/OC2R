package li.cil.oc2.common.entity.robot.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RobotBlockColliderTest {
    private static final AABB BOX = new AABB(10, 64, 20, 11, 65, 21);
    private static final AABB BLOCK_EAST = new AABB(10.9, 64, 20, 11.9, 65, 21);
    private static final AABB BLOCK_WEST = new AABB(9.1, 64, 20, 10.1, 65, 21);

    @Test
    void aRobotWithoutADirectionClearsNothing() {
        assertNull(RobotBlockCollider.leadingHalf(BOX, Vec3.ZERO));
    }

    @Test
    void onlyTheHalfFacingTheMoveIsCleared() {
        final AABB east = RobotBlockCollider.leadingHalf(BOX, new Vec3(0.7, 0, 0));
        assertEquals(new AABB(10.5, 64, 20, 11, 65, 21), east);
        assertTrue(east.intersects(BLOCK_EAST));
        assertFalse(east.intersects(BLOCK_WEST));
    }

    @Test
    void afterABounceTheOldObstacleIsBehindTheRobot() {
        final AABB west = RobotBlockCollider.leadingHalf(BOX, new Vec3(-0.3, 0, 0));
        assertFalse(west.intersects(BLOCK_EAST));
        assertTrue(west.intersects(BLOCK_WEST));
    }

    @Test
    void verticalAndDepthMovesUseTheirOwnAxis() {
        assertEquals(new AABB(10, 64.5, 20, 11, 65, 21), RobotBlockCollider.leadingHalf(BOX, new Vec3(0, 1, 0)));
        assertEquals(new AABB(10, 64, 20, 11, 64.5, 21), RobotBlockCollider.leadingHalf(BOX, new Vec3(0, -1, 0)));
        assertEquals(new AABB(10, 64, 20.5, 11, 65, 21), RobotBlockCollider.leadingHalf(BOX, new Vec3(0, 0, 1)));
        assertEquals(new AABB(10, 64, 20, 11, 65, 20.5), RobotBlockCollider.leadingHalf(BOX, new Vec3(0, 0, -1)));
    }
}
