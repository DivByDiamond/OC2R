package li.cil.oc2.common.bus.device.rpc.item.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Block data of the scanner module.
 *
 * <p>The hardness of a block is reported as an unsigned value: 0 is air, 1 to 253 is the
 * block hardness, 254 is a fluid and 255 an unbreakable or unscannable block. The values are
 * stored in a signed byte array, but the RPC layer transports byte arrays as unsigned
 * numbers, so the guest reads 254 and 255 and not their signed byte counterparts -2 and -1.
 */
final class ScannerBlocks {
    static final int AIR = 0;
    static final int MAX = 253;
    static final int FLUID = 254;
    static final int UNBREAKABLE = 255;

    private ScannerBlocks() {}

    /** Whether the chunk containing the position is loaded and can be read. */
    static boolean isLoaded(final Level level, final BlockPos pos) {
        return level.hasChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    /** The unsigned hardness of the block at the position. */
    // Mojang deprecated BlockStateBase.liquid() without documenting a replacement; the cached
    // field read is still correct, and the sensitive NeoForge alternatives are unavailable from
    // this shared source set (it also compiles on Fabric).
    @SuppressWarnings("deprecation")
    static int hardness(final Level level, final BlockPos pos) {
        if (!isLoaded(level, pos)) {
            return UNBREAKABLE;
        }

        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return AIR;
        }
        if (state.liquid() || state.getBlock() instanceof LiquidBlock) {
            return FLUID;
        }

        final float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) {
            return UNBREAKABLE;
        }
        return Math.clamp(Math.round(hardness * 10), 1, MAX);
    }

    /** Encodes an unsigned hardness value (0-255) into the byte array used on the wire. */
    static byte encode(final int hardness) {
        return (byte) (hardness & 0xFF);
    }

    /** The source fluid of the given fluid state. */
    static Fluid sourceFluid(final FluidState fluid) {
        return fluid.getType() instanceof final FlowingFluid flowing
                ? flowing.getSource()
                : fluid.getType();
    }
}
