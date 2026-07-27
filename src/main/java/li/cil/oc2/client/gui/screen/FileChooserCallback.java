package li.cil.oc2.client.gui.screen;

import java.nio.file.Path;

@FunctionalInterface
public interface FileChooserCallback {
    /**
     * Called when a file is selected in the file chooser.
     *
     * @param path the selected file path.
     */
    void onFileSelected(Path path);

    /** Called when the file chooser is canceled. */
    default void onCanceled() {}
}