/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.screen;

import java.nio.file.Path;

@FunctionalInterface
public interface FileChooserCallback {
    void onFileSelected(Path path);

    default void onCanceled() {
    }
}
