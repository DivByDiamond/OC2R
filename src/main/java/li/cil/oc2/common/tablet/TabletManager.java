package li.cil.oc2.common.tablet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import li.cil.oc2.common.container.tablet.AbstractTabletContainer;
import li.cil.oc2.common.container.tablet.TabletInventoryContainer;
import li.cil.oc2.common.container.tablet.TabletTerminalContainer;
import li.cil.oc2.common.item.tablet.TabletItem;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.tablet.TabletTerminalDiffMessage;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs the machines of tablets that are in a hand or have a screen open and suspends the rest.
 * Suspending writes the state to the world folder first, so dropping, dying or logging out loses nothing.
 */
public final class TabletManager {
    private static final Logger LOGGER = LogManager.getLogger();
    // A short grace period keeps scrolling the hotbar from saving and restoring the whole machine.
    static final int SUSPEND_GRACE_TICKS = 40;
    private static final int SAVE_RETRY_TICKS = 200;

    private static final Map<UUID, Entry> MACHINES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<ServerPlayer>> VIEWERS = new ConcurrentHashMap<>();
    @Nullable private static TabletStore store;

    private static final class Entry {
        final TabletMachine machine;
        long lastActive;
        long retryAt;

        Entry(final TabletMachine machine, final long now) {
            this.machine = machine;
            this.lastActive = now;
        }
    }

    private TabletManager() {}

    public static void register() {
        CommonEvents.SERVER_TICK_START.register(TabletManager::tick);
        CommonEvents.SERVER_STOPPING.register(TabletManager::suspendAll);
        CommonEvents.SERVER_STOPPED.register(server -> reset());
    }

    public static void openTerminal(final ServerPlayer player, final UUID id) {
        TabletTerminalContainer.open(player, id);
        sendInitialState(player, id);
    }

    public static void openInventory(final ServerPlayer player, final UUID id) {
        TabletInventoryContainer.open(player, id);
        sendInitialState(player, id);
    }

    /** The machine of a tablet this player currently looks at, never one guessed from a packet. */
    public static Optional<TabletMachine> viewedMachine(final ServerPlayer player, final UUID id) {
        if (player.containerMenu instanceof final AbstractTabletContainer menu
                && menu.tabletId().equals(id)) {
            return Optional.ofNullable(machineOf(player.level().getServer(), id));
        }
        return Optional.empty();
    }

    /** The live machine of a tablet, loading or creating it if it is not running. */
    @Nullable
    public static TabletMachine machineOf(@Nullable final MinecraftServer server, final UUID id) {
        if (server == null) return null;
        final Entry existing = MACHINES.get(id);
        if (existing != null) return existing.machine;
        final TabletMachine machine =
                new TabletMachine(id, server.overworld(), () -> viewersOf(id));
        try {
            final Optional<CompoundTag> saved = storeOf(server).load(id);
            if (saved.isPresent()) {
                machine.load(saved.get());
            } else {
                machine.initializeNew();
            }
        } catch (final IOException | RuntimeException e) {
            // Keep the file as it is; running on top of unreadable data could overwrite it later.
            LOGGER.error("Cannot restore tablet {}; its saved state is left untouched", id, e);
            machine.suspend();
            return null;
        }
        MACHINES.put(id, new Entry(machine, server.getTickCount()));
        return machine;
    }

    public static void addViewer(final ServerPlayer player, final UUID id) {
        VIEWERS.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(player);
    }

    public static void removeViewer(final ServerPlayer player, final UUID id) {
        VIEWERS.computeIfPresent(
                id,
                (k, set) -> {
                    set.remove(player);
                    return set.isEmpty() ? null : set;
                });
    }

    private static Collection<ServerPlayer> viewersOf(final UUID id) {
        return new CopyOnWriteArrayList<>(VIEWERS.getOrDefault(id, Set.of()));
    }

    private static void sendInitialState(final ServerPlayer player, final UUID id) {
        final TabletMachine machine = machineOf(player.level().getServer(), id);
        if (machine == null) return;
        machine.virtualMachine().sendState(player);
        NetworkMessages.sendToClient(
                new TabletTerminalDiffMessage(id, TerminalDiff.captureFull(machine.terminal())), player);
    }

    private static void tick(final MinecraftServer server) {
        tickFor(server, activeIds(server));
    }

    /** One manager tick for the given set of active tablets; tests drive it without real players. */
    public static void tickFor(final MinecraftServer server, final Set<UUID> active) {
        final long now = server.getTickCount();
        for (final UUID id : active) {
            final TabletMachine machine = machineOf(server, id);
            final Entry entry = MACHINES.get(id);
            if (machine == null || entry == null) continue;
            entry.lastActive = now;
            machine.tick();
        }
        for (final Map.Entry<UUID, Entry> e : MACHINES.entrySet()) {
            final Entry entry = e.getValue();
            if (now - entry.lastActive > SUSPEND_GRACE_TICKS && now >= entry.retryAt) {
                suspend(server, e.getKey(), entry, now);
            }
        }
    }

    /** Ids of tablets held in a hand or shown on an open screen. */
    static Set<UUID> activeIds(final MinecraftServer server) {
        final Set<UUID> ids = ConcurrentHashMap.newKeySet();
        for (final ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            for (final ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
                TabletItem.getId(stack).ifPresent(ids::add);
            }
            if (player.containerMenu instanceof final AbstractTabletContainer menu) {
                ids.add(menu.tabletId());
            }
        }
        return ids;
    }

    private static void suspend(
            final MinecraftServer server, final UUID id, final Entry entry, final long now) {
        try {
            storeOf(server).save(id, entry.machine.save());
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Cannot save tablet {}; it keeps running in memory", id, e);
            entry.retryAt = now + SAVE_RETRY_TICKS;
            return;
        }
        MACHINES.remove(id);
        entry.machine.suspend();
    }

    private static void suspendAll(final MinecraftServer server) {
        final long now = server.getTickCount();
        for (final Map.Entry<UUID, Entry> e : MACHINES.entrySet()) {
            suspend(server, e.getKey(), e.getValue(), now);
        }
    }

    /** Drops the machine and its saved state without saving; used by tests to leave no files behind. */
    public static void discard(final MinecraftServer server, final UUID id) throws IOException {
        final Entry entry = MACHINES.remove(id);
        if (entry != null) entry.machine.suspend();
        storeOf(server).delete(id);
    }

    /** Whether a machine for this id is currently live; used by tests. */
    public static boolean isLive(final UUID id) {
        return MACHINES.containsKey(id);
    }

    /** The store of the running world. */
    public static TabletStore storeOf(final MinecraftServer server) {
        TabletStore current = store;
        if (current == null) {
            current = new TabletStore(server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("oc2r_tablets"));
            store = current;
        }
        return current;
    }

    private static void reset() {
        MACHINES.clear();
        VIEWERS.clear();
        store = null;
    }
}
