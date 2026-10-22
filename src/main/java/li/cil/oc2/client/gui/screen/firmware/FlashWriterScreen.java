package li.cil.oc2.client.gui.screen.firmware;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import li.cil.oc2.client.RenderCompat;
import li.cil.oc2.client.gui.screen.file.FileChooserCallback;
import li.cil.oc2.client.gui.screen.file.FileChooserScreen;
import li.cil.oc2.common.firmware.FlashImageUpload;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.firmware.FlashWriterActionMessage;
import li.cil.oc2.common.network.message.firmware.FlashWriterChunkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** A local chooser plus server-confirmed writer; paths and image bytes never enter item NBT. */
public final class FlashWriterScreen extends Screen {
    private static FlashWriterScreen active;
    private final BlockPos position;
    private final UUID session;
    private final int capacity;
    private final String kind;
    private StringWidget fileLabel;
    private StringWidget statusLabel;
    private Button chooseButton;
    private Button writeButton;
    private Button minuxButton;
    private Button onyxButton;
    private Path selected;
    private String fileName = "";
    private String digest = "";
    private int length;
    private int offset;
    private int sent;
    private volatile InputStream input;
    private volatile boolean closed;
    private volatile boolean busy;
    private volatile boolean completed;
    private volatile long lastReply = System.currentTimeMillis();
    private Component status = text("choose");

    private FlashWriterScreen(final BlockPos position, final UUID session, final int capacity, final String kind) {
        super(text("title"));
        this.position = position;
        this.session = session;
        this.capacity = capacity;
        this.kind = kind;
    }

    public static void receive(final BlockPos position, final UUID session, final String state,
                               final int offset, final int capacity, final String detail) {
        if ("open".equals(state)) {
            if (active != null) active.onClose();
            active = new FlashWriterScreen(position, session, capacity, detail);
            RenderCompat.setScreen(active);
        } else if (active != null && active.session.equals(session) && active.position.equals(position)) {
            active.reply(state, offset, detail);
        }
    }

    @Override
    protected void init() {
        final int left = (width - 300) / 2;
        final int top = Math.max(8, (height - 232) / 2);
        addRenderableWidget(new StringWidget(left, top, 300, 16, title, font));
        addRenderableWidget(new StringWidget(left, top + 21, 300, 12, text("inserted_" + kind), font));
        addRenderableWidget(new StringWidget(left, top + 37, 300, 12,
                Component.translatable("gui.oc2r.flash_writer.capacity", Math.min(capacity, FlashImageUpload.MAX_IMAGE_SIZE) / 1024), font));
        addRenderableWidget(new StringWidget(left, top + 53, 300, 12, text("format_" + kind), font));
        addRenderableWidget(new StringWidget(left, top + 69, 300, 12, text("builtin_hint"), font));
        fileLabel = addRenderableWidget(new StringWidget(left, top + 85, 300, 12, text("no_file"), font));
        statusLabel = addRenderableWidget(new StringWidget(left, top + 103, 300, 20, status, font));
        chooseButton = addRenderableWidget(Button.builder(text("choose_file"), button -> choose())
                .bounds(left, top + 133, 146, 20).build());
        writeButton = addRenderableWidget(Button.builder(text("write_file"), button -> write())
                .bounds(left + 154, top + 133, 146, 20).build());
        minuxButton = addRenderableWidget(Button.builder(text("minux"), button -> builtin("minux"))
                .bounds(left, top + 163, 146, 20).build());
        onyxButton = addRenderableWidget(Button.builder(text("onyxos"), button -> builtin("onyxos"))
                .bounds(left + 154, top + 163, 146, 20).build());
        addRenderableWidget(new StringWidget(left, top + 191, 300, 12, text("replaces"), font));
        addRenderableWidget(Button.builder(text("close"), button -> onClose())
                .bounds(left + 75, top + 209, 150, 20).build());
        updateWidgets();
    }

    private static Component text(final String key) {
        return Component.translatable("gui.oc2r.flash_writer." + key);
    }

    private void updateWidgets() {
        if (fileLabel == null) return;
        fileLabel.setMessage(fileName.isEmpty() ? text("no_file")
                : Component.literal(font.plainSubstrByWidth(fileName, 290)));
        statusLabel.setMessage(status);
        chooseButton.active = !busy && !completed && capacity > 0;
        writeButton.active = !busy && !completed && selected != null;
        minuxButton.active = !busy && !completed && "flash".equals(kind);
        onyxButton.active = minuxButton.active;
    }

    private void choose() {
        FileChooserScreen.openFileChooserForLoad(new FileChooserCallback() {
            @Override
            public void onFileSelected(final Path path) {
                inspect(path);
            }

            @Override
            public void onCanceled() {}
        });
    }

