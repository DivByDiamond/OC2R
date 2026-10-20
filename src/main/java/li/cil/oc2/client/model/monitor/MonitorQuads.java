//? if >=26.1 {
/*package li.cil.oc2.client.model.monitor;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.joml.Vector3fc;

// Quad generation of the fragment-based monitor model, ported from OpenComputers' ScreenModel.
// One quad per face of a full cube, picking the frame fragment texture from the block's position
// inside the multiblock. Same tables as the 1.21 version, emitted as 26.x BakedQuads.
public final class MonitorQuads {
    private static final MonitorModelTypes.MonitorData DEFAULT_DATA =
            new MonitorModelTypes.MonitorData(1, 1, 0, 0, Direction.NORTH);

    private static final float[][][] UNIT_CUBE = {
        {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
        {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
        {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
        {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
        {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
        {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}}
    };

    // U and V axis of each face, indexed like Direction#get3DDataValue.
    private static final float[][][] PLANES = {
        {{1, 0, 0}, {0, 0, -1}}, // down
        {{1, 0, 0}, {0, 0, 1}}, // up
        {{-1, 0, 0}, {0, -1, 0}}, // north
        {{1, 0, 0}, {0, -1, 0}}, // south
        {{0, 0, 1}, {0, -1, 0}}, // west
        {{0, 0, -1}, {0, -1, 0}} // east
    };

    private MonitorQuads() {}

    // All six faces of the monitor block at the multiblock position data (the default if null),
    // each registered for culling against its neighbour.
    public static QuadCollection quads(
            final Map<String, Material.Baked> materials,
            @Nullable final MonitorModelTypes.MonitorData data) {
        final MonitorModelTypes.MonitorData monitor = data != null ? data : DEFAULT_DATA;
        final QuadCollection.Builder builder = new QuadCollection.Builder();
        for (final Direction face : Direction.values()) {
            final Material.Baked material = materials.get(MonitorModelTypes.spriteForFace(monitor, face));
            if (material == null) {
                continue;
            }
            builder.addCulledFace(
                    face,
                    bakeQuad(face, material, MonitorModelTypes.rotationForFace(monitor.facing(), face)));
        }
        return builder.build();
    }

    private static BakedQuad bakeQuad(
            final Direction side, final Material.Baked material, final int rotation) {
        final float[][] vertices = UNIT_CUBE[side.get3DDataValue()];
        final float[] uAxis = PLANES[side.get3DDataValue()][0];
        final float[] vAxis = PLANES[side.get3DDataValue()][1];
        final float uSign = uAxis[0] + uAxis[1] + uAxis[2];
        final float vSign = vAxis[0] + vAxis[1] + vAxis[2];
        final int rot = (rotation + 4) % 4;

        final Vector3fc[] positions = new Vector3fc[4];
        final long[] packedUv = new long[4];
        for (int i = 0; i < 4; i++) {
            final float[] vertex = vertices[i];
            float u = dot(vertex, uAxis);
            float v = dot(vertex, vAxis);
            if (uSign < 0) u = 1 + u;
            if (vSign < 0) v = 1 + v;
            for (int r = 0; r < rot; r++) {
                final float tmp = u;
                u = v;
                v = -(tmp - 0.5f) + 0.5f;
            }
            positions[i] = new Vector3f(vertex[0], vertex[1], vertex[2]);
            packedUv[i] = UVPair.pack(material.sprite().getU(u), material.sprite().getV(v));
        }

        final BakedQuad.MaterialInfo info =
                BakedQuad.MaterialInfo.of(material, material.sprite().transparency(), -1, true, 0, true);
        return new BakedQuad(
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                packedUv[0],
                packedUv[1],
                packedUv[2],
                packedUv[3],
                side,
                info,
                BakedNormals.of(
                        BakedNormals.computeQuadNormal(
                                positions[0], positions[1], positions[2], positions[3])),
                BakedColors.DEFAULT);
    }

    private static float dot(final float[] a, final float[] b) { // NOPMD - This parameter is a complete fixed-shape array, not a variable argument list.
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
*///?} else {
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
//?}
