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
import li.cil.oc2.common.tablet.radio.TabletHolder;
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
@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Each live stack needs its own holder reference.
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
        if (!bindHolder(player, id)) return;
        TabletTerminalContainer.open(player, id);
        sendInitialState(player, id);
    }

    public static void openInventory(final ServerPlayer player, final UUID id) {
        if (!bindHolder(player, id)) return;
        TabletInventoryContainer.open(player, id);
        sendInitialState(player, id);
    }

    private static boolean bindHolder(final ServerPlayer player, final UUID id) {
        final TabletMachine machine = machineOf(player.level().getServer(), id);
        if (machine == null) return false;
        for (final ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            final TabletHolder holder = new TabletHolder(player, stack);
            if (machine.radio().accepts(holder)) {
                machine.radio().update(holder, player.level().getServer().getTickCount());
                return true;
            }
        }
        return false;
    }

    /** Authorizes screens, input and slots using the held item, not just its UUID. */
    public static boolean canUse(final ServerPlayer player, final UUID id) {
        final TabletMachine machine = machineOf(player.level().getServer(), id);
        if (machine == null) return false;
        for (final ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (machine.radio().accepts(new TabletHolder(player, stack))) return true;
        }
        return false;
    }

    /** The machine of a tablet this player currently looks at, never one guessed from a packet. */
    public static Optional<TabletMachine> viewedMachine(final ServerPlayer player, final UUID id) {
        if (player.containerMenu instanceof final AbstractTabletContainer menu
                && menu.tabletId().equals(id) && canUse(player, id)) {
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
        if (canUse(player, id)) VIEWERS.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(player);
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
        // Terminal diffs may be produced by the VM worker; authorization is reconciled on the server thread.
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
        tickHolders(server, activeHolders(server));
    }

    /** One manager tick for the given set of active tablets; tests drive it without real players. */
    public static void tickFor(final MinecraftServer server, final Set<UUID> active) {
        final Map<UUID, TabletHolder> holders = activeHolders(server);
        for (final UUID id : active) holders.putIfAbsent(id, null);
        tickHolders(server, holders);
    }

    /**
     * One manager tick; each active tablet maps to the player holding it, or null when only a screen
     * names it. Only the first holder of an id counts, so a copy of the item gets no machine and no radio.
     */
    public static void tickHolders(final MinecraftServer server, final Map<UUID, TabletHolder> active) {
        final long now = server.getTickCount();
        VIEWERS.forEach((id, players) -> players.removeIf(player -> !canUse(player, id)));
        for (final Map.Entry<UUID, TabletHolder> held : active.entrySet()) {
            final UUID id = held.getKey();
            final TabletMachine machine = machineOf(server, id);
            final Entry entry = MACHINES.get(id);
            if (machine == null || entry == null) continue;
            final TabletHolder holder = held.getValue();
            if (holder != null && !machine.radio().accepts(holder)) {
                if (!machine.radio().holderStillPresent()) machine.radio().update(holder, now);
                continue;
            }
            entry.lastActive = now;
            machine.tick(holder, now);
        }
        for (final Map.Entry<UUID, Entry> e : MACHINES.entrySet()) {
            final Entry entry = e.getValue();
            if (!active.containsKey(e.getKey())) entry.machine.radio().update(null, now);
            if (now - entry.lastActive > SUSPEND_GRACE_TICKS && now >= entry.retryAt) {
                suspend(server, e.getKey(), entry, now);
            }
        }
    }

    /** Tablets held in a hand or shown on an open screen, with their holders. */
    @SuppressWarnings("PMD.UseConcurrentHashMap") // Per-tick server-thread map; null denotes a headless screen.
    static Map<UUID, TabletHolder> activeHolders(final MinecraftServer server) {
        final Map<UUID, TabletHolder> ids = new java.util.LinkedHashMap<>();
        for (final ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            for (final ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
                TabletItem.getId(stack).ifPresent(id -> {
                    final TabletMachine machine = machineOf(server, id);
                    final TabletHolder holder = new TabletHolder(player, stack);
                    if (machine != null && machine.radio().accepts(holder)) ids.putIfAbsent(id, holder);
                });
            }
            if (player.containerMenu instanceof final AbstractTabletContainer menu
                    && canUse(player, menu.tabletId())) {
                ids.putIfAbsent(menu.tabletId(), null);
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
