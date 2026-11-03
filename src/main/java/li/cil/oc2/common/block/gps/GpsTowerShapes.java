package li.cil.oc2.common.block.gps;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Collision stays local; selecting any part outlines the entire tower. */
final class GpsTowerShapes {
    private static final VoxelShape BASE = Shapes.block();
    private static final VoxelShape UPPER = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape[] OUTLINES = createOutlines();

    static VoxelShape collision(final int part) {
        return part == 0 ? BASE : UPPER;
    }

    static VoxelShape outline(final int part) {
        return OUTLINES[part];
    }

    private static VoxelShape[] createOutlines() {
        VoxelShape whole = BASE;
        for (int i = 1; i < GpsTowerStructure.HEIGHT; i++) {
            whole = Shapes.or(whole, UPPER.move(0, i, 0));
        }
        final VoxelShape[] result = new VoxelShape[GpsTowerStructure.HEIGHT];
        for (int i = 0; i < result.length; i++) {
            result[i] = whole.move(0, -i, 0);
        }
        return result;
    }

    private GpsTowerShapes() {}
}
