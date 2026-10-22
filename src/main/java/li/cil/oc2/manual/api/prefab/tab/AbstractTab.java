/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.tab;

import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.manual.api.Tab;
import net.minecraft.network.chat.Component;

public abstract class AbstractTab implements Tab {
    private final String path;
    @Nullable private final Component tooltip;

    public AbstractTab(final String path, @Nullable final Component tooltip) {
        this.path = path;
        this.tooltip = tooltip;
    }

    @Override
    public void getTooltip(final List<Component> tooltip) {
        if (this.tooltip != null) {
            tooltip.add(this.tooltip);
        }
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
