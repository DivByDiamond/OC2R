package li.cil.oc2.client.model.monitor;

import java.util.List;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.block.monitor.MonitorMultiblock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Texture tables and model data for the fragment-based monitor model, ported from
 * OpenComputers' {@code Textures.Block.Screen} and {@code ScreenModel}.
 * <p>
 * Fragment textures live in {@code textures/block/monitor_oc/}. The tables below mirror the
 * OC pitch=0 tables (our monitors are always upright) but are indexed with our multiblock
 * convention (origin at the top-right corner, width growing toward the viewer's left, height
 * growing down).
 */
public final class MonitorModelTypes {
    public static final ModelProperty<MonitorData> MONITOR_PROPERTY = new ModelProperty<>();

    /** Multiblock position of a single monitor block, derived from its BlockState. */
    public record MonitorData(int width, int height, int offsetX, int offsetY, Direction facing) {}

    private static final String SINGLE_FRONT = "f";
    private static final String[] SINGLE = {"b", "b", "b2", "b2", "b2", "b2"};
    private static final String[] VERTICAL_FRONT = {"fvt", "fvm", "fvb2"};
    private static final String[][] VERTICAL = {
        {"b", "b", "bvt", "bvt", "bvt", "bvt"},
        {"b", "b", "bvm", "bvm", "bvm", "bvm"},
        {"b", "b", "bvb2", "bvb2", "bvb2", "bvb2"}
    };
    private static final String[] HORIZONTAL_FRONT = {"fhb2", "fhm2", "fht2"};
    private static final String[][] HORIZONTAL = {
        {"bht", "bhb", "bht2", "bht2", "b2", "b2"},
        {"bhm", "bhm", "bhm2", "bhm2", "b", "b"},
        {"bhb", "bht", "bhb2", "bhb2", "b2", "b2"}
    };
    private static final String[][] MULTI_FRONT = {
        {"ftr", "ftm", "ftl"},
        {"fmr", "fmm", "fml"},
        {"fbr2", "fbm2", "fbl2"}
    };
    private static final String[][][] MULTI = {
        {
            {"bht", "bhb", "btl", "btr", "bvb", "bvt"},
            {"bhm", "bhm", "btm", "btm", "b", "b"},
            {"bhb", "bht", "btr", "btl", "bvt", "bvb"}
        },
        {
            {"b", "b", "bml", "bmr", "bvm", "bvm"},
            {"b", "b", "bmm", "bmm", "b", "b"},
            {"b", "b", "bmr", "bml", "bvm", "bvt"}
        },
        {
            {"bht", "bhb", "bbl2", "bbr2", "bvt", "bvb2"},
            {"bhm", "bhm", "bbm2", "bbm2", "b", "b"},
            {"bhb", "bht", "bbr2", "bbl2", "bvb2", "bvt"}
        }
    };

    /** All fragment textures referenced by the tables, resolved into the atlas at bake time. */
    static final List<String> TEXTURE_NAMES =
            List.of(
                    "b", "b2",
                    "bvt", "bvm", "bvb2",
                    "bht", "bhb", "bht2", "bhm", "bhm2", "bhb2",
                    "btl", "btr", "btm", "bml", "bmr", "bmm", "bbl2", "bbr2", "bbm2",
                    "f", "fvt", "fvm", "fvb2",
                    "fhb2", "fhm2", "fht2",
                    "ftr", "ftm", "ftl", "fmr", "fmm", "fml", "fbr2", "fbm2", "fbl2");

    /** The particle icon texture. */
    static final String PARTICLE_TEXTURE = "bmm";

    private MonitorModelTypes() {}

    public static ModelData fromState(final BlockState state) {
        return ModelData.of(
                MONITOR_PROPERTY,
                new MonitorData(
                        state.getValue(MonitorBlock.WIDTH),
                        state.getValue(MonitorBlock.HEIGHT),
                        state.getValue(MonitorBlock.ORIGIN_OFFSET_X),
                        state.getValue(MonitorBlock.ORIGIN_OFFSET_Y),
                        state.getValue(MonitorBlock.FACING)));
    }

    public static ResourceLocation texture(final String name) {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/monitor_oc/" + name);
    }

    /**
     * Pick the fragment texture for the given world-facing {@code side} of a monitor block at
     * the multiblock position described by {@code data}.
     */
    public static String spriteForFace(final MonitorData data, final Direction side) {
        final Direction facing = data.facing();
        final int w = data.width();
        final int h = data.height();
        final Direction widthDir = MonitorMultiblock.getWidthDir(facing);

        final int ocSide = ocSideForFace(facing, widthDir, side);

        // OC computes the fragment position from its origin (bottom-left). Our origin is the
        // top-right, so both axes are mirrored before applying OC's xy2part mapping.
        int px = xy2part(w - 1 - data.offsetX(), w - 1);
        int py = xy2part(h - 1 - data.offsetY(), h - 1);
        if (side == Direction.DOWN) {
            px = 2 - px;
            py = 2 - py;
        }

        return spriteForPosition(ocSide, w, h, px, py);
    }

    private static int ocSideForFace(
            final Direction facing, final Direction widthDir, final Direction side) {
        if (side == facing) {
            return 3; // local front
        } else if (side == facing.getOpposite()) {
            return 2; // local back
        } else if (side == Direction.UP) {
            return 1;
        } else if (side == Direction.DOWN) {
            return 0;
        } else if (side == widthDir) {
            return 4; // viewer's left
        } else {
            return 5; // viewer's right
        }
    }

    private static String spriteForPosition(
            final int ocSide, final int w, final int h, final int px, final int py) {
        if (ocSide == 3) {
            return spriteForFront(w, h, px, py);
        }
        return spriteForSide(ocSide, w, h, px, py);
    }

    private static String spriteForFront(
            final int w, final int h, final int px, final int py) {
        if (w == 1 && h == 1) return SINGLE_FRONT;
        if (w == 1) return VERTICAL_FRONT[py];
        if (h == 1) return HORIZONTAL_FRONT[px];
        return MULTI_FRONT[py][px];
    }

    private static String spriteForSide(
            final int ocSide, final int w, final int h, final int px, final int py) {
        if (w == 1 && h == 1) return SINGLE[ocSide];
        if (w == 1) return VERTICAL[py][ocSide];
        if (h == 1) return HORIZONTAL[px][ocSide];
        return MULTI[py][px][ocSide];
    }

    /**
     * Texture rotation applied to the top and bottom faces so the frame stays aligned with the
     * monitor's front edge. Side and front/back faces need no rotation.
     */
    public static int rotationForFace(final Direction facing, final Direction side) {
        if (side == Direction.UP) {
            return switch (facing) {
                case SOUTH -> 0;
                case NORTH -> 2;
                case WEST -> 1;
                default -> 3;
            };
        }
        if (side == Direction.DOWN) {
            return switch (facing) {
                case SOUTH -> 2;
                case NORTH -> 0;
                case WEST -> 1;
                default -> 3;
            };
        }
        return 0;
    }

    private static int xy2part(final int value, final int high) {
        if (value == 0) return 2;
        if (value == high) return 0;
        return 1;
    }
}
