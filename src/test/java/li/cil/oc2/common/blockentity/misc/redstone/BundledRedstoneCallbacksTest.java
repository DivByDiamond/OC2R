package li.cil.oc2.common.blockentity.misc.redstone;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import li.cil.oc2.common.blockentity.misc.redstone.state.RedstoneInterfaceState;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class BundledRedstoneCallbacksTest {
    @Test
    void aWrittenColourIsReadBackOnTheSameFaceAndSeenByACableThere() {
        final RedstoneInterfaceState state = new RedstoneInterfaceState();
        assertTrue(BundledRedstoneCallbacks.setBundledOutput(Direction.EAST, 200, 5, state));
        assertEquals((byte) 200, BundledRedstoneCallbacks.getBundledOutput(Direction.EAST, state)[5]);
        // A bundled cable asks the block entity by world direction.
        assertEquals((byte) 200, state.getBundledSignal(Direction.EAST)[5]);
        for (final Direction other : Direction.values()) {
            if (other != Direction.EAST) assertArrayEquals(new byte[16], state.getBundledSignal(other));
        }
    }

    @Test
    void writingTheSameValueAgainReportsNoChange() {
        final RedstoneInterfaceState state = new RedstoneInterfaceState();
        assertTrue(BundledRedstoneCallbacks.setBundledOutput(Direction.WEST, 7, 0, state));
        assertFalse(BundledRedstoneCallbacks.setBundledOutput(Direction.WEST, 7, 0, state));
    }

    @Test
    void valuesAndColoursAreClampedToTheirRanges() {
        final RedstoneInterfaceState state = new RedstoneInterfaceState();
        BundledRedstoneCallbacks.setBundledOutput(Direction.NORTH, 999, 99, state);
        assertEquals((byte) 255, state.getBundledSignal(Direction.NORTH)[15]);
        BundledRedstoneCallbacks.setBundledOutput(Direction.NORTH, -4, -1, state);
        assertEquals((byte) 0, state.getBundledSignal(Direction.NORTH)[0]);
    }

    @Test
    void allSixteenColoursOfAFaceAreWrittenAtOnce() {
        final RedstoneInterfaceState state = new RedstoneInterfaceState();
        final int[] values = new int[16];
        for (int i = 0; i < values.length; i++) values[i] = i * 10;
        assertTrue(BundledRedstoneCallbacks.setBundledOutputs(Direction.SOUTH, values, state));
        final byte[] seen = state.getBundledSignal(Direction.SOUTH);
        for (int i = 0; i < values.length; i++) assertEquals((byte) (i * 10), seen[i]);
        assertFalse(BundledRedstoneCallbacks.setBundledOutputs(Direction.SOUTH, values, state));
    }
}
