package li.cil.oc2.common.blockentity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

final class NetworkConnectorConnectionValidator {
    static boolean isObstructed(final Level level, final BlockPos a, final BlockPos b) {
        final Vec3 va = Vec3.atCenterOf(a);
        final Vec3 vb = Vec3.atCenterOf(b);
        final Vec3 ab = vb.subtract(va).normalize().scale(0.5);

        final BlockHitResult hitAB = level.clip(new ClipContext(
            va.add(ab),
            vb.subtract(ab),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            (CollisionContext) null
        ));
        final BlockHitResult hitBA = level.clip(new ClipContext(
            vb.subtract(ab),
            va.add(ab),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            (CollisionContext) null
        ));

        return hitAB.getType() != HitResult.Type.MISS ||
            hitBA.getType() != HitResult.Type.MISS;
    }
}
