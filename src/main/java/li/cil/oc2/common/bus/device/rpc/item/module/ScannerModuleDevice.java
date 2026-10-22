package li.cil.oc2.common.bus.device.rpc.item.module;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Comparator;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.rpc.item.AbstractItemRPCDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

public final class ScannerModuleDevice extends AbstractItemRPCDevice {
    /**
     * Result of an inspection.
     *
     * <p>The entity names are built for every call and are only serialised into the RPC reply
     * that the guest receives; the array itself stays on the server and is never mutated
     * afterwards.
     */
    @SuppressFBWarnings(
            value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification =
                    "the entity names are built per call and only serialised into the RPC reply;"
                        + " the guest never receives the array itself")
    public record Inspection(@Nullable String block, @Nullable String fluid, String[] entities) {
    }

    /**
     * Result of a scan.
     *
     * @param hardness one unsigned value per scanned block: 0 is air, 1 to 253 is the block
     *     hardness, 254 a fluid and 255 an unbreakable or unscannable block. The values are
     *     unsigned, so guests read 254 and 255 and not their signed byte counterparts.
     * @param entities the types of the entities in the scanned cube.
     */
    @SuppressFBWarnings(
            value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification =
                    "the hardness and entity arrays are built per scan and only serialised into"
                        + " the RPC reply; the guest never receives the arrays themselves")
    public record ScanResult(byte[] hardness, String[] entities) {
    }

    private static final String LAST_SCAN_TAG_NAME = "last_scan";

    private static final int SCAN_RADIUS = 3;
    private static final int SCAN_LENGTH = SCAN_RADIUS * 2 + 1;
    private static final int SCAN_LAYER_SIZE = SCAN_LENGTH * SCAN_LENGTH;
    private static final int SCAN_SIZE = SCAN_LAYER_SIZE * SCAN_LENGTH;
    private static final Duration SCAN_COOLDOWN = Duration.ofSeconds(5);
    private static final int MAX_ENTITIES = 20;

    private final Entity entity;
    private final ScannerCooldown cooldown = new ScannerCooldown(SCAN_COOLDOWN);
    @Nullable private byte[] lastScan;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification =
                    "the module scans around the live robot entity; every scan needs the current"
                        + " level and position of that very instance")
    public ScannerModuleDevice(final ItemStack identity, final Entity entity) {
        super(identity, "scanner");
        this.entity = entity;
    }

    @Override
    public void exportToItemStack(final CompoundTag nbt) {
        cooldown.serializeNBT(nbt);
        if (lastScan != null) {
            nbt.putByteArray(LAST_SCAN_TAG_NAME, lastScan);
        }
    }

    @Override
    public void importFromItemStack(final CompoundTag nbt) {
        cooldown.deserializeNBT(nbt);
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
                        : BuiltInRegistries.FLUID.getKey(ScannerBlocks.sourceFluid(fluid)).toString(),
                getEntities(new AABB(pos)));
    }

    @Callback(
            description =
                    "Measures the hardness of the blocks around the robot and lists nearby entities.",
            returnValueDescription =
                    "a table with the block `hardness` as unsigned numbers (0 to 255, where 254 is a"
                            + " fluid and 255 an unbreakable block) and the list of `entities` in the"
                            + " scanned cube.")
    public ScanResult scan() {
        final Level level = entity.level();
        if (cooldown.isRecharging()) {
            throw new IllegalStateException("scanner is recharging");
        }
        cooldown.begin();

        final BlockPos center = getCenter();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final byte[] hardness = new byte[SCAN_SIZE];
        int index = 0;
        for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
                    pos.setWithOffset(center, x, y, z);
                    hardness[index++] = ScannerBlocks.encode(ScannerBlocks.hardness(level, pos));
                }
            }
        }

        // The scan is kept for the per-layer queries, so it gets its own copy: the result
        // handed to the guest must not alias the cached scan.
        lastScan = hardness.clone();
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
            returnValueDescription =
                    "49 unsigned numbers (0 to 255), the hardness of each block in the layer.")
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
        return ScannerBlocks.isLoaded(level, pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    private String[] getEntities(final AABB bounds) {
        return entity.level().getEntities(entity, bounds, EntitySelector.NO_SPECTATORS).stream()
                .sorted(Comparator.comparingDouble(other -> other.distanceToSqr(entity)))
                .limit(MAX_ENTITIES)
                .map(other -> BuiltInRegistries.ENTITY_TYPE.getKey(other.getType()).toString())
                .toArray(String[]::new);
    }
}
