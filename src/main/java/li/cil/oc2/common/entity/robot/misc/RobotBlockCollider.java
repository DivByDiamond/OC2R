package li.cil.oc2.common.entity.robot.misc;

import java.util.List;
import li.cil.oc2.common.entity.Robot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RobotBlockCollider {
    private final Robot robot;
    private final BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();

    public RobotBlockCollider(final Robot robot) {
        this.robot = robot;
    }

    public void collideWithWorld() {
        if (!(robot.level() instanceof final ServerLevel serverLevel)) {
            return;
        }

        // Only clear obstructing blocks while the robot is actually moving or rotating.
        // Without this check a stationary robot (or one stuck overlapping a block, e.g. due to
        // a piston, chunk regen or being placed into a block) would continuously "eat" terrain.
        if (!robot.getMovementController().hasQueuedActions()) {
            return;
        }

        final VoxelShape shape = Shapes.create(robot.getBoundingBox());
        final Cursor3D iterator = getBlockPosIterator();
        while (iterator.advance()) {
            final int x = iterator.nextX();
            final int y = iterator.nextY();
            final int z = iterator.nextZ();
            mutablePosition.set(x, y, z);
            final BlockState blockState = serverLevel.getBlockState(mutablePosition);
            if (blockState.isAir()
                    || blockState.is(Blocks.MOVING_PISTON)
                    || blockState.is(Blocks.PISTON_HEAD)) {
                continue;
            }

            final VoxelShape blockShape =
                    blockState.getCollisionShape(serverLevel, mutablePosition);
            if (Shapes.joinIsNotEmpty(shape, blockShape.move(x, y, z), BooleanOp.AND)) {
                final BlockEntity blockEntity = serverLevel.getBlockEntity(mutablePosition);
                // NOPMD: builder accumulates per-block loot parameters
                final LootParams.Builder builder =
                        new LootParams.Builder(serverLevel) // NOPMD allocation depends on loop iteration / per-item state
                                .withParameter(LootContextParams.THIS_ENTITY, robot)
                                .withParameter(LootContextParams.ORIGIN, robot.position())
                                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                .withParameter(LootContextParams.BLOCK_STATE, blockState)
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
                final List<ItemStack> drops = blockState.getDrops(builder);
                serverLevel.setBlockAndUpdate(mutablePosition, Blocks.AIR.defaultBlockState());
                for (final ItemStack drop : drops) {
                    Block.popResource(serverLevel, mutablePosition, drop);
                }
            }
        }
    }

    private Cursor3D getBlockPosIterator() {
        final AABB bounds = robot.getBoundingBox();
        return new Cursor3D(
                Mth.floor(bounds.minX),
                Mth.floor(bounds.minY),
                Mth.floor(bounds.minZ),
                Mth.floor(bounds.maxX),
                Mth.floor(bounds.maxY),
                Mth.floor(bounds.maxZ));
    }
}