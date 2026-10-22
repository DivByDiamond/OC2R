package li.cil.oc2.common.vm.terminal.render;

//? if >=26.1 {
/*import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import li.cil.oc2.common.vm.terminal.render.TerminalRenderer.GuiSnapshot;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class TerminalGuiSnapshotTest {
    @Test
    void backgroundAndTextReplayDisjointRangesWithTheirOwnVertexAttributes() {
        final ByteBuffer bytes = ByteBuffer.allocate(48).order(ByteOrder.nativeOrder());
        putTexturedVertex(bytes, 1, 2, 0.25f, 0.5f, 0x80402010);
        putTexturedVertex(bytes, 3, 4, 0.75f, 1, 0xFF336699);
        final GuiSnapshot snapshot = new GuiSnapshot(bytes.array(), 2, 1, new byte[0]);
        final VertexConsumer background = mock(VertexConsumer.class, RETURNS_SELF);
        final VertexConsumer text = mock(VertexConsumer.class, RETURNS_SELF);
        final Matrix3x2f pose = new Matrix3x2f().translation(10, 20).scale(2, 3);

        snapshot.replayBackground(background, pose);
        snapshot.replayText(text, pose);

        assertEquals(1, snapshot.backgroundVertexCount());
        assertEquals(1, snapshot.textVertexCount());
        verify(background).addVertex(12f, 26f, 0f);
        verify(background).setColor(16, 32, 64, 128);
        verify(background, never()).setUv(anyFloat(), anyFloat());
        verifyNoMoreInteractions(background);
        verify(text).addVertex(16f, 32f, 0f);
        verify(text).setColor(153, 102, 51, 255);
        verify(text).setUv(0.75f, 1f);
        verifyNoMoreInteractions(text);
    }

    @Test
    void cursorReplayUsesCapturedGeometryAndFullGuiTransform() {
        final ByteBuffer bytes = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
        bytes.putFloat(2).putFloat(3).putFloat(0).putInt(0x80402010);
        final GuiSnapshot snapshot = new GuiSnapshot(new byte[0], 0, 0, bytes.array());
        final VertexConsumer consumer = mock(VertexConsumer.class, RETURNS_SELF);
        final Matrix3x2f pose = new Matrix3x2f().translation(10, 20).rotate((float) (Math.PI / 2)).scale(2, 3);

        snapshot.replayCursor(consumer, pose);

        assertTrue(snapshot.cursorVisible());
        verify(consumer).addVertex(argThat((Matrix4f matrix) ->
                        Math.abs(matrix.m00() - pose.m00()) < 0.0001f
                                && Math.abs(matrix.m01() - pose.m01()) < 0.0001f
                                && Math.abs(matrix.m10() - pose.m10()) < 0.0001f
                                && Math.abs(matrix.m11() - pose.m11()) < 0.0001f
                                && matrix.m30() == 10f && matrix.m31() == 20f),
                eq(2f), eq(3f), eq(0f));
        verify(consumer).setColor(16, 32, 64, 128);
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void worldReplayKeepsBackgroundAndGlyphPassesSeparateAndPreservesDepth() {
        final ByteBuffer bytes = ByteBuffer.allocate(48).order(ByteOrder.nativeOrder());
        bytes.putFloat(1).putFloat(2).putFloat(-0.5f)
                .putFloat(0.25f).putFloat(0.5f).putInt(0x80402010);
        bytes.putFloat(3).putFloat(4).putFloat(-0.25f)
                .putFloat(0.75f).putFloat(1).putInt(0xFF336699);
        final GuiSnapshot snapshot = new GuiSnapshot(bytes.array(), 2, 1, new byte[0]);
        final PoseStack stack = new PoseStack();
        stack.translate(10, 20, 30);
        stack.scale(2, 3, 4);
        final PoseStack.Pose capturedPose = stack.last().copy();
        // Collection and execution are separate phases: resetting the caller's stack cannot
        // move the already submitted block-face geometry.
        stack.setIdentity();
        final VertexConsumer background = mock(VertexConsumer.class, RETURNS_SELF);
        final VertexConsumer text = mock(VertexConsumer.class, RETURNS_SELF);

        snapshot.replayBackground(capturedPose, background);
        snapshot.replayText(capturedPose, text);

        assertEquals(10f, capturedPose.pose().m30());
        assertEquals(20f, capturedPose.pose().m31());
        assertEquals(30f, capturedPose.pose().m32());
        verify(background).addVertex(capturedPose, 1f, 2f, -0.5f);
        verify(background).setColor(16, 32, 64, 128);
        verify(background, never()).setUv(anyFloat(), anyFloat());
        verifyNoMoreInteractions(background);
        verify(text).addVertex(capturedPose, 3f, 4f, -0.25f);
        verify(text).setColor(153, 102, 51, 255);
        verify(text).setUv(0.75f, 1f);
        verifyNoMoreInteractions(text);
    }

    @Test
    void emptySnapshotEmitsNoGeometryIncludingCursor() {
        final GuiSnapshot snapshot = new GuiSnapshot(new byte[0], 0, 0, new byte[0]);
        final VertexConsumer consumer = mock(VertexConsumer.class, RETURNS_SELF);
        final Matrix3x2f pose = new Matrix3x2f();

        snapshot.replayBackground(consumer, pose);
        snapshot.replayText(consumer, pose);
        snapshot.replayCursor(consumer, pose);

        assertFalse(snapshot.cursorVisible());
        assertEquals(0, snapshot.textVertexCount());
        verifyNoInteractions(consumer);
    }

    private static void putTexturedVertex(final ByteBuffer bytes, final float x, final float y,
                                          final float u, final float v, final int abgr) {
        bytes.putFloat(x).putFloat(y).putFloat(0).putFloat(u).putFloat(v).putInt(abgr);
    }
}
*///?}
