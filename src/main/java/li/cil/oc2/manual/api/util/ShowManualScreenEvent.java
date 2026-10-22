/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.util;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.ManualScreenStyle;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.platform.event.Event;

/**
 * May be fired to request showing the default implementation of the manual screen for the
 * given manual and style.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The event carries the live client navigation model to its screen.")
public final class ShowManualScreenEvent {
    public static final Event<Consumer<ShowManualScreenEvent>> SHOW_MANUAL_SCREEN_EVENT = new Event<>();

    private final ManualModel manualModel;
    @Nullable private final ManualStyle manualStyle;
    @Nullable private final ManualScreenStyle screenStyle;

    public ShowManualScreenEvent(final ManualModel model, @Nullable final ManualStyle manualStyle, @Nullable final ManualScreenStyle screenStyle) {
        this.manualModel = model;
        this.manualStyle = manualStyle;
        this.screenStyle = screenStyle;
    }

    public ShowManualScreenEvent(final ManualModel model) {
        this(model, null, null);
    }

    /**
     * The manual to show the default manual screen for.
     * <p>
     * This defines the contents of the displayed manual.
     *
     * @return the manual.
     */
    public ManualModel getManualModel() {
        return manualModel;
    }

    /**
     * The style to use for the default manual screen.
     * <p>
     * This defines the looks of the displayed manual.
     *
     * @return the style.
     */
    public Optional<ManualStyle> getManualStyle() {
        return Optional.ofNullable(manualStyle);
    }

    /**
     * The style overrides for the manual screen.
     *
     * @return the screen style.
     */
    public Optional<ManualScreenStyle> getScreenStyle() {
        return Optional.ofNullable(screenStyle);
    }
}
