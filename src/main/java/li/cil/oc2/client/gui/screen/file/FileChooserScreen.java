package li.cil.oc2.client.gui.screen.file;

import static li.cil.oc2.common.util.TranslationUtils.text;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FileChooserScreen extends Screen {

    private static final int MARGIN = 30;
    private static final int WIDGET_SPACING = 8;
    private static final int TEXT_FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LIST_ENTRY_HEIGHT = 12;

    private static final Component OPEN_TITLE_TEXT = text("gui.{mod}.file_chooser.title.load");
    private static final Component SAVE_TITLE_TEXT = text("gui.{mod}.file_chooser.title.save");
    private static final Component FILE_NAME_TEXT =
            text("gui.{mod}.file_chooser.text_field.filename");
    static final Component LOAD_TEXT = text("gui.{mod}.file_chooser.confirm_button.load");
    static final Component SAVE_TEXT = text("gui.{mod}.file_chooser.confirm_button.save");
    static final Component OVERWRITE_TEXT = text("gui.{mod}.file_chooser.confirm_button.overwrite");
    private static final Component CANCEL_TEXT = text("gui.{mod}.file_chooser.cancel_button");

    static Path directory = Paths.get("").toAbsolutePath();

    private final FileChooserCallback callback;
    private final boolean isLoad;
    private final Screen previousScreen;

    private FileList fileList;
    EditBox fileNameTextField;
    Button okButton;
    private FileChooserController controller;

    public static void openFileChooserForSave(
            final String name, final FileChooserCallback callback) {
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

    public FileChooserScreen(final FileChooserCallback callback, final boolean isLoad) {
        super(isLoad ? OPEN_TITLE_TEXT : SAVE_TITLE_TEXT);

        this.callback = callback;
        this.isLoad = isLoad;

        this.previousScreen = Minecraft.getInstance().screen;
    }

    @Override
    public void onClose() {
        if (!controller.isComplete()) {
            callback.onCanceled();
        }

        if (previousScreen != null) {
            getMinecraft().tell(() -> getMinecraft().setScreen(previousScreen));
        }
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        final int widgetsWidth = width - MARGIN * 2;
        final int listHeight =
                height
                        - MARGIN
                        - WIDGET_SPACING
                        - TEXT_FIELD_HEIGHT
                        - WIDGET_SPACING
                        - BUTTON_HEIGHT
                        - MARGIN;
        fileList = new FileList(this, width, MARGIN, listHeight, LIST_ENTRY_HEIGHT);
        addRenderableWidget(fileList);

        final int fileNameTop = MARGIN + listHeight + WIDGET_SPACING;
        fileNameTextField =
                new EditBox(
                        font, MARGIN, fileNameTop, widgetsWidth, TEXT_FIELD_HEIGHT, FILE_NAME_TEXT);
        fileNameTextField.setResponder(
                s -> {
                    fileList.setSelected(null);
                    controller.updateButtons();
                });
        fileNameTextField.setMaxLength(1024);
        addRenderableWidget(fileNameTextField);

        final int buttonTop = fileNameTop + TEXT_FIELD_HEIGHT + WIDGET_SPACING;
        final int buttonCount = 2;
        final int buttonWidth = widgetsWidth / buttonCount - WIDGET_SPACING;
        okButton =
                addRenderableWidget(
                        Button.builder(Component.empty(), b -> handleOkPressed())
                                .bounds(MARGIN, buttonTop, buttonWidth, BUTTON_HEIGHT)
                                .createNarration(Supplier::get)
                                .build());
        addRenderableWidget(
                Button.builder(CANCEL_TEXT, b -> handleCancelPressed())
                        .bounds(
                                MARGIN + buttonWidth + WIDGET_SPACING,
                                buttonTop,
                                buttonWidth,
                                BUTTON_HEIGHT)
                        .createNarration(Supplier::get)
                        .build());

        controller =
                new FileChooserController(
                        this, fileList, fileNameTextField, okButton, callback, isLoad);

        fileList.refreshFiles(directory);

        controller.updateButtons();
    }

    @Override
    public void onFilesDrop(final List<Path> files) {
        files.stream()
                .filter(
                        file -> {
                            try {
                                return Files.exists(file) && !Files.isHidden(file);
                            } catch (final IOException | SecurityException ignored) {
                                return false;
                            }
                        })
                .findFirst()
                .ifPresent(fileList::selectPath);
    }

    void updateButtons() {
        controller.updateButtons();
    }

    void confirm() {
        controller.confirm();
    }

    private void cancel() {
        controller.cancel();
    }

    private void handleOkPressed() {
        confirm();
    }

    private void handleCancelPressed() {
        cancel();
    }
}