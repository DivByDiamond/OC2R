package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for Robot entities providing the Minecraft Entity lifecycle
 * overrides, synced data definitions, and passive collision behavior.
 *
 * <p>Subclasses must implement {@link #addAdditionalSaveData} and
 * {@link #readAdditionalSaveData} for entity-specific serialization.
 */
public abstract class AbstractRobotEntity extends Entity implements ICaptureInputStateStorage {
    public static final EntityDataAccessor<BlockPos> TARGET_POSITION =
            SynchedEntityData.defineId(AbstractRobotEntity.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<Direction> TARGET_DIRECTION =
            SynchedEntityData.defineId(AbstractRobotEntity.class, EntityDataSerializers.DIRECTION);
    public static final EntityDataAccessor<Byte> SELECTED_SLOT =
            SynchedEntityData.defineId(AbstractRobotEntity.class, EntityDataSerializers.BYTE);

    public static final int INVENTORY_SIZE = 12;

    private long lastPistonMovement;
    public boolean captureInputState;

    protected AbstractRobotEntity(final EntityType<?> type, final Level world) {
        super(type, world);
    }

    @Override
    public int getSelectedSlot() {
        return getEntityData().get(SELECTD_SLOT);
    }

    @Override
    public void setSelectedSlot(final int value) {
        getEntityData().set(SELECTED_SLOT, (byte) Mth.clamp(value, 0, INVENTORY_SIZE - 1));
    }

    @Override
    public boolean getCaptureInputState() {
        return captureInputState;
    }

    @Override
    public void setCaptureInputState(final boolean value) {
        this.captureInputState = value;
    }

    public long getLastPistonMovement() {
        return lastPistonMovement;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(TARGET_POSITION, BlockPos.ZERO);
        builder.define(TARGET_DIRECTION, Direction.NORTH);
        builder.define(SELECTED_SLOT, (byte) 0);
    }

    @Override
    protected abstract void addAdditionalSaveData(final CompoundTag tag);

    @Override
    protected abstract void readAdditionalSaveData(final CompoundTag tag);

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canCollideWith(final Entity entity) {
        return !entity.equals(this);
    }

    @Override
    public void push(final Entity entity) {}

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return false;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void checkInsideBlocks() {}

    @Override
    protected Vec3 limitPistonMovement(final Vec3 pos) {
        lastPistonMovement = level().getGameTime();
        return super.limitPistonMovement(pos);
    }
}
