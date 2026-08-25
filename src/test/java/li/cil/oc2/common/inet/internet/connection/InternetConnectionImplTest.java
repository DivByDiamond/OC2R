package li.cil.oc2.common.inet.internet.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import li.cil.oc2.common.inet.internet.InternetAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class InternetConnectionImplTest {
    private ExecutorService executor;
    private InternetAdapter adapter;
    private LinkLocalLayer ethernet;
    private InternetConnectionImpl connection;

    @BeforeEach
    void setUp() {
        executor = mock(ExecutorService.class);
        adapter = mock(InternetAdapter.class);
        ethernet = mock(LinkLocalLayer.class);
        connection = new InternetConnectionImpl(executor, adapter, ethernet);
    }

    private static byte[] frame(final int value) {
        return new byte[] {(byte) value};
    }

    @Test
    void processDrainsAllOutcomingFramesWithoutLoss() {
        final byte[] first = frame(1);
        final byte[] second = frame(2);
        final byte[] third = frame(3);
        assertTrue(connection.outcoming.offer(first));
        assertTrue(connection.outcoming.offer(second));
        assertTrue(connection.outcoming.offer(third));

        connection.process();

        final ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(ethernet, times(3)).sendEthernetFrame(captor.capture());
        assertEquals(first, captor.getAllValues().get(0).array());
        assertEquals(second, captor.getAllValues().get(1).array());
        assertEquals(third, captor.getAllValues().get(2).array());
        assertNull(connection.outcoming.poll());
    }

    private static boolean fillFrame(final ByteBuffer buffer, final int value) {
        buffer.put(frame(value));
        buffer.flip();
        return true;
    }

    @Test
    void processCollectsAllIncomingFramesWithoutLoss() {
        when(ethernet.receiveEthernetFrame(any()))
                .thenAnswer(invocation -> fillFrame(invocation.getArgument(0), 1))
                .thenAnswer(invocation -> fillFrame(invocation.getArgument(0), 2))
                .thenReturn(false);

        connection.process();

        assertEquals(2, connection.incoming.size());
        final byte[] firstFrame = connection.incoming.poll();
        final byte[] secondFrame = connection.incoming.poll();
        assertEquals(1, firstFrame[0]);
        assertEquals(2, secondFrame[0]);
    }

    @Test
    void processStopsReadingWhenQueueFull() {
        when(ethernet.receiveEthernetFrame(any()))
                .thenAnswer(invocation -> fillFrame(invocation.getArgument(0), 1));

        connection.process();

        assertEquals(
                InternetConnectionImpl.FRAME_QUEUE_CAPACITY, connection.incoming.size());
        verify(ethernet, times(InternetConnectionImpl.FRAME_QUEUE_CAPACITY))
                .receiveEthernetFrame(any());
    }
}
