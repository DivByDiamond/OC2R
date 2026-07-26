package li.cil.oc2.client.renderer;

import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class ProjectorCameraEntity extends Player {
    private static ProjectorCameraEntity instance;

    public static ProjectorCameraEntity get(final Level level, final Vec3 pos, final float rotationY) {
        if (instance == null) {
            instance = new ProjectorCameraEntity(level, BlockPos.ZERO, rotationY);
        }

        instance.setLevel(level);
        instance.moveTo(pos.x(), pos.y(), pos.z(), rotationY, 0);

        return instance;
    }

    private ProjectorCameraEntity(final Level level, final BlockPos blockPos, final float rotationY) {
        super(level, blockPos, rotationY, FakePlayerUtils.getFakePlayerProfile());
    }

    @Override
    public float getViewYRot(final float partialTicks) {
        return yRotO;
    }

    @Override
    public float getViewXRot(final float partialTicks) {
        return xRotO;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return true;
    }
}
