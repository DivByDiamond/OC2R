package li.cil.oc2.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

final class NetworkCableConnection {
    private static final Vec3 POS_Y = new Vec3(0, 1, 0);

    public final BlockPos fromPos, toPos;
    public final Vec3 from, to, forward, right;
    public final AABB bounds;

    NetworkCableConnection(final BlockPos fromPos, final BlockPos toPos) {
        if (fromPos.compareTo(toPos) > 0) {
            this.fromPos = toPos;
            this.toPos = fromPos;
        } else {
            this.fromPos = fromPos;
            this.toPos = toPos;
        }

        from = Vec3.atCenterOf(this.fromPos);
        to = Vec3.atCenterOf(this.toPos);
        forward = to.subtract(from).normalize();
        right =
                this.fromPos.getX() == this.toPos.getX() && this.fromPos.getZ() == this.toPos.getZ()
                        ? null
                        : forward.cross(POS_Y);
        bounds = new AABB(from, to).inflate(0, 0.5f, 0);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NetworkCableConnection that = (NetworkCableConnection) o;
        return fromPos.equals(that.fromPos) && toPos.equals(that.toPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromPos, toPos);
    }
}
