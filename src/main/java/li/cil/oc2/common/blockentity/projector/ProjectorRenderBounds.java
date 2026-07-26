package li.cil.oc2.common.blockentity.projector;

import li.cil.oc2.common.block.ProjectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

final class ProjectorRenderBounds {
    private AABB renderBounds;

    AABB get() {
        return renderBounds;
    }

    void update(final BlockState state, final BlockPos pos) {
        final Direction blockFacing = state.getValue(ProjectorBlock.FACING);
        final Direction canvasUp = Direction.UP;
        final Direction canvasLeft = blockFacing.getCounterClockWise();

        final BlockPos screenBasePos = pos.relative(blockFacing, ProjectorBlockEntity.MAX_RENDER_DISTANCE);
        final BlockPos screenMinPos = screenBasePos.relative(canvasLeft.getOpposite(), ProjectorBlockEntity.MAX_WIDTH / 2);
        final BlockPos screenMaxPos = screenBasePos.relative(canvasLeft, ProjectorBlockEntity.MAX_WIDTH / 2)
            .relative(canvasUp, ProjectorBlockEntity.MAX_HEIGHT - 2);

        renderBounds = new AABB(pos).minmax(new AABB(screenMinPos)).minmax(new AABB(screenMaxPos));
    }
}
