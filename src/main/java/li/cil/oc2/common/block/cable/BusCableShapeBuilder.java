package li.cil.oc2.common.block.cable;

import java.util.Arrays;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.types.ConnectionType;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class BusCableShapeBuilder {
    static VoxelShape[] makeShapes() {
        final VoxelShape ownCableBounds = Block.box(5, 5, 5, 11, 11, 11);
        final VoxelShape[] cableShapes = new VoxelShape[Constants.BLOCK_FACE_COUNT];
        final VoxelShape[] interfaceShapes = new VoxelShape[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            cableShapes[i] = getCableShape(Constants.DIRECTIONS[i]);
            interfaceShapes[i] = getInterfaceShape(Constants.DIRECTIONS[i]);
        }

        final int configurations = 1 << (6 + 6 + 1);
        final VoxelShape[] result = new VoxelShape[configurations];
        Arrays.fill(result, Shapes.empty());

        for (int i = 0; i < result.length; i++) {
            final int mask = i >> 1;
            for (int sideIndex = 0; sideIndex < Constants.BLOCK_FACE_COUNT; sideIndex++) {
                final int cableBit = 1 << sideIndex;
                if ((mask & cableBit) != 0) {
                    result[i] = Shapes.or(result[i], cableShapes[sideIndex]);
                }

                final int interfaceBit = cableBit << 6;
                if ((mask & interfaceBit) != 0) {
                    result[i] = Shapes.or(result[i], interfaceShapes[sideIndex]);
                }
            }

            if ((i & 1) != 0) {
                result[i] = Shapes.or(result[i], ownCableBounds);
            }
        }

        return result;
    }

    private static VoxelShape getCableShape(final Direction zDirection) {
        final int xSize = 6;
        final int ySize = 6;
        final int zSize = 5;

        final Direction yDirection =
                zDirection.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        final Direction xDirection =
                zDirection.getAxis() == Direction.Axis.Y
                        ? Direction.WEST
                        : zDirection.getClockWise();

        final Vec3i min =
                new Vec3i(8, 8, 8)
                        .relative(xDirection, -xSize / 2)
                        .relative(yDirection, -ySize / 2)
                        .relative(zDirection, 8 - zSize);
        final Vec3i max =
                new Vec3i(8, 8, 8)
                        .relative(xDirection, xSize / 2)
                        .relative(yDirection, ySize / 2)
                        .relative(zDirection, 8);

        final AABB bounds =
                new AABB(
                        Vec3.atLowerCornerOf(min).scale(1 / 16.0),
                        Vec3.atLowerCornerOf(max).scale(1 / 16.0));

        return Shapes.create(bounds);
    }

    private static VoxelShape getInterfaceShape(final Direction zDirection) {
        final int xSize = 8;
        final int ySize = 8;
        final int zSize = 1;

        final Direction yDirection =
                zDirection.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        final Direction xDirection =
                zDirection.getAxis() == Direction.Axis.Y
                        ? Direction.WEST
                        : zDirection.getClockWise();

        final Vec3i min =
                new Vec3i(8, 8, 8)
                        .relative(xDirection, -xSize / 2)
                        .relative(yDirection, -ySize / 2)
                        .relative(zDirection, 8 - zSize);
        final Vec3i max =
                new Vec3i(8, 8, 8)
                        .relative(xDirection, xSize / 2)
                        .relative(yDirection, ySize / 2)
                        .relative(zDirection, 8);

        final AABB bounds =
                new AABB(
                        Vec3.atLowerCornerOf(min).scale(1 / 16.0),
                        Vec3.atLowerCornerOf(max).scale(1 / 16.0));

        return Shapes.or(getCableShape(zDirection), Shapes.create(bounds));
    }

    static int getShapeIndex(final BlockState state) {
        int index = 0;

        for (int sideIndex = 0; sideIndex < Constants.BLOCK_FACE_COUNT; sideIndex++) {
            final int cableBit = 1 << sideIndex;
            final int interfaceBit = cableBit << 6;
            final ConnectionType connectionType = state.getValue(
                    BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(
                            Constants.DIRECTIONS[sideIndex]));
            if (connectionType == ConnectionType.CABLE) {
                index |= cableBit;
            } else if (connectionType == ConnectionType.INTERFACE) {
                index |= interfaceBit;
            }
        }

        index = index << 1;

        if (state.getValue(BusCableStateProperties.HAS_CABLE)) {
            index |= 1;
        }

        return index;
    }
}