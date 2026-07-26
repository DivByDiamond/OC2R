package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.common.util.Vec3Utils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Predicate;

final class CableRenderUtils {
    private static final int CABLE_VERTEX_COUNT = 9;
    private static final float CABLE_THICKNESS = 0.025f;
    private static final float CABLE_LENGTH_FOR_MAX_SWING = 6f;
    private static final float CABLE_MAX_SWING_AMOUNT = 0.05f;
    private static final int CABLE_SWING_INTERVAL = 8000;
    private static final float CABLE_HANG_MIN = 0.1f;
    private static final float CABLE_HANG_MAX = 0.5f;
    private static final float CABLE_MAX_LENGTH = 8f;
    private static final Vector3f CABLE_COLOR = new Vector3f(0.0f, 0.33f, 0.4f);
    private static final int MAX_RENDER_DISTANCE = 100;

    private static final ArrayList<NetworkCablePoint> cablePoints = new ArrayList<>();

    private CableRenderUtils() {}

    static void renderCables(final BlockAndTintGetter level, final PoseStack stack, final Vec3 eye, final ArrayList<NetworkCableConnection> connections, final Predicate<AABB> filter) {
        final Matrix4f viewMatrix = stack.last().pose();

        final RenderType renderType = ModRenderType.getNetworkCable();
        final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        final float r = CABLE_COLOR.x();
        final float g = CABLE_COLOR.y();
        final float b = CABLE_COLOR.z();

        for (final NetworkCableConnection connection : connections) {
            final Vec3 p0 = connection.from;
            final Vec3 p1 = connection.to;

            if (!p0.closerThan(eye, MAX_RENDER_DISTANCE) && !p1.closerThan(eye, MAX_RENDER_DISTANCE)) {
                continue;
            }

            if (!filter.test(connection.bounds)) {
                continue;
            }

            final Vec3 p2 = animateCableSwing(
                lerp(p0, p1, 0.5f).subtract(0, computeCableHang(p0, p1), 0),
                connection.right,
                computeCableSwingAmount(p0, p1),
                connection.hashCode());

            final VertexConsumer consumer = bufferSource.getBuffer(renderType);

            cablePoints.clear();
            for (int i = 0; i < CABLE_VERTEX_COUNT; i++) {
                final float t = i / (CABLE_VERTEX_COUNT - 1f);
                final Vec3 p = quadraticBezier(p0, p1, p2, t);
                final Vec3 n = getExtrusionVector(eye, p, connection.forward);

                final BlockPos blockPos = new BlockPos(Vec3Utils.round(p));
                final int blockLight = level.getBrightness(LightLayer.BLOCK, blockPos);
                final int skyLight = level.getBrightness(LightLayer.SKY, blockPos);
                final int packedLight = LightTexture.pack(blockLight, skyLight);

                final Vector3f v0 = p.subtract(n).toVector3f();
                final Vector3f v1 = p.add(n).toVector3f();

                cablePoints.add(new NetworkCablePoint(v0, v1, packedLight));
            }

            for (int i = 0; i < cablePoints.size() - 1; i++) {
                final NetworkCablePoint pa = cablePoints.get(i);
                final NetworkCablePoint pb = cablePoints.get(i + 1);

                consumer.addVertex(viewMatrix, pa.v0().x(), pa.v0().y(), pa.v0().z())
                    .setColor(r, g, b, 1f)
                    .setUv2(pa.packedLight(), 0);
                consumer.addVertex(viewMatrix, pa.v1().x(), pa.v1().y(), pa.v1().z())
                    .setColor(r, g, b, 1f)
                    .setUv2(pa.packedLight(), 0);
                consumer.addVertex(viewMatrix, pb.v1().x(), pb.v1().y(), pb.v1().z())
                    .setColor(r, g, b, 1f)
                    .setUv2(pa.packedLight(), 0);
                consumer.addVertex(viewMatrix, pb.v0().x(), pb.v0().y(), pb.v0().z())
                    .setColor(r, g, b, 1f)
                    .setUv2(pa.packedLight(), 0);
            }

            bufferSource.endBatch(renderType);
        }
    }

    private static Vec3 lerp(final Vec3 a, final Vec3 b, final float t) {
        return a.add(b.subtract(a).scale(t));
    }

    private static Vec3 quadraticBezier(final Vec3 a, final Vec3 b, final Vec3 c, final float t) {
        final Vec3 a1 = lerp(a, c, t);
        final Vec3 b1 = lerp(c, b, t);
        return lerp(a1, b1, t);
    }

    private static Vec3 getExtrusionVector(final Vec3 eye, final Vec3 v, final Vec3 forward) {
        return forward.cross(eye.subtract(v)).normalize().scale(CABLE_THICKNESS);
    }

    private static float computeCableHang(final Vec3 a, final Vec3 b) {
        final double length = a.distanceTo(b);
        final double hangFactor = Mth.clamp(length / CABLE_MAX_LENGTH, 0, 1);
        return (float) (CABLE_HANG_MIN + (CABLE_HANG_MAX - CABLE_HANG_MIN) * hangFactor);
    }

    private static float computeCableSwingAmount(final Vec3 p0, final Vec3 p1) {
        return Mth.clamp((float) p0.distanceTo(p1) / CABLE_LENGTH_FOR_MAX_SWING, 0.1f, 1f) * CABLE_MAX_SWING_AMOUNT;
    }

    private static Vec3 animateCableSwing(final Vec3 c, @Nullable final Vec3 right, final float swingAmount, final int seed) {
        final float relTime = ((System.currentTimeMillis() + seed) % CABLE_SWING_INTERVAL) / (float) CABLE_SWING_INTERVAL;
        final float relRadialTime = relTime * 2 * (float) Math.PI;

        if (right == null) {
            return c.add(swingAmount * Mth.sin(relRadialTime),
                0,
                swingAmount * Mth.cos(relRadialTime));
        } else {
            return c.add(swingAmount * Mth.cos(relRadialTime) * right.x,
                0.5f * swingAmount * Mth.sin(relRadialTime * 2 - (float) Math.PI) - swingAmount,
                swingAmount * Mth.cos(relRadialTime) * right.z);
        }
    }
}
