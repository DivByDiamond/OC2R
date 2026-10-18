/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.gui;

import java.util.*;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.ManualScreenStyle;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.api.Tab;
import li.cil.oc2.manual.api.content.Document;
import li.cil.oc2.manual.client.document.DocumentRenderer;
import li.cil.oc2.manual.client.document.segment.InteractiveSegment;
import li.cil.oc2.manual.client.util.IterableUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The screen updates the shared navigation model.")
public final class ManualScreen extends Screen {
    private final ManualModel model;
    private final ManualStyle manualStyle;
    private final ManualScreenStyle screenStyle;
    private final DocumentRenderer documentRenderer;
    private String currentPath;

    private int leftPos = 0;
    private int topPos = 0;
    private float scrollPos = 0;

    private boolean isDraggingScrollButton = false;
    private int documentHeight = 0;
    private Optional<InteractiveSegment> currentSegment = Optional.empty();

    private ScrollButton scrollButton = null;

    public ManualScreen(final ManualModel model, final ManualStyle manualStyle, final ManualScreenStyle screenStyle) {
        super(Component.literal("Manual"));
        this.model = model;
        this.manualStyle = manualStyle;
        this.screenStyle = screenStyle;
        this.documentRenderer = new DocumentRenderer(model, manualStyle);
    }

    @Override
    public void init() {
        super.init();

        this.leftPos = (width - screenStyle.getWindowRect().getWidth()) / 2 + screenStyle.getWindowRect().getX();
        this.topPos = (height - screenStyle.getWindowRect().getHeight()) / 2 + screenStyle.getWindowRect().getY();

        IterableUtils.forEachWithIndex(model.getTabs(), (i, tab) -> {
            final int x = screenStyle.getTabAreaRect().getX();
            final int y = screenStyle.getTabAreaRect().getY() + i * getTabClickableHeight();
            if (y + screenStyle.getTabRect().getHeight() > screenStyle.getTabAreaRect().getHeight()) return;
            addRenderableWidget(new TabButton(leftPos + x, topPos + y, tab, (button) -> pushManualPage(tab)));
        });

        scrollButton = addWidget(new ScrollButton(
            leftPos + screenStyle.getScrollBarRect().getX() + screenStyle.getScrollButtonRect().getX(),
            topPos + screenStyle.getScrollBarRect().getY() + screenStyle.getScrollButtonRect().getY(),
            screenStyle.getScrollButtonRect().getWidth(),
            screenStyle.getScrollButtonRect().getHeight()));
    }

    @Override
    //? if >=26.1 {
        /*public void extractRenderState(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        *///?} else {
        public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        //?}
        // 26.x's screen wrapper extracts the background before calling this method.
        //? if <26.1 {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        //?}

        if (!Objects.equals(currentPath, model.peek())) {
            refreshPage();
            currentPath = model.peek();

            final SoundManager soundHandler = Minecraft.getInstance().getSoundManager();
            soundHandler.play(SimpleSoundInstance.forUI(manualStyle.getPageChangeSound(), 1));
        }

        scrollPos = li.cil.oc2.manual.client.util.ManualGraphics.approach(scrollPos, getScrollPosition(), partialTicks);

        // Don't render scroll button behind manual.
        scrollButton.active = false;

        // Render tabs behind manual.
        //? if >=26.1 {
        /*super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        *///?} else {
        super.render(graphics, mouseX, mouseY, partialTicks);
        //?}

        // Render manual background.
        final Rect2i windowRect = screenStyle.getWindowRect();
        li.cil.oc2.manual.client.util.ManualGraphics.blit(graphics, screenStyle.getWindowBackground(), leftPos, topPos, 0, 0, windowRect.getWidth(), windowRect.getHeight(), windowRect.getWidth(), windowRect.getHeight());

        // Render scroll bar tooltip (button will override this tooltip if currently dragging).
        renderScrollbarTooltip(graphics, mouseX, mouseY);

        // Render scroll button in front of manual.
        scrollButton.active = canScroll();
        //? if >=26.1 {
        /*scrollButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        *///?} else {
        scrollButton.render(graphics, mouseX, mouseY, partialTicks);
        //?}

