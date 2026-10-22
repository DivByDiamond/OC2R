package li.cil.oc2.common.firmware;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import li.cil.oc2.common.blockentity.misc.flash.FlashMemoryFlasherBlockEntity;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.storage.AbstractStorageItem;
import li.cil.oc2.common.item.storage.HardDriveItem;
import li.cil.oc2.common.item.storage.flash.FlashMemoryItem;
import li.cil.oc2.common.item.storage.flash.FlashMemoryWithExternalDataItem;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.firmware.FlashWriterActionMessage;
import li.cil.oc2.common.network.message.firmware.FlashWriterChunkMessage;
import li.cil.oc2.common.network.message.firmware.FlashWriterStatusMessage;
import li.cil.oc2.common.network.util.PlayerRateLimits;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Server-authoritative, stop-and-wait transfer into a new world-local storage blob. */
public final class FlashWriterService {
    private static final int MAX_SESSIONS = 16;
    private static final int MAX_STORAGE_CAPACITY = 1024 * 1024 * 1024;
    private static final long TIMEOUT = Duration.ofMinutes(2).toMillis();
    // Monitor for the one-time event registration below. It is a private object instead of the
    // class monitor, so that no other class can lock the class itself (SEI CERT LCK00-J), and it
    // guards exactly the state it protects: the `initialized` flag.
    private static final Object INIT_LOCK = new Object();
    private static final SessionRegistry SESSIONS = new SessionRegistry();
    private static final ExecutorService IO = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(64), task -> {
        final Thread thread = new Thread(task, "oc2r-image-upload");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());
    private static boolean initialized;

    private FlashWriterService() {}

    /**
     * Registers the session lifecycle events exactly once.
     *
     * <p>The monitor is a private object, so that no other class can lock this class itself
     * (SEI CERT LCK00-J), and it guards exactly the state it protects: the {@code initialized}
     * flag and the event registration. The session registry is not guarded by it; that registry
     * is confined to the server thread by {@link SessionRegistry}.
     */
    @SuppressWarnings("PMD.AvoidSynchronizedStatement") // one-time setup lock, not a hot path
    public static void initialize() {
        synchronized (INIT_LOCK) {
            if (initialized) return;
            initialized = true;
            CommonEvents.SERVER_TICK_START.register(server -> {
                for (final Session session : SESSIONS.values()) {
                    // Instance identity is required for ownership, canonical states and recursion guards.
                    if (session.player.level().getServer() == server && !valid(session)) { // NOPMD
                        if (session.player.hasDisconnected()) discard(session);
                        else fail(session, "expired");
                    }
                }
            });
            CommonEvents.SERVER_STOPPING.register(server -> {
                for (final Session session : SESSIONS.values()) {
                    // Instance identity is required for ownership, canonical states and recursion guards.
                    if (session.player.level().getServer() == server) discard(session); // NOPMD
                }
            });
        }
    }

    @javax.annotation.Nullable
    public static UUID open(final ServerPlayer player, final FlashMemoryFlasherBlockEntity owner) {
        if (!PlayerRateLimits.allowThrottled(player, owner, 500) || !accessible(player, owner)) return null;
        final Session old = SESSIONS.get(player.getUUID());
        if (old != null) discard(old);
        if (SESSIONS.size() >= MAX_SESSIONS) return null;
        final Session session = new Session(player, owner);
        SESSIONS.put(player.getUUID(), session);
        final ItemStack stack = session.stack;
        final String kind = stack.getItem() instanceof HardDriveItem ? "disk"
                : stack.getItem() instanceof FlashMemoryItem
                        || stack.getItem() instanceof FlashMemoryWithExternalDataItem
                ? "flash" : "empty";
        reply(session, "open", kind);
        return session.token;
    }

    public static void action(final ServerPlayer player, final FlashWriterActionMessage message) {
        final Session session = find(player, message.pos(), message.session());
        if (session == null) return;
        if ("cancel".equals(message.action())) {
            discard(session);
            return;
        }
        if (!valid(session)) {
            fail(session, "invalid_target");
            return;
        }
        session.lastAccess = System.currentTimeMillis();
        if (session.busy) {
            fail(session, "invalid_chunk");
            return;
        }
        switch (message.action()) {
            case "minux", "onyxos" -> builtin(session, message.action());
            case "begin" -> begin(session, message);
            case "finish" -> finish(session);
            default -> fail(session, "invalid_image");
        }
    }

    public static void chunk(final ServerPlayer player, final FlashWriterChunkMessage message) {
        final Session session = find(player, message.pos(), message.session());
        if (session == null) return;
        if (!valid(session) || !session.leased || session.upload == null || session.busy) {
            fail(session, "invalid_target");
            return;
        }
        session.lastAccess = System.currentTimeMillis();
        final byte[] bytes = message.data();
        // Flash executes a flat image at RAM base. An ELF container/zip archive is not firmware.
        if (message.offset() == 0 && session.stack.getItem() instanceof FlashMemoryItem
                && bytes.length >= 4 && ((bytes[0] == 0x7F && bytes[1] == 'E' && bytes[2] == 'L' && bytes[3] == 'F')
                || (bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4))) {
            fail(session, "flat_image_required");
            return;
        }
        runIo(session, () -> session.upload.append(message.offset(), bytes),
                () -> reply(session, "chunk", ""));
    }

