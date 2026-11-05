package li.cil.oc2.common.blockentity.radio;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.common.blockentity.energy.ChargerBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.radio.RadioAccessPoints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Finds the robot dock stations (chargers) around a client. Nothing is registered or saved: the
 * docks are read from the block entities of the already loaded chunks within dock range, so an
 * unloaded, broken or replaced charger simply stops being found and no chunk is ever loaded.
 */
public final class DockStations {
    private DockStations() {}

    /** Every loaded dock whose square bounds may cover the given X/Z position. */
    public static List<RadioAccessPoints.Dock> near(final ServerLevel level, final double x, final double z) {
        final int range = Config.robotDockRange;
        final String dimension = dimension(level);
        final int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(x) - range - 1);
        final int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(x) + range + 1);
        final int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(z) - range - 1);
        final int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(z) + range + 1);
        final List<RadioAccessPoints.Dock> result = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (final BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof ChargerBlockEntity) || blockEntity.isRemoved()) continue;
                    final BlockPos pos = blockEntity.getBlockPos();
                    // The access point sits at the block centre, like a tower's radio.
                    result.add(new RadioAccessPoints.Dock(
                            dimension, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range));
                }
            }
        }
        return result;
    }

    private static String dimension(final ServerLevel level) {
        //? if >=26.1 {
        /*return level.dimension().identifier().toString();
        *///?} else {
        return level.dimension().location().toString();
        //?}
    }
}
