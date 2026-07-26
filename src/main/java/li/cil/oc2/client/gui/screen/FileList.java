/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

final class FileList extends ObjectSelectionList<FileList.FileEntry> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final FileChooserScreen screen;
    private final Font font;

    FileList(final FileChooserScreen screen, final int width, final int y, final int height, final int slotHeight) {
        super(screen.getMinecraft(), width, height, y, slotHeight);
        this.screen = screen;
        this.font = Minecraft.getInstance().font;
    }

    void refreshFiles(@Nullable final Path directory) {
        FileChooserScreen.directory = directory;

        setScrollAmount(0);
        clearEntries();

        if (directory != null && Files.isDirectory(directory)) {
            addEntry(createDirectoryEntry(directory.getParent(), ".."));

            try {
                final List<Path> files = Files.list(directory)
                    .sorted((p1, p2) -> {
                        if (Files.isDirectory(p1) && !Files.isDirectory(p2)) return -1;
                        if (!Files.isDirectory(p1) && Files.isDirectory(p2)) return 1;
                        return p1.getFileName().compareTo(p2.getFileName());
                    }).toList();
                for (final Path path : files) {
                    try {
                        if (Files.isHidden(path)) continue;
                        if (Files.isDirectory(path)) {
                            addEntry(createDirectoryEntry(path));
                        } else {
                            addEntry(createFileEntry(path));
                        }
                    } catch (final IOException | SecurityException ignored) {
                    }
                }
            } catch (final IOException | SecurityException e) {
                LOGGER.error(e);
            }
        } else {
            for (final Path path : FileSystems.getDefault().getRootDirectories()) {
                addEntry(createDirectoryEntry(path, path.toString()));
            }
        }

        screen.fileNameTextField.setValue("");
    }

    void selectPath(final Path path) {
        if (Files.isDirectory(path)) {
            refreshFiles(path);
        } else {
            refreshFiles(path.getParent());
            children().stream().filter(entry -> path.equals(entry.file))
                .findFirst().ifPresent(entry -> {
                    entry.select();
                    centerScrollOn(entry);
                });
        }
    }

    @Override
    public void setSelected(@Nullable final FileEntry entry) {
        super.setSelected(entry);
        screen.updateButtons();
    }

    ///////////////////////////////////////////////////////////////////

    private FileEntry createFileEntry(final Path file) {
        return new FileEntry(file, Component.literal(file.getFileName().toString()));
    }

    private FileEntry createDirectoryEntry(final Path path) {
        return createDirectoryEntry(path, path.getFileName().toString() + path.getFileSystem().getSeparator());
    }

    private FileEntry createDirectoryEntry(@Nullable final Path path, final String displayName) {
        final TextColor color = path != null && Files.exists(path)
            ? TextColor.fromRgb(0xA0A0FF)
            : TextColor.fromLegacyFormat(ChatFormatting.GRAY);
        return new FileEntry(path, Component.literal(displayName)
            .withStyle(s -> s.withColor(color)));
    }

    ///////////////////////////////////////////////////////////////////

    final class FileEntry extends ObjectSelectionList.Entry<FileEntry> {
        @Nullable final Path file;
        private final Component displayName;

        private long lastEntryClickTime = 0;

        FileEntry(@Nullable final Path file, final Component displayName) {
            this.file = file;
            this.displayName = displayName;
        }

        @Override
        public void render(final GuiGraphics graphics, final int index, final int top, final int left, final int width, final int height,
                           final int mouseX, final int mouseY, final boolean isHovered, final float deltaTime) {
            drawShadow(font, graphics, displayName, left, top, 0xFFFFFFFF);
        }

        private void drawShadow(Font font, GuiGraphics graphics, Component text, float x, float y, int color) {
            var batch = graphics.bufferSource();
            font.drawInBatch(text, x, y, color, true, graphics.pose().last().pose(), batch, Font.DisplayMode.NORMAL, 0, 15728880);
            batch.endBatch();
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            final boolean isLeftClick = button == 0;
            if (isLeftClick) {
                select();

                final boolean isDoubleClick = System.currentTimeMillis() - lastEntryClickTime < 250;
                if (isDoubleClick && screen.okButton.active) {
                    screen.confirm();
                }

                lastEntryClickTime = System.currentTimeMillis();
            }

            return false;
        }

        void select() {
            if (FileChooserScreen.directory != null && Objects.equals(FileChooserScreen.directory.getParent(), file)) {
                screen.fileNameTextField.setValue("..");
            } else if (file != null) {
                final Path fileName = file.getFileName();
                screen.fileNameTextField.setValue(fileName != null ? fileName.toString() : file.toString());
            } else {
                return;
            }
            screen.fileNameTextField.moveCursorToStart(true);
            setSelected(this);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", displayName);
        }
    }
}
