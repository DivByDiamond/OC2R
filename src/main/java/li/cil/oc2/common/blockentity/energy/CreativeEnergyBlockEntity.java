package li.cil.oc2.common.blockentity.energy;

import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.InfiniteEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

public final class CreativeEnergyBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    private final Direction[] SIDES = Direction.values();

    public final InfiniteEnergyStorage energy = new InfiniteEnergyStorage();

    public CreativeEnergyBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.CREATIVE_ENERGY.get(), pos, state);
    }

    @Override
    public void serverTick() {
        assert level != null;

        for (final Direction side : SIDES) {
            final BlockPos neighborPos = getBlockPos().relative(side);
            final ChunkPos neighborChunkPos =
                    new ChunkPos(neighborPos); // NOPMD: depends on loop side
            if (level.hasChunk(neighborChunkPos.x, neighborChunkPos.z)) {
                final var energy =
                        level.getCapability(
                                Capabilities.EnergyStorage.BLOCK, neighborPos, side.getOpposite());
                if (energy != null) {
                    energy.receiveEnergy(Integer.MAX_VALUE, false);
                }
            }
        }
    }
}