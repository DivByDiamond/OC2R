package li.cil.oc2.common.entity.robot.misc;

import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.movement.RobotMovementAction;
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
import net.minecraft.world.phys.Vec3;
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

        // Only blocks in the way of a move in progress are cleared. A robot that stands still, turns,
        // has only queued work, or overlaps a block behind it (piston, chunk regeneration, being
        // placed into a block) must not eat terrain; after a bounce the old obstacle is behind it.
        if (!(robot.getMovementController().getCurrentAction() instanceof final RobotMovementAction movement)) {
            return;
        }
        final AABB leading = leadingHalf(robot.getBoundingBox(), movement.heading(robot));
        if (leading == null) {
            return;
        }

        final VoxelShape shape = Shapes.create(leading);
        final Cursor3D iterator = getBlockPosIterator(leading);
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
                        // allocation depends on loop iteration / per-item state
                        new LootParams.Builder(serverLevel) // NOPMD
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

    /**
     * The half of the box that faces the direction of travel, or null without a direction. Blocks
     * touching only the trailing half are not in the way.
     */
    @Nullable
    static AABB leadingHalf(final AABB box, final Vec3 heading) {
        final double x = Math.abs(heading.x);
        final double y = Math.abs(heading.y);
        final double z = Math.abs(heading.z);
        if (Math.max(x, Math.max(y, z)) < RobotMovementAction.TARGET_EPSILON) {
            return null;
        }
        final Vec3 center = box.getCenter();
        if (x >= y && x >= z) {
            return heading.x > 0
                    ? new AABB(center.x, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
                    : new AABB(box.minX, box.minY, box.minZ, center.x, box.maxY, box.maxZ);
        }
        if (y >= z) {
            return heading.y > 0
                    ? new AABB(box.minX, center.y, box.minZ, box.maxX, box.maxY, box.maxZ)
                    : new AABB(box.minX, box.minY, box.minZ, box.maxX, center.y, box.maxZ);
        }
        return heading.z > 0
                ? new AABB(box.minX, box.minY, center.z, box.maxX, box.maxY, box.maxZ)
                : new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, center.z);
    }

    private static Cursor3D getBlockPosIterator(final AABB bounds) {
        return new Cursor3D(
                Mth.floor(bounds.minX),
                Mth.floor(bounds.minY),
                Mth.floor(bounds.minZ),
                Mth.floor(bounds.maxX),
                Mth.floor(bounds.maxY),
                Mth.floor(bounds.maxZ));
    }
}
