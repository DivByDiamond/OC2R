package li.cil.oc2.common.inet.internet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.InternetManager;
import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.provider.InternetProvider;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.internet.connection.InternetConnectionImpl;
import li.cil.oc2.common.inet.internet.connection.TaskImpl;
import li.cil.oc2.common.inet.layer.LayerParametersImpl;
import li.cil.oc2.common.inet.util.InetUtils;
import li.cil.oc2.common.inet.util.Ipv4Space;
import net.minecraft.nbt.Tag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central manager handing out internet access to internet card devices.
 *
 * <p>Threading model: the server thread only shuffles ethernet frames between each card's
 * {@link InternetAdapter} and its connection's frame queues (see
 * {@code InternetConnectionImpl}) on every server tick, while the entire TCP/IP stack of every
 * connection — including the session layer, the shared socket selector and tasks scheduled via
 * {@link #runOnInternetThreadTick} — runs on a single dedicated {@code Internet} executor thread.
 */
public final class InternetManagerImpl implements InternetManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static InternetManagerImpl INSTANCE = null;

    private final InternetProvider internetProvider;
    private final List<InternetConnectionImpl> connections = new LinkedList<>();
    private final List<TaskImpl> tasks = new LinkedList<>();

    private final ExecutorService executor;
    private final Ipv4Space ipSpace;

    private InternetManagerImpl() {
        final ServiceLoader<InternetProvider> serviceLoader =
                ServiceLoader.load(InternetProvider.class);
        final Iterator<InternetProvider> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            internetProvider = iterator.next();
        } else {
            internetProvider = DefaultInternetProvider.INSTANCE;
        }
        executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Internet"));
        ipSpace = InetUtils.computeIpSpace(Config.deniedHosts, Config.allowedHosts);
        NeoForge.EVENT_BUS.register(this);
    }

    public static void initialize() {
        if (!Config.internetCardEnabled) {
            LOGGER.info("Internet card is disabled; Internet manager will not start");
        } else {
            if (!Config.enable) {
                LOGGER.warn(
                        "internet card is enabled but VXLAN is disabled"
                                + " — internet will not work");
            }
            INSTANCE = new InternetManagerImpl();
            LOGGER.warn("Internet card is enabled; Players may access to the internal network");
        }
    }

    public static Optional<InternetManagerImpl> getInstance() {
        return Optional.ofNullable(INSTANCE);
    }

    @Override
    public Task runOnInternetThreadTick(final Runnable action) {
        final TaskImpl task = new TaskImpl(action);
        tasks.add(task);
        return task;
    }

    /**
     * Creates a connection for the given adapter, wiring the adapter to a freshly built TCP/IP
     * stack obtained from the {@link InternetProvider}. Called from the server thread when an
     * internet card is mounted; the returned connection must be released via
     * {@code InternetConnection.stop()}.
     */
    public InternetConnection connect(
            final InternetAdapter internetAdapter, @Nullable final Tag savedState) {
        final LayerParameters layerParameters =
                new LayerParametersImpl(Optional.ofNullable(savedState), this);
        final InternetConnectionImpl internetConnection =
                new InternetConnectionImpl(
                        executor,
                        internetAdapter,
                        internetProvider.provideInternet(layerParameters));
        connections.add(internetConnection);
        LOGGER.debug("A new internet access provided");
        return internetConnection;
    }

    /**
     * Moves frames between the VM-side adapter and the connection's frame queues. Runs on the
     * server thread: frames received by the stack (queued on the internet thread in
     * {@code incoming}) are written to the adapter, and frames read from the adapter are queued
     * in {@code outcoming} for the internet thread; excess frames are dropped when full.
     */
    private void processInternetAdapter(final InternetConnectionImpl connection) {
        final InternetAdapter adapter = connection.adapter;
        for (;;) {
            final byte[] received = connection.incoming.poll();
            if (received == null) {
                break;
            }
            adapter.sendEthernetFrame(received);
            // Adapters that retain the frame copy it themselves (borrowed-array contract);
            // once sendEthernetFrame returns, nobody holds a reference and the buffer can
            // be recycled for the next receive cycle (todo.md §38 П7).
            connection.recycleFrame(received);
        }
        for (;;) {
            final byte[] sending = adapter.receiveEthernetFrame();
            if (sending == null) {
                break;
            }
            if (!connection.outcoming.offer(sending)) {
                LOGGER.trace("Outcoming frame queue is full, dropping frame");
                break;
            }
        }
    }

    public boolean isAllowedToConnect(final int ipAddress) {
        return ipSpace.isAllowed(ipAddress);
    }

    private void runTasks() {
        tasks.removeIf(
                task -> {
                    if (task.isClosed()) {
                        return true;
                    } else {
                        final Runnable action = task.getAction();
                        try {
                            action.run();
                            return false;
                        } catch (final Exception exception) {
                            LOGGER.error(
                                    "Uncaught exception while running internet thread task; this"
                                            + " task removed from schedule",
                                    exception);
                            return true;
                        }
                    }
                });
    }

    private void runOnInternetThread(
            final List<InternetConnectionImpl> connectionsToStop,
            final List<InternetConnectionImpl> connectionsToProcess) {
        runTasks();
        connectionsToStop.forEach(
                connection -> {
                    LOGGER.debug("Revoked internet access");
                    connection.ethernet.onStop();
                });
        connectionsToProcess.forEach(InternetConnectionImpl::process);
    }

    /**
     * Per-tick driver, runs on the server thread. Connections flagged as stopped are torn down
     * ({@code ethernet.onStop()}) and removed; all others first exchange frames with their
     * adapters here, then have their TCP/IP stacks processed by the internet thread.
     *
     * <p>When there is nothing to do — no connections and no scheduled tasks — the handoff to
     * the internet thread is skipped entirely: a pointless cross-thread round trip with fresh
     * list allocations every tick would otherwise run even on servers without any internet card.
     */
    @SubscribeEvent
    public void onTick(final ServerTickEvent.Pre event) {
        if (connections.isEmpty() && tasks.isEmpty()) {
            return;
        }

        final List<InternetConnectionImpl> connectionsToStop = new ArrayList<>();
        for (final InternetConnectionImpl connection : connections) {
            if (connection.isStopped) {
                connectionsToStop.add(connection);
            }
        }
        connections.removeIf(connection -> connection.isStopped);

        final List<InternetConnectionImpl> connectionsToProcess = new ArrayList<>();
        for (final InternetConnectionImpl connection : connections) {
            processInternetAdapter(connection);
            connectionsToProcess.add(connection);
        }

        executor.execute(() -> runOnInternetThread(connectionsToStop, connectionsToProcess));
    }

    @SubscribeEvent
    public void onStopping(final ServerStoppingEvent event) {
        connections.clear();
    }
}