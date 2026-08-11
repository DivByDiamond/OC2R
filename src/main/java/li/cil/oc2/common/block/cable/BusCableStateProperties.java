package li.cil.oc2.common.block.cable;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.world.LevelUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BusCableStateProperties {
    public static final BooleanProperty HAS_CABLE = BooleanProperty.create("has_cable");
    public static final BooleanProperty HAS_FACADE = BooleanProperty.create("has_facade");
    public static final EnumProperty<ConnectionType> CONNECTION_NORTH =
            EnumProperty.create("connection_north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_EAST =
            EnumProperty.create("connection_east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_SOUTH =
            EnumProperty.create("connection_south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_WEST =
            EnumProperty.create("connection_west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_UP =
            EnumProperty.create("connection_up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_DOWN =
            EnumProperty.create("connection_down", ConnectionType.class);

    public static final Map<Direction, EnumProperty<ConnectionType>> FACING_TO_CONNECTION_MAP =
            Util.make(
                    Maps.newEnumMap(Direction.class),
                    directions -> {
                        directions.put(Direction.NORTH, CONNECTION_NORTH);
                        directions.put(Direction.EAST, CONNECTION_EAST);
                        directions.put(Direction.SOUTH, CONNECTION_SOUTH);
                        directions.put(Direction.WEST, CONNECTION_WEST);
                        directions.put(Direction.UP, CONNECTION_UP);
                        directions.put(Direction.DOWN, CONNECTION_DOWN);
                    });

    public static ConnectionType getConnectionType(
            final BlockState state, @Nullable final Direction direction) {
        if (direction != null) {
            return state.getValue(FACING_TO_CONNECTION_MAP.get(direction));
        } else {
            return ConnectionType.NONE;
        }
    }

    public static int getInterfaceCount(final BlockState state) {
        int partCount = 0;
        for (final EnumProperty<ConnectionType> connectionType :
                FACING_TO_CONNECTION_MAP.values()) {
            if (state.getValue(connectionType) == ConnectionType.INTERFACE) {
                partCount++;
            }
        }
        return partCount;
    }

    public static Direction getHitSide(final BlockPos pos, final BlockHitResult hit) {
        final Vec3 localHitPos = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        return Direction.getNearest(localHitPos.x, localHitPos.y, localHitPos.z);
    }

    public static boolean addInterface(
            final Level level, final BlockPos pos, final BlockState state, final Direction side) {
        if (!state.getBlock().equals(Blocks.BUS_CABLE.get())) {
            return false;
        }
        if (state.getValue(HAS_FACADE)) {
            return false;
        }
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);
        if (state.getValue(property) != ConnectionType.NONE) {
            return false;
        }
        level.setBlock(
                pos,
                state.setValue(property, ConnectionType.INTERFACE),
                Block.UPDATE_ALL_IMMEDIATE);
        onConnectionTypeChanged(level, pos, side, false);
        return true;
    }

    public static boolean addCable(final Level level, final BlockPos pos, final BlockState state) {
        if (!state.getBlock().equals(Blocks.BUS_CABLE.get())) {
            return false;
        }
        if (state.getValue(HAS_CABLE)) {
            return false;
        }
        level.setBlock(pos, state.setValue(HAS_CABLE, true), Block.UPDATE_ALL_IMMEDIATE);
        onConnectionTypeChanged(level, pos, null, false);
        return true;
    }

    public static void setHasFacade(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            @Nullable final BlockState facadeState,
            final boolean value) {
        if (state.getValue(HAS_FACADE) == value) {
            return;
        }
        level.setBlock(pos, state.setValue(HAS_FACADE, value), Block.UPDATE_ALL_IMMEDIATE);
        final BlockState soundsSource = facadeState != null ? facadeState : state;
        LevelUtils.playSound(
                level,
                pos,
                soundsSource.getSoundType(level, pos, null),
                value ? SoundType::getPlaceSound : SoundType::getBreakSound);
    }

    static boolean canHaveCableTo(final BlockState state, final Direction side) {
        return state.getBlock().equals(Blocks.BUS_CABLE.get())
                && state.getValue(HAS_CABLE)
                && state.getValue(FACING_TO_CONNECTION_MAP.get(side)) != ConnectionType.INTERFACE;
    }

    static int getPartCount(final BlockState state) {
        int partCount = getInterfaceCount(state);
        if (state.getValue(HAS_CABLE)) {
            partCount++;
        }
        return partCount;
    }

    static void onConnectionTypeChanged(
            final LevelAccessor level,
            final BlockPos pos,
            @Nullable final Direction face,
            final boolean neighborConnectionChanged) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final BusCableBlockEntity busCable) {
            busCable.handleConfigurationChanged(face, neighborConnectionChanged);
        }
    }
}