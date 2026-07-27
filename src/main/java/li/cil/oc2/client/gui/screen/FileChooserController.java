package li.cil.oc2.client.gui.screen;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

class FileChooserController {
    private final FileChooserScreen screen;
    private final FileList fileList;
    private final EditBox fileNameTextField;
    private final Button okButton;
    private final FileChooserCallback callback;
    private final boolean isLoad;
    private boolean complete;

    FileChooserController(
            final FileChooserScreen screen,
            final FileList fileList,
            final EditBox fileNameTextField,
            final Button okButton,
            final FileChooserCallback callback,
            final boolean isLoad) {
        this.screen = screen;
        this.fileList = fileList;
        this.fileNameTextField = fileNameTextField;
        this.okButton = okButton;
        this.callback = callback;
        this.isLoad = isLoad;
    }

    boolean isComplete() {
        return complete;
    }

    void confirm() {
        if (isParentPath()) {
            fileList.refreshFiles(getPath().orElse(null));
            return;
        }

        getPath()
                .ifPresent(
                        path -> {
                            if (Files.isDirectory(path)) {
                                fileList.refreshFiles(path);
                                return;
                            }
                            if (Files.isRegularFile(path)) {
                                complete = true;
                                callback.onFileSelected(path);
                                screen.onClose();
                            } else if (!isLoad) {
                                complete = true;
                                callback.onFileSelected(path);
                                screen.onClose();
                            }
                        });
    }

    void cancel() {
        complete = true;
        callback.onCanceled();
        screen.onClose();
    }

    void updateButtons() {
        okButton.active = false;
        okButton.setMessage(isLoad ? FileChooserScreen.LOAD_TEXT : FileChooserScreen.SAVE_TEXT);
        okButton.clearFGColor();

        if (isParentPath()) {
            okButton.active = true;
            return;
        }

        getPath()
                .ifPresent(
                        path -> {
                            if (isLoad) {
                                okButton.active = Files.exists(path);
                            } else {
                                okButton.active = true;
                                if (Files.isRegularFile(path)) {
                                    okButton.setMessage(FileChooserScreen.OVERWRITE_TEXT);
                                    okButton.setFGColor(0xFF0000);
                                }
                            }
                        });
    }

    private boolean isParentPath() {
        if (FileChooserScreen.directory == null) {
            return false;
        }

        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return Objects.equals(selected.file, FileChooserScreen.directory.getParent());
        }

        final String selectedFileEntry = fileNameTextField.getValue();
        return "..".equals(selectedFileEntry);
    }

    private Optional<Path> getPath() {
        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return Optional.ofNullable(selected.file);
        }

        if (FileChooserScreen.directory == null) {
            return Optional.empty();
        }

        final String selectedFileEntry = fileNameTextField.getValue();
        if (selectedFileEntry.isEmpty() || ".".equals(selectedFileEntry)) {
            return Optional.empty();
        }

        try {
            return Optional.of(FileChooserScreen.directory.resolve(selectedFileEntry));
        } catch (final InvalidPathException e) {
            return Optional.empty();
        }
    }
}