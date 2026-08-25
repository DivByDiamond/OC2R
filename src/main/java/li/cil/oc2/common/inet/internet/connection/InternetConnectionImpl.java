package li.cil.oc2.common.inet.internet.connection;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import li.cil.oc2.common.inet.internet.InternetAdapter;
import li.cil.oc2.common.inet.internet.InternetConnection;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bidirectional frame pump between the server thread and the dedicated {@code Internet} thread.
 *
 * <p>The TCP/IP stack provided by the {@code InternetProvider} (and everything built on top of it,
 * such as the session layer) is only ever touched from the internet thread inside
 * {@link #process()}, while the server thread moves frames between these queues and the card's
 * {@link InternetAdapter}. The bounded queues decouple the two threads and apply backpressure by
 * dropping frames instead of blocking either side.
 */
public final class InternetConnectionImpl implements InternetConnection {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Number of ethernet frames buffered per direction between server and internet threads. */
    public static final int FRAME_QUEUE_CAPACITY = 64;

    /**
     * Frames received from the real network, waiting to be handed to the VM's network interface.
     *
     * <p>Produced by the internet thread in {@link #process()} (reading from
     * {@code ethernet}), consumed by the server thread in {@code processInternetAdapter()}.
     */
    public final ArrayBlockingQueue<byte[]> incoming =
            new ArrayBlockingQueue<>(FRAME_QUEUE_CAPACITY);

    /**
     * Frames captured from the VM's network interface, waiting to enter the TCP/IP stack.
     *
     * <p>Produced by the server thread in {@code processInternetAdapter()}, consumed by the
     * internet thread in {@link #process()} (feeding {@code ethernet}).
     */
    public final ArrayBlockingQueue<byte[]> outcoming =
            new ArrayBlockingQueue<>(FRAME_QUEUE_CAPACITY);

    private final ExecutorService executor;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_SIZE);
    public final LinkLocalLayer ethernet;
    public final InternetAdapter adapter;

    /**
     * Stop flag. Set from the server thread via {@link #stop()}; read from both threads.
     * Once set, the manager removes this connection on its next tick and {@link #process()}
     * stops refilling {@link #incoming}.
     */
    public boolean isStopped = false;

    public InternetConnectionImpl(
            final ExecutorService executor,
            final InternetAdapter adapter,
            final LinkLocalLayer ethernet) {
        this.executor = executor;
        this.adapter = adapter;
        this.ethernet = ethernet;
    }

    /**
     * Saves the adapter state by running {@code ethernet.onSave()} on the internet thread and
     * blocking the caller (server thread) until it completes, since the stack state must not be
     * mutated concurrently while it is serialized.
     */
    @Override
    public Optional<Tag> saveAdapterState() {
        try {
            return executor.submit(ethernet::onSave).get();
        } catch (final InterruptedException | ExecutionException exception) {
            LOGGER.error("Error on saving internet adapter state", exception);
            return Optional.empty();
        }
    }

    /**
     * Drains queued frames in both directions. Runs on the internet thread once per manager tick:
     * every frame buffered in {@link #outcoming} (originating from the VM) is pushed into the
     * TCP/IP stack, then {@link #incoming} is refilled from the stack until it is full, the stack
     * has no more frames, or {@link #isStopped} was set.
     */
    public void process() {
        try {
            byte[] outFrame;
            while ((outFrame = outcoming.poll()) != null) {
                ethernet.sendEthernetFrame(ByteBuffer.wrap(outFrame));
            }
            while (!isStopped && incoming.remainingCapacity() > 0) {
                receiveBuffer.clear();
                if (!ethernet.receiveEthernetFrame(receiveBuffer)) {
                    break;
                }
                final byte[] inFrame = new byte[receiveBuffer.remaining()];
                receiveBuffer.get(inFrame);
                incoming.add(inFrame);
            }
        } catch (final Exception e) {
            LOGGER.error("Uncaught exception", e);
        }
    }

    /**
     * Flags this connection as stopped from the server thread. The actual teardown
     * ({@code ethernet.onStop()}) is performed by the manager on the internet thread on the
     * next tick; this connection is removed from the managed list at the same time.
     */
    @Override
    public void stop() {
        isStopped = true;
    }
}