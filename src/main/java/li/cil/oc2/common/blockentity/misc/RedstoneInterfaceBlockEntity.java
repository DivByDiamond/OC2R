package li.cil.oc2.common.blockentity.misc;

import static java.util.Collections.singletonList;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.IEventSink;
import li.cil.oc2.api.bus.device.rpc.RPCEventSource;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.integration.util.BundledRedstone;
import li.cil.oc2.common.util.HorizontalBlockUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public final class RedstoneInterfaceBlockEntity extends ModBlockEntity
        implements NamedDevice, DocumentedDevice, RPCEventSource {
    private static final String GET_REDSTONE_INPUT = "getRedstoneInput";
    private static final String GET_REDSTONE_OUTPUT = "getRedstoneOutput";
    private static final String SET_REDSTONE_OUTPUT = "setRedstoneOutput";
    private static final String GET_BUNDLED_INPUT = "getBundledInput";
    private static final String GET_BUNDLED_OUTPUT = "getBundledOutput";
    private static final String SET_BUNDLED_OUTPUT = "setBundledOutput";
    private static final String SET_BUNDLED_OUTPUTS = "setBundledOutputs";
    private static final String SIDE = "side";
    private static final String VALUE = "value";
    private static final String VALUES = "values";
    private static final String COLOUR = "colour";

    private final RedstoneInterfaceState state = new RedstoneInterfaceState();
    private final RedstoneInterfaceEventDispatcher eventDispatcher =
            new RedstoneInterfaceEventDispatcher();

    public RedstoneInterfaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.REDSTONE_INTERFACE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        state.saveAdditional(tag);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state.loadAdditional(tag);
    }

    public int getOutputForDirection(final Direction direction) {
        return state.getOutputForDirection(getBlockState(), direction);
    }

    @Callback(name = GET_REDSTONE_INPUT)
    public int getRedstoneInput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        if (level == null) return 0;

        final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
        assert direction != null;

        final BlockPos neighborPos = getBlockPos().relative(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) return 0;

        return level.getSignal(neighborPos, direction);
    }

    @Callback(name = GET_REDSTONE_OUTPUT, synchronize = false)
    public int getRedstoneOutput(@Parameter(SIDE) @Nullable final Side side) {
        if (side == null) throw new IllegalArgumentException();
        return state.getOutput(side.getDirection().get3DDataValue());
    }

    @Callback(name = SET_REDSTONE_OUTPUT)
    public void setRedstoneOutput(
            @Parameter(SIDE) @Nullable final Side side, @Parameter(VALUES) final int value) {
        if (side == null) throw new IllegalArgumentException();
        final int index = side.getDirection().get3DDataValue();
        final byte clampedValue = (byte) Mth.clamp(value, 0, 15);
        if (clampedValue == state.getOutput(index)) return;

        state.setOutput(index, clampedValue);
        final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
        if (direction != null) notifyNeighbor(direction);
        setChanged();
    }

    @Nullable
    @Callback(name = GET_BUNDLED_INPUT)
    public byte[] getBundledInput(@Parameter(SIDE) @Nullable final Side side) {
        if (!ModList.get().isLoaded("projectred_transmission")) throw new IllegalStateException();
        if (side == null) throw new IllegalArgumentException();

        final BundledRedstone bundledRedstone = BundledRedstone.getInstance();
        if (bundledRedstone.isAvailable()) {
            return bundledRedstone.getBundledInput(
                    level, getBlockPos(), side.getDirection().getOpposite());
        }
        return new byte[Constants.BLOCK_FACE_COUNT];
    }

    @Callback(name = GET_BUNDLED_OUTPUT)
    public byte[] getBundledOutput(@Parameter(SIDE) @Nullable final Side side) {
        if (!ModList.get().isLoaded("projectred_transmission")) throw new IllegalStateException();
        if (side == null) throw new IllegalArgumentException();
        return state.getBundledOutput(side.getDirection().get3DDataValue());
    }

    @Callback(name = SET_BUNDLED_OUTPUT)
    public void setBundledOutput(
            @Parameter(SIDE) @Nullable final Side side,
            @Parameter(VALUE) final int value,
            @Parameter(COLOUR) final int color) {
        if (!ModList.get().isLoaded("projectred_transmission")) throw new IllegalStateException();
        if (side == null) throw new IllegalArgumentException();

        final int index = side.getDirection().getOpposite().get3DDataValue();
        final byte clampedValue = (byte) Mth.clamp(value, 0, 255);
        final byte clampedColor = (byte) Mth.clamp(color, 0, 15);

        boolean changed = false;
        if (state.getBundledOutput(index)[clampedColor] != clampedValue) {
            changed = true;
            state.getBundledOutput(index)[clampedColor] = clampedValue;
        }

        if (changed) {
            final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
            if (direction != null) notifyNeighbor(direction);
            setChanged();
        }
    }

    @Callback(name = SET_BUNDLED_OUTPUTS)
    public void setBundledOutputs(
            @Parameter(SIDE) @Nullable final Side side, @Parameter(VALUES) final int[] values) {
        if (!ModList.get().isLoaded("projectred_transmission")) throw new IllegalStateException();
        if (side == null) throw new IllegalArgumentException();

        boolean changed = false;
        final int index = side.getDirection().getOpposite().get3DDataValue();
        final byte[] output = state.getBundledOutput(index);
        for (int i = 0; i < values.length; i++) {
            final byte clampedValue = (byte) Mth.clamp(values[i], 0, 255);
            if (clampedValue != output[i]) {
                output[i] = clampedValue;
                changed = true;
            }
        }

        if (changed) {
            final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
            if (direction != null) notifyNeighbor(direction);
            setChanged();
        }
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("redstone");
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        RedstoneInterfaceDocs.getDeviceDocumentation(visitor);
    }

    private void notifyNeighbor(final Direction direction) {
        if (level == null) return;
        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        level.updateNeighborsAt(getBlockPos().relative(direction), getBlockState().getBlock());
    }

    @Nullable
    public byte[] getBundledSignal(final Direction direction) {
        return state.getBundledSignal(direction);
    }

    @Override
    public void subscribe(final IEventSink sink, final UUID id) {
        eventDispatcher.subscribe(sink, id);
    }

    @Override
    public void unsubscribe(final IEventSink sink) {
        eventDispatcher.unsubscribe(sink);
    }

    public void neighborChanged(final BlockPos fromPos) {
        eventDispatcher.neighborChanged(level, getBlockPos(), getBlockState(), fromPos);
    }
}