    private void inspect(final Path path) {
        if (closed || completed) return;
        lastReply = System.currentTimeMillis();
        busy = true;
        selected = null;
        status = text("checking");
        updateWidgets();
        CompletableFuture.runAsync(() -> {
            try {
                final ImageInfo image = inspectImage(path, capacity);
                Minecraft.getInstance().execute(() -> {
                    if (closed) return;
                    selected = path;
                    length = image.length();
                    // The file name of a root path is null, so it must not be dereferenced blindly.
                    final Path name = path.getFileName();
                    fileName = name == null ? path.toString() : name.toString();
                    digest = image.checksum();
                    busy = false;
                    status = text("ready_to_write");
                    updateWidgets();
                });
            } catch (final Exception error) {
                Minecraft.getInstance().execute(() -> localError("invalid_image"));
            }
        });
    }

    private record ImageInfo(int length, String checksum) {}

    private static ImageInfo inspectImage(final Path path, final int capacity)
            throws IOException, java.security.NoSuchAlgorithmException {
        final long size = Files.size(path);
        if (!Files.isRegularFile(path) || size <= 0 || size > capacity || size > FlashImageUpload.MAX_IMAGE_SIZE) {
            throw new IOException("invalid_image");
        }
        final MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(path)) {
            final byte[] bytes = new byte[32 * 1024];
            long count = 0;
            for (int read = stream.read(bytes); read != -1; read = stream.read(bytes)) {
                count += read;
                if (count > size) throw new IOException("invalid_image");
                hash.update(bytes, 0, read);
            }
            if (count != size) throw new IOException("invalid_image");
        }
        return new ImageInfo((int) size, HexFormat.of().formatHex(hash.digest()));
    }

    private void builtin(final String firmware) {
        busy = true;
        lastReply = System.currentTimeMillis();
        status = text("writing");
        updateWidgets();
        send(firmware);
    }

    private void write() {
        busy = true;
        lastReply = System.currentTimeMillis();
        status = text("writing");
        updateWidgets();
        send("begin");
    }

    private void send(final String action) {
        NetworkMessages.sendToServer(new FlashWriterActionMessage(position, session, action, length, fileName, digest));
    }

    private void reply(final String state, final int confirmedOffset, final String detail) {
        if (closed) return;
        lastReply = System.currentTimeMillis();
        switch (state) {
            case "ready" -> {
                if (!busy || selected == null || confirmedOffset != 0) {
                    localError("invalid_chunk");
                    return;
                }
                offset = 0;
                nextChunk();
            }
            case "chunk" -> {
                if (confirmedOffset != offset + sent || confirmedOffset > length) {
                    localError("invalid_chunk");
                    return;
                }
                offset = confirmedOffset;
                status = Component.translatable("gui.oc2r.flash_writer.progress", offset * 100L / length);
                updateWidgets();
                if (offset == length) {
                    closeInput();
                    send("finish");
                } else {
                    nextChunk();
                }
            }
            case "done" -> {
                closeInput();
                busy = false;
                completed = true;
                status = text("success");
                updateWidgets();
            }
            case "error" -> localError(detail);
            default -> localError("invalid_image");
        }
    }

    private void nextChunk() {
        CompletableFuture.runAsync(() -> {
            try {
                if (closed) return;
                // The render thread may close the input concurrently, so the stream is read once
                // into a local variable and used from there. A runtime failure must not escape
                // the future, or the transfer would stall until the timeout expires.
                InputStream stream = input;
                if (stream == null) {
                    stream = Files.newInputStream(selected);
                    input = stream;
                    if (closed) {
                        // The screen was closed while the file was being opened, so the stream
                        // would never be closed by closeInput() and would leak the handle.
                        closeInput();
                        return;
                    }
                }
                final byte[] bytes = stream.readNBytes(Math.min(FlashImageUpload.CHUNK_SIZE, length - offset));
                if (bytes.length == 0) {
                    Minecraft.getInstance().execute(() -> localError("io_error"));
                    return;
                }
                Minecraft.getInstance().execute(() -> {
                    if (closed) return;
                    sent = bytes.length;
                    NetworkMessages.sendToServer(new FlashWriterChunkMessage(position, session, offset, bytes));
                });
            } catch (final Exception error) {
                Minecraft.getInstance().execute(() -> localError("io_error"));
            }
        });
    }

    private void localError(final String key) {
        if (closed) return;
        closeInput();
        busy = false;
        completed = true; // A rejected session must be reopened to obtain a fresh server token.
        status = text(key);
        send("cancel");
        updateWidgets();
    }

    private void closeInput() {
        final InputStream stream = input;
        input = null;
        if (stream != null) {
            try {
                stream.close();
            } catch (final IOException ignored) {
                // Cancellation must still close the screen even if the selected file disappeared.
            }
        }
    }

    @Override
    public void tick() {
        if (busy && System.currentTimeMillis() - lastReply > 90_000) localError("expired");
        if (minecraft.player == null || !position.closerToCenterThan(minecraft.player.position(), 8)) onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        closed = true;
        closeInput();
        if (active == this) active = null; // NOPMD - Instance identity is required for ownership, canonical states and recursion guards.
        if (!completed) send("cancel");
        super.onClose();
    }
}
