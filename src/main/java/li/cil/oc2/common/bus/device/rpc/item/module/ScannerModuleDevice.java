package li.cil.oc2.common.bus.device.rpc.item.module;

import java.time.Duration;
import java.util.Comparator;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.rpc.item.AbstractItemRPCDevice;
import li.cil.oc2.common.util.tick.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

public final class ScannerModuleDevice extends AbstractItemRPCDevice {
    public record Inspection(@Nullable String block, @Nullable String fluid, String[] entities) {
    }

    public record ScanResult(byte[] hardness, String[] entities) {
    }

    private static final String READY_AT_TAG_NAME = "ready_at";
    private static final String LAST_SCAN_TAG_NAME = "last_scan";

    private static final int SCAN_RADIUS = 3;
    private static final int SCAN_LENGTH = SCAN_RADIUS * 2 + 1;
    private static final int SCAN_LAYER_SIZE = SCAN_LENGTH * SCAN_LENGTH;
    private static final int SCAN_SIZE = SCAN_LAYER_SIZE * SCAN_LENGTH;
    private static final int SCAN_COOLDOWN = TickUtils.toTicks(Duration.ofSeconds(5));
    private static final int MAX_ENTITIES = 20;

    private static final int HARDNESS_AIR = 0;
    private static final int HARDNESS_MAX = 253;
    private static final int HARDNESS_FLUID = 254;
    private static final int HARDNESS_UNBREAKABLE = 255;

    private final Entity entity;
    private long readyAt;
    @Nullable private byte[] lastScan;

    public ScannerModuleDevice(final ItemStack identity, final Entity entity) {
        super(identity, "scanner");
        this.entity = entity;
    }

    @Override
    public void exportToItemStack(final CompoundTag nbt) {
        nbt.putLong(READY_AT_TAG_NAME, readyAt);
        if (lastScan != null) {
            nbt.putByteArray(LAST_SCAN_TAG_NAME, lastScan);
        }
    }

    @Override
    public void importFromItemStack(final CompoundTag nbt) {
        //? if >=26.1 {
        /*readyAt = nbt.getLongOr(READY_AT_TAG_NAME, 0);
        *///?} else {
        readyAt = nbt.getLong(READY_AT_TAG_NAME);
        //?}
        //? if >=26.1 {
        /*lastScan = nbt.getByteArray(LAST_SCAN_TAG_NAME).orElse(null);
        *///?} else {
        lastScan =
                nbt.contains(LAST_SCAN_TAG_NAME, net.minecraft.nbt.Tag.TAG_BYTE_ARRAY)
                        ? nbt.getByteArray(LAST_SCAN_TAG_NAME)
                        : null;
        //?}
    }

    @Callback(
            description = "Identifies the block, fluid and entities in the space on the specified side of the robot.",
            returnValueDescription =
                    "a table with the `block` and `fluid`, if present, and a list of `entities`.")
    public Inspection inspect(
            @Parameter(value = "side", description = "the side to look at: `front`, `up` or `down`.")
                    @Nullable
                    final RobotOperationSide side) {
        final BlockPos pos = getAdjacent(side);
        final BlockState state = getBlockState(pos);
        final FluidState fluid = state.getFluidState();
        return new Inspection(
                state.isAir() ? null : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                fluid.isEmpty()
                        ? null
                        : BuiltInRegistries.FLUID.getKey(getSourceFluid(fluid)).toString(),
                getEntities(new AABB(pos)));
    }

    @Callback(
            description =
                    "Measures the hardness of the blocks around the robot and lists nearby entities.",
            returnValueDescription =
                    "a table with the block `hardness` as binary data and the list of `entities` in the scanned cube.")
    public ScanResult scan() {
        final Level level = entity.level();
        if (level.getGameTime() < readyAt) {
            throw new IllegalStateException("scanner is recharging");
        }
        readyAt = level.getGameTime() + SCAN_COOLDOWN;

        final BlockPos center = getCenter();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final byte[] hardness = new byte[SCAN_SIZE];
        int index = 0;
        for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
                    pos.setWithOffset(center, x, y, z);
                    hardness[index++] = (byte) getHardness(level, pos);
                }
            }
        }

        lastScan = hardness;
        return new ScanResult(hardness, getEntities(new AABB(center).inflate(SCAN_RADIUS)));
    }

    @Callback(
            description = "Checks whether the robot's space gets full sky light.",
            returnValueDescription = "whether the sky is visible.")
    public boolean canSeeSky() {
        return entity.level().canSeeSky(getCenter());
    }

    @Callback(
            description = "Reads one layer of the last scan. Fails if there was no scan yet.",
            returnValueDescription = "49 bytes, the hardness of each block in the layer.")
    public byte[] getScanLayer(
            @Parameter(value = "layer", description = "the layer, 0 to 6 from bottom to top.")
                    final int layer) {
        if (layer < 0 || layer >= SCAN_LENGTH) {
            throw new IllegalArgumentException(
                    "layer out of range: " + layer + " (expected 0 to " + (SCAN_LENGTH - 1) + ")");
        }
        if (lastScan == null) {
            throw new IllegalStateException("no scan yet");
        }
        final byte[] result = new byte[SCAN_LAYER_SIZE];
        System.arraycopy(lastScan, layer * SCAN_LAYER_SIZE, result, 0, SCAN_LAYER_SIZE);
        return result;
    }

    private BlockPos getCenter() {
        return BlockPos.containing(entity.getBoundingBox().getCenter());
    }

    private BlockPos getAdjacent(@Nullable final RobotOperationSide side) {
        if (side == null) throw new IllegalArgumentException();
        return entity.blockPosition().relative(RobotOperationSide.toGlobal(entity, side));
    }

    private BlockState getBlockState(final BlockPos pos) {
        final Level level = entity.level();
        return isLoaded(level, pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    private String[] getEntities(final AABB bounds) {
        return entity.level().getEntities(entity, bounds, EntitySelector.NO_SPECTATORS).stream()
                .sorted(Comparator.comparingDouble(other -> other.distanceToSqr(entity)))
                .limit(MAX_ENTITIES)
                .map(other -> BuiltInRegistries.ENTITY_TYPE.getKey(other.getType()).toString())
                .toArray(String[]::new);
    }

    // Mojang deprecated BlockStateBase.liquid() without documenting a replacement; the cached
    // field read is still correct, and the sensitive NeoForge alternatives are unavailable from
    // this shared source set (it also compiles on Fabric).
    @SuppressWarnings("deprecation")
    private static int getHardness(final Level level, final BlockPos pos) {
        if (!isLoaded(level, pos)) {
            return HARDNESS_UNBREAKABLE;
        }

        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return HARDNESS_AIR;
        }
        if (state.liquid() || state.getBlock() instanceof LiquidBlock) {
            return HARDNESS_FLUID;
        }

        final float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) {
            return HARDNESS_UNBREAKABLE;
        }
        return Math.clamp(Math.round(hardness * 10), 1, HARDNESS_MAX);
    }

    private static Fluid getSourceFluid(final FluidState fluid) {
        return fluid.getType() instanceof final FlowingFluid flowing
                ? flowing.getSource()
                : fluid.getType();
    }

    private static boolean isLoaded(final Level level, final BlockPos pos) {
        return level.hasChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }
}