        final Rect2i documentRect = screenStyle.getDocumentRect();
        final int documentX = leftPos + documentRect.getX();
        final int documentY = topPos + documentRect.getY();

        final var pose = li.cil.oc2.manual.client.util.ManualGraphics.pose(graphics);
        pose.pushPose();
        pose.translate(documentX, documentY, 0);

        currentSegment = documentRenderer.render(graphics, getSmoothScrollPosition(),
            documentRect.getWidth(), documentRect.getHeight(),
            mouseX - documentX, mouseY - documentY);

        pose.popPose();

        currentSegment.flatMap(InteractiveSegment::getTooltip).ifPresent(t ->
            li.cil.oc2.manual.client.util.ManualGraphics.componentTooltip(graphics, font, Collections.singletonList(t), mouseX, mouseY));
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double deltaX, final double deltaY) {
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }

        scrollBy(deltaY);
        return true;
    }

    //? if >=26.1 {
    /*    @Override
    public boolean keyPressed(final net.minecraft.client.input.KeyEvent event) {
        final Minecraft mc = Objects.requireNonNull(minecraft);
        if (mc.options.keyJump.matches(event)) {
            popManualPage();
            return true;
        } else if (mc.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    *///?} else {
    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        final Minecraft mc = Objects.requireNonNull(minecraft);
        if (mc.options.keyJump.matches(keyCode, scanCode)) {
            popManualPage();
            return true;
        } else if (mc.options.keyInventory.matches(keyCode, scanCode)) {
            final LocalPlayer player = mc.player;
            if (player != null) {
                player.closeContainer();
            }
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    //?}

    @Override
    //? if >=26.1 {
        /*public boolean mouseClicked(final net.minecraft.client.input.MouseButtonEvent event, final boolean doubleClick) {
        final double mouseX = event.x(), mouseY = event.y();
        final int button = event.button();
        *///?} else {
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        //?}
        //? if >=26.1 {
        /*if (super.mouseClicked(event, doubleClick)) {
        *///?} else {
        if (super.mouseClicked(mouseX, mouseY, button)) {
        //?}
            return true;
        }

        if (canScroll() && button == 0 && isCoordinateOverScrollBar(mouseX, mouseY)) {
            isDraggingScrollButton = true;
            scrollButton.playDownSound(Minecraft.getInstance().getSoundManager());
            scrollTo(mouseY);
            return true;
        } else if (button == 0) {
            return currentSegment.map(InteractiveSegment::mouseClicked).orElse(false);
        } else if (button == 1) {
            popManualPage();
            return true;
        }

        return false;
    }

    @Override
    //? if >=26.1 {
        /*public boolean mouseDragged(final net.minecraft.client.input.MouseButtonEvent event, final double dragX, final double dragY) {
        final double mouseY = event.y();
        *///?} else {
        public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX, final double dragY) {
        //?}
        //? if >=26.1 {
        /*if (super.mouseDragged(event, dragX, dragY)) {
        *///?} else {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
        //?}
            return true;
        }

        if (isDraggingScrollButton) {
            scrollTo(mouseY);
            return true;
        }

        return false;
    }

    @Override
    //? if >=26.1 {
        /*public boolean mouseReleased(final net.minecraft.client.input.MouseButtonEvent event) {
        final int button = event.button();
        *///?} else {
        public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        //?}
        //? if >=26.1 {
        /*super.mouseReleased(event);
        *///?} else {
        super.mouseReleased(mouseX, mouseY, button);
        //?}

        if (button == 0) {
            isDraggingScrollButton = false;
        }

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --------------------------------------------------------------------- //

    private void renderScrollbarTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        if (isCoordinateOverScrollBar(mouseX, mouseY)) {
            scrollButton.applyTooltip(graphics, mouseX, mouseY, false);
        }
    }

    private void pushManualPage(final Tab tab) {
        model.push(tab.getPath());
    }

    private void popManualPage() {
        if (!model.pop()) {
            onClose();
        }
    }

    private boolean canScroll() {
        return maxScrollPosition() > 0;
    }

    private int getScrollPosition() {
        return model.getUserData(ScrollOffset.class)
            .map(offset -> offset.value)
            .orElse(0);
    }

    private void setScrollPosition(final int value) {
        model.setUserData(new ScrollOffset(value));
    }

    private int maxScrollPosition() {
        return Math.max(0, documentHeight - screenStyle.getDocumentRect().getHeight());
    }

    private void refreshPage() {
        final Optional<Document> document = model.documentFor(model.peek());
        documentRenderer.parse(document.orElse(new Document(Collections.singletonList("Page not found: " + model.peek()))));
        documentHeight = documentRenderer.height(screenStyle.getDocumentRect().getWidth());
        scrollPos = getScrollPosition() - manualStyle.getLineHeight() * 3;
    }

    private void scrollTo(final double mouseY) {
        final int scrollButtonHeight = screenStyle.getScrollButtonRect().getHeight();
        final int halfScrollButtonHeight = (int) Math.ceil(scrollButtonHeight * 0.5);
        final int scrollMinY = topPos + screenStyle.getScrollBarRect().getY() + halfScrollButtonHeight;
        final int scrollHeight = screenStyle.getScrollBarRect().getHeight() - scrollButtonHeight; // Subtract half button top and bottom.
        final int localMouseY = (int) (mouseY - scrollMinY);
        scrollTo(maxScrollPosition() * localMouseY / scrollHeight, true);
    }

    private void scrollBy(final double amount) {
        scrollTo(getScrollPosition() - (int) Math.round(manualStyle.getLineHeight() * 3 * amount), false);
    }

    private void scrollTo(final int y, final boolean immediate) {
        setScrollPosition(Math.max(0, Math.min(maxScrollPosition(), y)));
        if (immediate) {
            scrollPos = getScrollPosition();
        }
    }

    private int getSmoothScrollPosition() {
        if (scrollPos < getScrollPosition()) {
            return (int) Math.ceil(scrollPos);
        } else {
            return (int) Math.floor(scrollPos);
        }
    }

    private int getScrollButtonY() {
        if (canScroll()) {
            final int yMax = screenStyle.getScrollBarRect().getHeight() - screenStyle.getScrollButtonRect().getHeight();
            return Math.max(0, Math.min(yMax, yMax * getSmoothScrollPosition() / maxScrollPosition()));
        } else {
            return 0;
        }
    }

    private boolean isCoordinateOverScrollBar(final double x, final double y) {
        return screenStyle.getScrollBarRect().contains((int) (x - leftPos), (int) (y - topPos));
    }

    private int getTabClickableHeight() {
        return screenStyle.getTabRect().getHeight() - screenStyle.getTabOverlap();
    }

    private record ScrollOffset(int value) {
    }

    private class TabButton extends Button {
        private final Tab tab;
        private final int baseX;
        private float currentX;
        private int targetX;

        TabButton(final int x, final int y, final Tab tab, final OnPress action) {
            super(x, y, screenStyle.getTabRect().getWidth(), getTabClickableHeight(), Component.empty(), action, DEFAULT_NARRATION);
            this.tab = tab;
            this.baseX = x;
            this.currentX = x + screenStyle.getTabHoverShift();
            this.targetX = (int) currentX;
        }

        @Override
        //? if >=26.1 {
        /*protected void extractContents(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        *///?} else {
        public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        //?}
            if (isHoveredOrFocusedUsingKeyboard()) {
                targetX = baseX;
            } else {
                targetX = baseX + screenStyle.getTabHoverShift();
            }

            currentX = li.cil.oc2.manual.client.util.ManualGraphics.approach(currentX, targetX, partialTicks);

            if (currentX < targetX) {
                setX((int) Math.ceil(currentX));
            } else {
                setX((int) Math.floor(currentX));
            }

            width = screenStyle.getTabAreaRect().getWidth() - (getX() - baseX);

            final int v0 = isHoveredOrFocusedUsingKeyboard() ? screenStyle.getTabRect().getHeight() : 0;
            final int visualWidth = screenStyle.getTabRect().getWidth();
            final int visualHeight = screenStyle.getTabRect().getHeight();
            final int textureWidth = screenStyle.getTabRect().getWidth();
            final int textureHeight = screenStyle.getTabRect().getHeight() * 2;

            li.cil.oc2.manual.client.util.ManualGraphics.blit(graphics, screenStyle.getTabButtonTexture(), getX(), getY(), 0, v0, visualWidth, visualHeight, textureWidth, textureHeight);

            final var pose = li.cil.oc2.manual.client.util.ManualGraphics.pose(graphics);
            pose.pushPose();
            pose.translate(getX() + 12, (float) (getY() + (screenStyle.getTabRect().getHeight() - 18) / 2), 0);

            tab.renderIcon(graphics);

            pose.popPose();

            updateTooltip(graphics, mouseX, mouseY);
        }

        private void updateTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
            if (!isHoveredOrFocusedUsingKeyboard() || isDraggingScrollButton) {
                return;
            }

            final var tooltip = new ArrayList<Component>();
            tab.getTooltip(tooltip);
            if (tooltip.isEmpty()) {
                return;
            }

            final var screen = li.cil.oc2.client.RenderCompat.currentScreen();
            if (screen != null) {
                li.cil.oc2.manual.client.util.ManualGraphics.tooltip(graphics, font,
                        tooltip.stream().map(Component::getVisualOrderText).toList(),
                        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, mouseX, mouseY);
            }
        }

        @Override
        public void playDownSound(final SoundManager soundHandler) {
        }

        private boolean isHoveredOrFocusedUsingKeyboard() {
            return isHovered() || (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard());
        }
    }

    private class ScrollButton extends Button {
        private static final int TOOLTIP_HEIGHT = 18;

        private final int baseY;

        ScrollButton(final int x, final int y, final int w, final int h) {
            super(x, y, w, h, Component.empty(), (button) -> {
            }, DEFAULT_NARRATION);
            this.baseY = y;
        }

        @Override
        //? if >=26.1 {
        /*public boolean mouseClicked(final net.minecraft.client.input.MouseButtonEvent event, final boolean doubleClick) {
            final double mouseX = event.x(), mouseY = event.y();
        *///?} else {
        protected boolean clicked(final double mouseX, final double mouseY) {
        //?}
            //? if >=26.1 {
        /*if (isMouseOver(mouseX, mouseY)) {
        *///?} else {
        if (super.clicked(mouseX, mouseY)) {
        //?}
                playDownSound(Minecraft.getInstance().getSoundManager());
            }
            return false;
        }

        @Override
        //? if >=26.1 {
        /*protected void extractContents(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        *///?} else {
        public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        //?}
            setY(baseY + getScrollButtonY());

            final int vOffset = (isDraggingScrollButton || isHoveredOrFocusedUsingKeyboard()) ? height : 0;
            li.cil.oc2.manual.client.util.ManualGraphics.blit(graphics, screenStyle.getScrollButtonTexture(), getX(), getY(), 0, vOffset, width, height, width, height * 2);

            updateTooltip(graphics, mouseX, mouseY);
        }

        public void applyTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY, final boolean fixedY) {
            final var screen = li.cil.oc2.client.RenderCompat.currentScreen();
            if (screen != null && canScroll()) {
                li.cil.oc2.manual.client.util.ManualGraphics.tooltip(graphics, font, getTooltipContent(),
                        getClientTooltipPositioner(fixedY), mouseX, mouseY);
            }
        }

        private void updateTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
            if (!isHoveredOrFocusedUsingKeyboard() && !isDraggingScrollButton) {
                return;
            }

            applyTooltip(graphics, mouseX, mouseY, true);
        }

        private List<FormattedCharSequence> getTooltipContent() {
            return List.of(Component.literal(100 * getScrollPosition() / maxScrollPosition() + "%").getVisualOrderText());
        }

        private ClientTooltipPositioner getClientTooltipPositioner(final boolean fixedY) {
            return (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> new Vector2i(
                leftPos + screenStyle.getScrollBarRect().getX() + screenStyle.getScrollBarRect().getWidth(),
                fixedY ? getY() + (getHeight() + TOOLTIP_HEIGHT) / 2 : mouseY);
        }

        private boolean isHoveredOrFocusedUsingKeyboard() {
            return isHovered() || (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard());
        }
    }
}
