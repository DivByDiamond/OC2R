package li.cil.oc2.common.inet.internet;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
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

    private void processInternetAdapter(final InternetConnectionImpl connection) {
        final InternetAdapter adapter = connection.adapter;
        byte[] received;
        while ((received = connection.incoming.poll()) != null) {
            adapter.sendEthernetFrame(received);
        }
        byte[] sending;
        while ((sending = adapter.receiveEthernetFrame()) != null) {
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

    @SubscribeEvent
    public void onTick(final ServerTickEvent.Pre event) {
        final List<InternetConnectionImpl> connectionsToStop =
                connections.stream()
                        .filter(connection -> connection.isStopped)
                        .collect(Collectors.toList());
        final List<InternetConnectionImpl> connectionsToProcess =
                connections.stream()
                        .filter(connection -> !connection.isStopped)
                        .collect(Collectors.toList());
        connections.removeIf(
                connection -> {
                    if (connection.isStopped) {
                        return true;
                    } else {
                        processInternetAdapter(connection);
                        return false;
                    }
                });
        executor.execute(() -> runOnInternetThread(connectionsToStop, connectionsToProcess));
    }

    @SubscribeEvent
    public void onStopping(final ServerStoppingEvent event) {
        connections.clear();
    }
}