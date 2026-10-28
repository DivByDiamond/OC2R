package li.cil.oc2.common.blockentity.keyboard.link;

import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.monitor.MonitorMultiblock;
import li.cil.oc2.common.block.projector.ProjectorBlock;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.util.world.level.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Canonical input endpoints and server-side focus validation. */
public final class KeyboardTargets {
    private KeyboardTargets() {}

    /** Resolves monitor fragments to their origin without loading chunks. */
    @Nullable
    public static BlockEntity resolve(final Level level, final BlockPos pos) {
        final BlockEntity entity = LevelUtils.getBlockEntityIfChunkExists(level, pos);
        if (entity instanceof MonitorBlockEntity monitor) {
            final BlockPos origin = MonitorMultiblock.getOriginPos(pos, monitor.getBlockState());
            final BlockEntity master = LevelUtils.getBlockEntityIfChunkExists(level, origin);
            return master instanceof MonitorBlockEntity ? master : null;
        }
        return device(entity) == null ? null : entity;
    }

    /** Returns the persistent identity of a supported endpoint. */
    @Nullable
    public static UUID identity(@Nullable final BlockEntity entity) {
        if (entity instanceof ComputerBlockEntity computer) return computer.busElement.deviceId;
        if (entity instanceof MonitorBlockEntity monitor) return monitor.getDeviceId();
        if (entity instanceof ProjectorBlockEntity projector) return projector.getDeviceId();
        return null;
    }

    /** Returns the native input device; computer input never passes through its UART terminal. */
    @Nullable
    public static KeyboardDevice<?> device(@Nullable final BlockEntity entity) {
        if (entity instanceof ComputerBlockEntity computer) return computer.keyboardDevice;
        if (entity instanceof MonitorBlockEntity monitor) return monitor.getKeyboardDevice();
        if (entity instanceof ProjectorBlockEntity projector) return projector.keyboardDevice;
        return null;
    }

    /** Validates a raycast focus against the visible monitor or projector image. */
    @SuppressWarnings("PMD.CompareObjectsWithEquals") // A hit must resolve to the exact loaded block entity.
    public static boolean isLookedAt(final Player player, final BlockEntity endpoint) {
        final HitResult hit = player.pick(32, 0, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return false;
        final BlockEntity touched = resolve(player.level(), blockHit.getBlockPos());
        return touched == endpoint || endpoint instanceof ProjectorBlockEntity projector
                && projector.isProjecting() && projectsOnto(projector, hit.getLocation());
    }

    /** Tests the same off-axis perspective cone used by the projector renderer. */
    public static boolean projectsOnto(final ProjectorBlockEntity projector, final Vec3 point) {
        final Direction facing = projector.getBlockState().getValue(ProjectorBlock.FACING);
        final Vec3 direction = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        final Vec3 offset = point.subtract(Vec3.atCenterOf(projector.getBlockPos()).add(direction.scale(7.0 / 16)));
        final double depth = offset.dot(direction);
        final double sideways = Math.abs(offset.x * direction.z - offset.z * direction.x);
        return depth > 0 && depth <= ProjectorBlockEntity.MAX_RENDER_DISTANCE
                && sideways <= depth * 0.5 && offset.y >= 0 && offset.y <= depth * 0.75;
    }
}
