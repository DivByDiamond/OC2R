//? if >=26.1 {
/*
package li.cil.oc2.client.renderer.projector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.ByteOrder;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.junit.jupiter.api.Test;

class ProjectorBlockEntityDepthTest {
    @Test
    void customModelKeepsItsActualTransformedGeometryUvAndVertexAlpha() {
        final RenderType type = opaqueQuads();
        final PoseStack pose = new PoseStack();
        pose.translate(11, 22, 33);
        try (var collector = new ProjectorBlockEntityDepth.Collector()) {
            collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
                for (int i = 0; i < 4; i++) {
                    buffer.addVertex(matrix, i * 0.125f, i * 0.25f, i * 0.5f)
                            .setUv(0, 0).setUv(0.25f, 0.75f).setColor(0x7FFFFFFF);
                }
            });
            try (MeshData mesh = collector.finish(type)) {
                assertNotNull(mesh);
                assertEquals(6, mesh.drawState().indexCount());
                final var vertices = mesh.vertexBuffer().order(ByteOrder.nativeOrder());
                assertEquals(11f, vertices.getFloat(0));
                assertEquals(22f, vertices.getFloat(4));
                assertEquals(33f, vertices.getFloat(8));
                assertEquals(0.25f, vertices.getFloat(12));
                assertEquals(0.75f, vertices.getFloat(16));
                assertEquals(0x7F, Byte.toUnsignedInt(vertices.get(23)));
                assertEquals(11.125f, vertices.getFloat(24));
                assertEquals(22.25f, vertices.getFloat(28));
                assertEquals(33.5f, vertices.getFloat(32));
            }
        }
    }

    @Test
    void translucentEffectsDoNotBecomeOpaqueOccludersAndOrdersShareTheCollector() {
        final RenderType type = opaqueQuads();
        when(type.hasBlending()).thenReturn(true);
        try (var collector = new ProjectorBlockEntityDepth.Collector()) {
            collector.order(7).submitCustomGeometry(new PoseStack(), type, (pose, buffer) -> fail("blended effect must not render"));
            assertNull(collector.finish(type));
        }
    }

    private static RenderType opaqueQuads() {
        final RenderType type = mock(RenderType.class);
        final RenderPipeline pipeline = mock(RenderPipeline.class);
        when(type.pipeline()).thenReturn(pipeline);
        when(pipeline.getDepthStencilState()).thenReturn(DepthStencilState.DEFAULT);
        //? if >=26.2 {
        when(type.primitiveTopology()).thenReturn(com.mojang.blaze3d.PrimitiveTopology.QUADS);
        //?} else {
        when(type.mode()).thenReturn(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
        //?}
        return type;
    }
}
*///?}
