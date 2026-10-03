package li.cil.oc2.client.model.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent quad generation of the fragment-based monitor model, ported from
 * OpenComputers' {@code ScreenModel}. One quad per face of a full cube, picking the frame fragment
 * texture from the block's position inside the multiblock.
 */
public final class MonitorQuads {
    private static final MonitorModelTypes.MonitorData DEFAULT_DATA =
            new MonitorModelTypes.MonitorData(1, 1, 0, 0, Direction.NORTH);

    private static final Vec3[][] UNIT_CUBE = {
        {new Vec3(0, 0, 1), new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1)},
        {new Vec3(0, 1, 0), new Vec3(0, 1, 1), new Vec3(1, 1, 1), new Vec3(1, 1, 0)},
        {new Vec3(1, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 0), new Vec3(0, 1, 0)},
        {new Vec3(0, 1, 1), new Vec3(0, 0, 1), new Vec3(1, 0, 1), new Vec3(1, 1, 1)},
        {new Vec3(0, 1, 0), new Vec3(0, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 1)},
        {new Vec3(1, 1, 1), new Vec3(1, 0, 1), new Vec3(1, 0, 0), new Vec3(1, 1, 0)}
    };

    private static final Vec3[][] PLANES = {
        {new Vec3(1, 0, 0), new Vec3(0, 0, -1)}, // down
        {new Vec3(1, 0, 0), new Vec3(0, 0, 1)}, // up
        {new Vec3(-1, 0, 0), new Vec3(0, -1, 0)}, // north
        {new Vec3(1, 0, 0), new Vec3(0, -1, 0)}, // south
        {new Vec3(0, 0, 1), new Vec3(0, -1, 0)}, // west
        {new Vec3(0, 0, -1), new Vec3(0, -1, 0)} // east
    };

    private MonitorQuads() {}

    /**
     * The quads of a monitor block at the multiblock position {@code data} (the default if null) for
     * {@code side}; all faces if {@code side} is null.
     */
    public static List<BakedQuad> quads(
            final Map<String, TextureAtlasSprite> sprites,
            @Nullable final MonitorModelTypes.MonitorData data,
            @Nullable final Direction side) {
        final MonitorModelTypes.MonitorData monitor = data != null ? data : DEFAULT_DATA;
        if (side == null) {
            // Item rendering queries all faces at once; emit one quad per cube face.
            final List<BakedQuad> quads = new ArrayList<>(6);
            for (final Direction face : Direction.values()) {
                final TextureAtlasSprite sprite =
                        sprites.get(MonitorModelTypes.spriteForFace(monitor, face));
                if (sprite == null) {
                    continue;
                }
                quads.add(
                        bakeQuad(face, sprite, MonitorModelTypes.rotationForFace(monitor.facing(), face)));
            }
            return quads;
        }

        final TextureAtlasSprite sprite = sprites.get(MonitorModelTypes.spriteForFace(monitor, side));
        if (sprite == null) {
            return Collections.emptyList();
        }
        return List.of(
                bakeQuad(side, sprite, MonitorModelTypes.rotationForFace(monitor.facing(), side)));
    }

    private static BakedQuad bakeQuad(
            final Direction side, final TextureAtlasSprite texture, final int rotation) {
        final int[] data =
                quadData(UNIT_CUBE[side.get3DDataValue()], side, texture, rotation);
        return new BakedQuad(data, -1, side, texture, true);
    }

    private static int[] quadData(
            final Vec3[] vertices,
            final Direction facing,
            final TextureAtlasSprite texture,
            final int rotation) {
        final Vec3 uAxis = PLANES[facing.get3DDataValue()][0];
        final Vec3 vAxis = PLANES[facing.get3DDataValue()][1];
        final int rot = (rotation + 4) % 4;
        final int[] data = new int[vertices.length * 8];
        int i = 0;
        for (final Vec3 vertex : vertices) {
            float u = (float) vertex.dot(uAxis);
            float v = (float) vertex.dot(vAxis);
            if (uAxis.x() + uAxis.y() + uAxis.z() < 0) u = 1 + u;
            if (vAxis.x() + vAxis.y() + vAxis.z() < 0) v = 1 + v;
            for (int r = 0; r < rot; r++) {
                final float tmp = u;
                u = v;
                v = -(tmp - 0.5f) + 0.5f;
            }
            final int nx = (facing.getStepX() * 127) & 0xFF;
            final int ny = (facing.getStepY() * 127) & 0xFF;
            final int nz = (facing.getStepZ() * 127) & 0xFF;
            data[i++] = Float.floatToRawIntBits((float) vertex.x());
            data[i++] = Float.floatToRawIntBits((float) vertex.y());
            data[i++] = Float.floatToRawIntBits((float) vertex.z());
            data[i++] = 0xFFFFFFFF;
            data[i++] = Float.floatToRawIntBits(texture.getU(u));
            data[i++] = Float.floatToRawIntBits(texture.getV(v));
            data[i++] = 0;
            data[i++] = nx | (ny << 8) | (nz << 16);
        }
        return data;
    }
}
