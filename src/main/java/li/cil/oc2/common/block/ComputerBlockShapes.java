package li.cil.oc2.common.block;

import li.cil.oc2.common.util.VoxelShapeUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class ComputerBlockShapes {
    static final VoxelShape NEG_Z_SHAPE = Shapes.or(
        Block.box(0, 0, 1, 16, 16, 16),
        Block.box(0, 15, 0, 16, 16, 1),
        Block.box(0, 0, 0, 16, 6, 1),
        Block.box(0, 0, 0, 1, 16, 1),
        Block.box(15, 0, 0, 16, 16, 1)
    );
    static final VoxelShape NEG_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_Z_SHAPE);
    static final VoxelShape POS_Z_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_X_SHAPE);
    static final VoxelShape POS_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(POS_Z_SHAPE);
}