    private static void begin(final Session session, final FlashWriterActionMessage message) {
        final String name = message.name();
        if (session.upload != null || session.leased || session.capacity <= 0
                || message.length() <= 0 || message.length() > session.capacity
                || message.length() > FlashImageUpload.MAX_IMAGE_SIZE
                || !message.digest().matches("[0-9a-f]{64}")
                || name.isBlank() || name.length() > 255 || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.chars().anyMatch(Character::isISOControl)
                || name.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(elf|iso|zip)$")) {
            fail(session, "invalid_image");
            return;
        }
        if (!session.owner.beginWriting()) {
            fail(session, "busy");
            return;
        }
        session.leased = true;
        session.blob = BlobStorage.validateHandle(null);
        runIo(session, () -> {
            session.channel = BlobStorage.getOrOpenAsync(session.blob).join();
            session.channel.truncate(0);
            session.channel.position(0);
            session.upload = new FlashImageUpload(message.length(), session.capacity,
                    message.digest(), Channels.newOutputStream(session.channel));
        }, () -> reply(session, "ready", name));
    }

    private static void builtin(final Session session, final String firmware) {
        if (session.upload != null || !(session.stack.getItem() instanceof FlashMemoryItem
                || session.stack.getItem() instanceof FlashMemoryWithExternalDataItem)) {
            fail(session, "insert_flash");
            return;
        }
        if (!session.owner.beginWriting()) {
            fail(session, "busy");
            return;
        }
        session.leased = true;
        final var selected = FirmwareRegistry.getValue("onyxos".equals(firmware)
                ? FirmwareRegistry.ONYXOS_ID : FirmwareRegistry.MINUX_ID);
        if (selected == null) {
            fail(session, "invalid_image");
            return;
        }
        session.owner.replaceWrittenItem(Items.FLASH_MEMORY_CUSTOM.get().withFirmware(selected));
        reply(session, "done", firmware);
        discard(session);
    }

    private static void finish(final Session session) {
        if (session.upload == null || !session.leased) {
            fail(session, "incomplete_image");
            return;
        }
        runIo(session, () -> {
            session.upload.finish();
            final ByteBuffer header = ByteBuffer.allocate(4);
            session.channel.read(header, 0);
            final byte[] prefix = header.array();
            if ((prefix[0] == 0x7F && prefix[1] == 'E' && prefix[2] == 'L' && prefix[3] == 'F')
                    || (prefix[0] == 'P' && prefix[1] == 'K' && prefix[2] == 3 && prefix[3] == 4)) {
                throw new IllegalArgumentException("flat_image_required");
            }
            // Pad to the item's capacity without allocating the whole disk or putting it in NBT.
            // The padding is bounded by the image limit, so a session cannot reserve the whole
            // (up to 1 GiB) capacity of a large medium on the server.
            final int padded = Math.min(session.capacity, FlashImageUpload.MAX_IMAGE_SIZE);
            if (session.channel.size() < padded) {
                session.channel.position(padded - 1L);
                session.channel.write(ByteBuffer.wrap(new byte[1]));
            }
            session.channel.force(true);
            BlobStorage.closeAsync(session.blob).join();
            session.channel = null;
        }, () -> {
            final ItemStack written = session.stack.copy();
            final UUID blob = session.blob;
            CustomData.update(DataComponents.CUSTOM_DATA, written, nbt -> {
                final CompoundTag data = new CompoundTag();
                //? if >=26.1 {
                /*data.store("blob", net.minecraft.core.UUIDUtil.CODEC, blob);
                *///?} else {
                data.putUUID("blob", blob);
                //?}
                ItemStackUtils.getOrCreateModDataTag(nbt).put("data", data);
            });
            session.owner.replaceWrittenItem(written);
            session.blob = null; // Ownership passed to the item; cancellation must not delete it.
            reply(session, "done", "local");
            discard(session);
        });
    }

    private static void runIo(final Session session, final IoAction action, final Runnable completed) {
        session.busy = true;
        final MinecraftServer server = session.player.level().getServer();
        try {
            session.job = CompletableFuture.runAsync(() -> {
                try {
                    action.run();
                } catch (final Exception error) {
                    throw new java.util.concurrent.CompletionException(error);
                }
            }, IO);
        } catch (final RejectedExecutionException error) {
            fail(session, "busy");
            return;
        }
        session.job.whenComplete((unused, error) -> server.execute(() -> {
            session.busy = false;
            if (session.closed) return;
            if (error != null) {
                final Throwable cause = error.getCause() == null ? error : error.getCause();
                fail(session, cause instanceof IllegalArgumentException && cause.getMessage() != null
                        ? cause.getMessage() : "io_error");
            } else if (!valid(session)) {
                fail(session, "invalid_target");
            } else {
                completed.run();
            }
        }));
    }

