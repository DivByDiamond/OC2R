package li.cil.oc2.common.blockentity.network.connector;

import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The rules deciding whether two connectors may be linked: identity, level, capacity, distance
 * and line of sight. Extracted from {@link NetworkConnectorConnectionManager} so the manager stays
 * focused on connection bookkeeping.
 */
public final class NetworkConnectorConnectionValidator {
    static final int MAX_CONNECTION_DISTANCE = 16;

    private NetworkConnectorConnectionValidator() {
    }

    /**
     * Checks whether the two connectors may be linked, without touching any state.
     *
     * @return {@code null} when the connectors may be linked, otherwise the failure to report.
     */
    static ConnectionResult validate(
            final NetworkConnectorBlockEntity connectorA,
            final NetworkConnectorBlockEntity connectorB) {
        if (areInvalid(connectorA, connectorB)) {
            return ConnectionResult.FAILURE;
        }

        final Level level = connectorA.getLevel();
        if (!isValidLevel(level, connectorB)) {
            return ConnectionResult.FAILURE;
        }

        if (!canConnectMore(connectorA, connectorB)) {
            return ConnectionResult.FAILURE_FULL;
        }

        final BlockPos posA = connectorA.getBlockPos();
        final BlockPos posB = connectorB.getBlockPos();

        if (!posA.closerThan(posB, MAX_CONNECTION_DISTANCE)) {
            return ConnectionResult.FAILURE_TOO_FAR;
        }

        if (isObstructed(level, posA, posB)) {
            return ConnectionResult.FAILURE_OBSTRUCTED;
        }

        return null;
    }

    public static boolean isObstructed(final Level level, final BlockPos a, final BlockPos b) {
        final Vec3 va = Vec3.atCenterOf(a);
        final Vec3 vb = Vec3.atCenterOf(b);
        final Vec3 ab = vb.subtract(va).normalize().scale(0.5);

        final BlockHitResult hitAB =
                level.clip(
                        new ClipContext(
                                va.add(ab),
                                vb.subtract(ab),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                (CollisionContext) null));
        final BlockHitResult hitBA =
                level.clip(
                        new ClipContext(
                                vb.subtract(ab),
                                va.add(ab),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                (CollisionContext) null));

        return hitAB.getType() != HitResult.Type.MISS || hitBA.getType() != HitResult.Type.MISS;
    }

    private static boolean areInvalid(
            final NetworkConnectorBlockEntity connectorA,
            final NetworkConnectorBlockEntity connectorB) {
        return connectorA.equals(connectorB)
                || !connectorA.isValid()
                || !connectorB.isValid();
    }

    private static boolean isValidLevel(
            final Level level, final NetworkConnectorBlockEntity connectorB) {
        return level != null
                && !level.isClientSide()
                && level.equals(connectorB.getLevel());
    }

    private static boolean canConnectMore(
            final NetworkConnectorBlockEntity connectorA,
            final NetworkConnectorBlockEntity connectorB) {
        return connectorA.connectionManager.canConnectMore()
                && connectorB.connectionManager.canConnectMore();
    }
}
