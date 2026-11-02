package li.cil.oc2.common.vm.terminal.render;

//? if >=26.1 {
/*import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.render.overlay.TerminalCursorRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DoubleSizeCursorTest {
    @ParameterizedTest
    @ValueSource(strings = {"3", "4", "5", "6", "alt3", "alt4", "alt5", "alt6"})
    void steadyBlockCursorFitsLastVisibleCellAndItsOwnRow(final String mode) {
        final Terminal terminal = new Terminal();
        final boolean alt = mode.startsWith("alt");
        final char size = mode.charAt(mode.length() - 1);
        final String sequence = (alt ? "\033[?1049h" : "")
                + "\033[3;1H\033#" + size + "\033[3;999H\033[2 q";
        terminal.io.putOutput(ByteBuffer.wrap(sequence.getBytes(StandardCharsets.UTF_8)));
        final List<Vector3f> vertices = new ArrayList<>();
        final VertexConsumer consumer = mock(VertexConsumer.class, RETURNS_SELF);
        doAnswer(call -> {
            final Matrix4f matrix = call.getArgument(0);
            vertices.add(matrix.transformPosition(new Vector3f(
                    call.getArgument(1), call.getArgument(2), call.getArgument(3))));
            return consumer;
        }).when(consumer).addVertex(any(Matrix4f.class), anyFloat(), anyFloat(), anyFloat());

        TerminalCursorRenderer.buildCursor(terminal, consumer, new Matrix4f());

        final int scale = size == '5' ? 1 : 2;
        final float right = terminal.width * Terminal.CHAR_WIDTH;
        final float left = right - scale * Terminal.CHAR_WIDTH;
        final float top = 2 * Terminal.CHAR_HEIGHT;
        assertEquals(List.of(new Vector3f(left, top + Terminal.CHAR_HEIGHT, 0),
                new Vector3f(right, top + Terminal.CHAR_HEIGHT, 0),
                new Vector3f(right, top, 0), new Vector3f(left, top, 0)), vertices);
    }
}
*///?}
