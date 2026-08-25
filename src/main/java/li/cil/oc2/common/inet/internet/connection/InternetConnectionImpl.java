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

public final class InternetConnectionImpl implements InternetConnection {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Number of ethernet frames buffered per direction between server and internet threads. */
    public static final int FRAME_QUEUE_CAPACITY = 64;

    public final ArrayBlockingQueue<byte[]> incoming =
            new ArrayBlockingQueue<>(FRAME_QUEUE_CAPACITY);
    public final ArrayBlockingQueue<byte[]> outcoming =
            new ArrayBlockingQueue<>(FRAME_QUEUE_CAPACITY);

    private final ExecutorService executor;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_SIZE);
    public final LinkLocalLayer ethernet;
    public final InternetAdapter adapter;
    public boolean isStopped = false;

    public InternetConnectionImpl(
            final ExecutorService executor,
            final InternetAdapter adapter,
            final LinkLocalLayer ethernet) {
        this.executor = executor;
        this.adapter = adapter;
        this.ethernet = ethernet;
    }

    @Override
    public Optional<Tag> saveAdapterState() {
        try {
            return executor.submit(ethernet::onSave).get();
        } catch (final InterruptedException | ExecutionException exception) {
            LOGGER.error("Error on saving internet adapter state", exception);
            return Optional.empty();
        }
    }

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

    @Override
    public void stop() {
        isStopped = true;
    }
}