    private static Session find(final ServerPlayer player, final BlockPos position, final UUID token) {
        final Session session = SESSIONS.get(player.getUUID());
        // Instance identity is required for ownership, canonical states and recursion guards.
        return session != null && session.player == player && session.token.equals(token) // NOPMD
                && session.owner.getBlockPos().equals(position) ? session : null;
    }

    private static boolean accessible(final ServerPlayer player, final FlashMemoryFlasherBlockEntity owner) {
        // Instance identity is required for ownership, canonical states and recursion guards.
        if (player.isSpectator() || !player.mayBuild() || owner.getLevel() != player.level() // NOPMD
                || !owner.getBlockPos().closerToCenterThan(player.position(), 8)) return false;
        final var level = player.level();
        return level.hasChunkAt(owner.getBlockPos()) && level.mayInteract(player, owner.getBlockPos())
                // Instance identity is required for ownership, canonical states and recursion guards.
                && !owner.isRemoved() && level.getBlockEntity(owner.getBlockPos()) == owner; // NOPMD
    }

    private static boolean valid(final Session session) {
        return !session.closed && !session.player.hasDisconnected()
                && System.currentTimeMillis() - session.lastAccess < TIMEOUT
                && accessible(session.player, session.owner)
                // Instance identity is required for ownership, canonical states and recursion guards.
                && session.owner.getDiskItemStack() == session.stack // NOPMD
                && ItemStack.isSameItemSameComponents(session.stack, session.snapshot)
                && session.stack.getCount() == session.snapshot.getCount();
    }

    private static void reply(final Session session, final String state, final String detail) {
        NetworkMessages.sendToClient(new FlashWriterStatusMessage(session.owner.getBlockPos(), session.token,
                state, session.upload == null ? 0 : session.upload.offset(), session.capacity, detail), session.player);
    }

    private static void fail(final Session session, final String reason) {
        reply(session, "error", reason);
        discard(session);
    }

    private static void discard(final Session session) {
        if (session.closed) return;
        session.closed = true;
        SESSIONS.remove(session.player.getUUID(), session);
        if (session.leased) session.owner.endWriting();
        // Wait for an in-flight write before closing/deleting its unpublished blob.
        session.job.whenComplete((unused, error) -> {
            final UUID blob = session.blob;
            if (blob == null) return;
            // The blob must be closed before it is deleted, because an open file cannot be
            // deleted on Windows. Closing an already closed channel is a no-op, so a blob that
            // was closed by the transfer itself is not closed twice.
            final FileChannel channel = session.channel;
            session.channel = null;
            if (channel != null) {
                try {
                    channel.close();
                } catch (final IOException ignored) {
                    // The blob is deleted below, so a failed close must not stop the cleanup.
                }
            }
            BlobStorage.deleteAsync(blob);
        });
    }

    /**
     * Registry of the open flash-writer sessions.
     *
     * <p>The registry is confined to the server thread: it is only reached from the server's
     * network handlers and from the server tick and stop events. Owning the map in a dedicated
     * type states that confinement as part of the registry's contract, instead of leaving it to
     * a comment on a static field, and it keeps the session state out of the class monitor that
     * the one-time initialization uses.
     */
    private static final class SessionRegistry {
        // Server thread only; the registry owns the map and confines it to that thread.
        // Map ownership is confined to one thread or protected by its explicit monitor.
        private final Map<UUID, Session> sessions = new HashMap<>(); // NOPMD

        Session get(final UUID player) {
            return sessions.get(player);
        }

        void put(final UUID player, final Session session) {
            sessions.put(player, session);
        }

        void remove(final UUID player, final Session session) {
            sessions.remove(player, session);
        }

        int size() {
            return sessions.size();
        }

        /** Snapshot of the open sessions, so that the callers can mutate the registry. */
        List<Session> values() {
            return new ArrayList<>(sessions.values());
        }
    }

    private static final class Session {
        final ServerPlayer player;
        final FlashMemoryFlasherBlockEntity owner;
        final ItemStack stack;
        final ItemStack snapshot;
        final UUID token = UUID.randomUUID();
        final int capacity;
        long lastAccess = System.currentTimeMillis();
        volatile boolean closed;
        boolean busy;
        boolean leased;
        // Shared between the server thread and the upload pool, so they must be published safely.
        volatile UUID blob;
        volatile FileChannel channel;
        volatile FlashImageUpload upload;
        CompletableFuture<Void> job = CompletableFuture.completedFuture(null);

        Session(final ServerPlayer player, final FlashMemoryFlasherBlockEntity owner) {
            this.player = player;
            this.owner = owner;
            owner.getFloppy(); // Flush the current backing handle before snapshotting item components.
            stack = owner.getDiskItemStack();
            snapshot = stack.copy();
            final int size = stack.getItem() instanceof AbstractStorageItem storage
                    && (storage instanceof FlashMemoryItem || storage instanceof HardDriveItem)
                    ? storage.getCapacity(stack) : 0;
            capacity = size > 0 && size <= MAX_STORAGE_CAPACITY ? size : 0;
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
