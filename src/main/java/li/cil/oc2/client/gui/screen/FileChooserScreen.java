/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class FileChooserScreen extends Screen {

    ///////////////////////////////////////////////////////////////////

    private static final int MARGIN = 30;
    private static final int WIDGET_SPACING = 8;

    private static final int TEXT_FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LIST_ENTRY_HEIGHT = 12;

    private static final Component OPEN_TITLE_TEXT = text("gui.{mod}.file_chooser.title.load");
    private static final Component SAVE_TITLE_TEXT = text("gui.{mod}.file_chooser.title.save");
    private static final Component FILE_NAME_TEXT = text("gui.{mod}.file_chooser.text_field.filename");
    private static final Component LOAD_TEXT = text("gui.{mod}.file_chooser.confirm_button.load");
    private static final Component SAVE_TEXT = text("gui.{mod}.file_chooser.confirm_button.save");
    private static final Component OVERWRITE_TEXT = text("gui.{mod}.file_chooser.confirm_button.overwrite");
    private static final Component CANCEL_TEXT = text("gui.{mod}.file_chooser.cancel_button");

    ///////////////////////////////////////////////////////////////////

    static Path directory = Paths.get("").toAbsolutePath();

    ///////////////////////////////////////////////////////////////////

    private final FileChooserCallback callback;
    private final boolean isLoad;

    private final Screen previousScreen;

    private FileList fileList;
    EditBox fileNameTextField;
    Button okButton;

    private boolean isComplete;

    ///////////////////////////////////////////////////////////////////

    public static void openFileChooserForSave(final String name, final FileChooserCallback callback) {
        final Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof FileChooserScreen) {
            currentScreen.onClose();
        }

        final FileChooserScreen screen = new FileChooserScreen(callback, false);
        Minecraft.getInstance().setScreen(screen);
        screen.fileNameTextField.setValue(name);
    }

    public static void openFileChooserForLoad(final FileChooserCallback callback) {
        final Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof FileChooserScreen) {
            currentScreen.onClose();
        }

        final FileChooserScreen screen = new FileChooserScreen(callback, true);
        Minecraft.getInstance().setScreen(screen);
    }

    ///////////////////////////////////////////////////////////////////

    public FileChooserScreen(final FileChooserCallback callback, final boolean isLoad) {
        super(isLoad ? OPEN_TITLE_TEXT : SAVE_TITLE_TEXT);

        this.callback = callback;
        this.isLoad = isLoad;

        this.previousScreen = Minecraft.getInstance().screen;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void onClose() {
        if (!isComplete) {
            callback.onCanceled();
        }

        if (previousScreen != null) {
            getMinecraft().tell(() -> getMinecraft().setScreen(previousScreen));
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        final int widgetsWidth = width - MARGIN * 2;
        final int listHeight = height - MARGIN - WIDGET_SPACING - TEXT_FIELD_HEIGHT - WIDGET_SPACING - BUTTON_HEIGHT - MARGIN;
        fileList = new FileList(this, width, MARGIN, listHeight, LIST_ENTRY_HEIGHT);
        addRenderableWidget(fileList);

        final int fileNameTop = MARGIN + listHeight + WIDGET_SPACING;
        fileNameTextField = new EditBox(font, MARGIN, fileNameTop, widgetsWidth, TEXT_FIELD_HEIGHT, FILE_NAME_TEXT);
        fileNameTextField.setResponder(s -> {
            fileList.setSelected(null);
            updateButtons();
        });
        fileNameTextField.setMaxLength(1024);
        addRenderableWidget(fileNameTextField);

        final int buttonTop = fileNameTop + TEXT_FIELD_HEIGHT + WIDGET_SPACING;
        final int buttonCount = 2;
        final int buttonWidth = widgetsWidth / buttonCount - WIDGET_SPACING;
        okButton = addRenderableWidget(
            Button.builder(Component.empty(), this::handleOkPressed)
                .bounds(MARGIN, buttonTop, buttonWidth, BUTTON_HEIGHT)
                .createNarration(Supplier::get)
                .build()
        );
        addRenderableWidget(
            Button.builder(CANCEL_TEXT, this::handleCancelPressed)
                .bounds(MARGIN + buttonWidth + WIDGET_SPACING, buttonTop, buttonWidth, BUTTON_HEIGHT)
                .createNarration(Supplier::get)
                .build()
        );

        fileList.refreshFiles(directory);

        updateButtons();
    }

    @Override
    public void onFilesDrop(final List<Path> files) {
        files.stream().filter(file -> {
            try {
                return Files.exists(file) && !Files.isHidden(file);
            } catch (final IOException | SecurityException ignored) {
                return false;
            }
        }).findFirst().ifPresent(fileList::selectPath);
    }

    ///////////////////////////////////////////////////////////////////

    private boolean isParentPath() {
        if (directory == null) {
            return false;
        }

        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return Objects.equals(selected.file, directory.getParent());
        }

        final String selectedFileEntry = fileNameTextField.getValue();
        return "..".equals(selectedFileEntry);
    }

    private Optional<Path> getPath() {
        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return Optional.ofNullable(selected.file);
        }

        if (directory == null) {
            return Optional.empty();
        }

        final String selectedFileEntry = fileNameTextField.getValue();
        if (selectedFileEntry.isEmpty() || ".".equals(selectedFileEntry)) {
            return Optional.empty();
        }

        try {
            return Optional.of(directory.resolve(selectedFileEntry));
        } catch (final InvalidPathException e) {
            return Optional.empty();
        }
    }

    void confirm() {
        if (isParentPath()) {
            fileList.refreshFiles(getPath().orElse(null));
            return;
        }

        getPath().ifPresent(path -> {
            if (Files.isDirectory(path)) {
                fileList.refreshFiles(path);
                return;
            }
            if (Files.isRegularFile(path)) {
                isComplete = true;
                callback.onFileSelected(path);
                onClose();
            } else if (!isLoad) {
                isComplete = true;
                callback.onFileSelected(path);
                onClose();
            }
        });
    }

    private void cancel() {
        isComplete = true;
        callback.onCanceled();
        onClose();
    }

    void updateButtons() {
        okButton.active = false;
        okButton.setMessage(isLoad ? LOAD_TEXT : SAVE_TEXT);
        okButton.clearFGColor();

        if (isParentPath()) {
            okButton.active = true;
            return;
        }

        getPath().ifPresent(path -> {
            if (isLoad) {
                okButton.active = Files.exists(path);
            } else {
                okButton.active = true;
                if (Files.isRegularFile(path)) {
                    okButton.setMessage(OVERWRITE_TEXT);
                    okButton.setFGColor(0xFF0000);
                }
            }
        });
    }

    private void handleOkPressed(final Button button) {
        confirm();
    }

    private void handleCancelPressed(final Button button) {
        cancel();
    }

    ///////////////////////////////////////////////////////////////////
}
