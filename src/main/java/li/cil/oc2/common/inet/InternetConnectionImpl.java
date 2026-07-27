package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.layer.LinkLocalLayer;

import net.minecraft.nbt.Tag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

final class InternetConnectionImpl implements InternetConnection {
    private static final Logger LOGGER = LogManager.getLogger();

    public final PendingFrame incoming = new PendingFrame();
    public final PendingFrame outcoming = new PendingFrame();

    private final ExecutorService executor;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_SIZE);
    final LinkLocalLayer ethernet;
    final InternetAdapter adapter;
    boolean isStopped = false;

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
            final byte[] outFrame = outcoming.get();
            if (outFrame != null) {
                ethernet.sendEthernetFrame(ByteBuffer.wrap(outFrame));
            }
            receiveBuffer.clear();
            if (ethernet.receiveEthernetFrame(receiveBuffer)) {
                final byte[] inFrame = new byte[receiveBuffer.remaining()];
                receiveBuffer.get(inFrame);
                incoming.put(inFrame);
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
