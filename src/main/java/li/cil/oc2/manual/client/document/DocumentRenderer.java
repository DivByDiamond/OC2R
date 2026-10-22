/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.api.content.Document;
import li.cil.oc2.manual.client.document.segment.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

/**
 * Primitive Markdown parser, only supports a very small subset. Used for
 * parsing documentation into segments, to be displayed in a GUI somewhere.
 * <p>
 * General usage is: parse a string using parse(), render it using render().
 * <p>
 * The parser generates a list of segments, each segment representing a part
 * of the document, with a specific formatting / render type. For example,
 * links are their own segments, a bold section in a link would be its own
 * section and so on.
 * The data structure is essentially a very flat multi-tree, where the segments
 * returned are the leaves, and the roots are the individual lines, represented
 * as text segments.
 * Formatting is done by accumulating formatting information over the parent
 * nodes, up to the root.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Rendering and navigation share the same live manual model.")
public final class DocumentRenderer {
    private final ManualModel model;
    private final ManualStyle style;
    @Nullable
    private ResourceLocation location;
    @Nullable
    private Segment root;
    @Nullable
    private InteractiveSegment lastHovered;

    private int lastScrollY;
    private int lastGlobalY;
    @Nullable
    private NextSegmentInfo lastFirstVisible;

    /**
     * Creates a new document instance with the specified configuration.
     *
     * @param model the manual the document belongs to.
     * @param style the fonts to use when rendering the generated segments.
     */
    public DocumentRenderer(final ManualModel model, final ManualStyle style) {
        this.model = model;
        this.style = style;
    }

    public ManualModel getModel() {
        return model;
    }

    public ManualStyle getStyle() {
        return style;
    }

    @Nullable
    public ResourceLocation getLocation() {
        return location;
    }

    /**
     * Parses a plain text document into a list of segments.
     * <p>
     * This replaces the current content of this document.
     *
     * @param document iterator over the lines of the document to parse.
     */
    public void parse(final Document document) {
        location = document.getLocation();

        // Recognize block fences before inline formatting. Each code line is its own
        // root so line breaks, including empty lines, survive the segment layout.
        List<Segment> segments = new ArrayList<>();
        int fenceLength = 0;
        int fenceIndent = 0;
        for (final String line : document.getLines()) {
            final Matcher fence = CODE_FENCE.matcher(line);
            final boolean isFence = fence.matches();
            if (fenceLength > 0) {
                if (isFence && fence.group(2).length() >= fenceLength && fence.group(3).isBlank()) {
                    fenceLength = 0;
                } else {
                    int indent = 0;
                    while (indent < fenceIndent && indent < line.length() && line.charAt(indent) == ' ') {
                        indent++;
                    }
                    segments.add(new CodeBlockLineSegment(this, line.substring(indent)));
                }
            } else if (isFence && fence.group(3).indexOf('`') < 0) {
                fenceLength = fence.group(2).length();
                fenceIndent = fence.group(1).length();
            } else {
                segments.add(new TextSegment(this, null, StringUtils.stripEnd(line, null)));
            }
        }

        // Refine text segments into sub-types.
        List<Segment> workSegments = new ArrayList<>();
        for (final PatternMapping type : SEGMENT_TYPES) {
            for (final Segment segment : segments) {
                segment.refine(type.pattern, type.refiner).forEach(workSegments::add);
            }

            // Swap buffers.
            final List<Segment> tmp = segments;
            segments = workSegments;
            workSegments = tmp;
            workSegments.clear();
        }

        for (int i = 0; i < segments.size() - 1; i++) {
            segments.get(i).setNext(segments.get(i + 1));
        }

        root = !segments.isEmpty() ? segments.get(0) : new TextSegment(this, null, "");

        if (lastHovered != null) {
            lastHovered.setMouseHovered(false);
            lastHovered = null;
        }

        lastFirstVisible = null;
    }

    /**
     * Compute the overall height of a document, e.g. for computation of scroll offsets.
     *
     * @param width the width available for rendering the document.
     * @return the height of the document.
     */
    public int height(final int width) {
        if (root == null) {
            return 0;
        }

        int globalY = 0;
        int lineHeight = 0;
        NextSegmentInfo current = new NextSegmentInfo(root);
        while (current.segment != null) {
            final Segment segment = current.segment;
            final int localX = current.absoluteX;
            final int relativeY = current.relativeY;
            final int segmentHeight = segment.getLineHeight(localX, width);

            globalY += relativeY;
            current = segment.getNext(localX, lineHeight, width);

            final boolean isFirstSegmentOnNewLine = current.relativeY > 0;
            if (isFirstSegmentOnNewLine) {
                lineHeight = current.absoluteX > 0 ? segmentHeight : 0;
            } else {
                lineHeight = Math.max(lineHeight, segmentHeight);
            }

            // For the last segment we also want to take into account that element's height.
            // Next sibling may still be allowed to render on same line, so we'd cut off
            // the last line in the height computation.
            if (current.segment == null) {
                if (current.relativeY > 0) {
                    if (current.absoluteX > 0) {
                        globalY += current.relativeY + segmentHeight;
                    } else {
                        globalY += current.relativeY;
                    }
                } else {
                    globalY += lineHeight;
                }
            }
        }
        return globalY;
    }

    /**
     * Renders a list of segments and tooltips if a segment with a tooltip is hovered.
     * Returns the hovered interactive segment, if any.
     *
     * @param graphics the current graphics context.
     * @param scrollY  the vertical scroll offset of the document.
     * @param width    the width of the area to render the document in.
     * @param height   the height of the area to render the document in.
     * @param mouseX   the x position of the mouse relative to the document.
     * @param mouseY   the y position of the mouse relative to the document.
     * @return the interactive segment being hovered, if any.
     */
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"}) // Upstream segment layout/cache traversal.
    public Optional<InteractiveSegment> render(final GuiGraphics graphics, final int scrollY, final int width, final int height, final int mouseX, final int mouseY) {
        if (root == null) {
            return Optional.empty();
        }

        li.cil.oc2.manual.client.util.ManualGraphics.enableScissor(graphics, width, height);
        try {
        final var pose = li.cil.oc2.manual.client.util.ManualGraphics.pose(graphics);

        // Actual rendering.
        final boolean isMouseOverDocument = mouseX >= 0 && mouseX <= width && mouseY >= 0 && mouseY <= height;
        Optional<InteractiveSegment> hovered = Optional.empty();
        int globalY;
        int lineHeight = 0;
        NextSegmentInfo current;
        if (scrollY != lastScrollY || lastFirstVisible == null) {
            current = new NextSegmentInfo(root);
            lastScrollY = scrollY;
            globalY = -scrollY;
            lastFirstVisible = null;
        } else {
            globalY = lastGlobalY;
            current = lastFirstVisible;
        }
        while (current.segment != null) {
            final NextSegmentInfo info = current;
            final Segment segment = current.segment;
            final int localX = current.absoluteX;
            final int relativeY = current.relativeY;
            final int segmentHeight = segment.getLineHeight(localX, width);

            globalY += relativeY;

            current = segment.getNext(localX, lineHeight, width);

            final int segmentTop = globalY;
            final int segmentBottom;
            if (current.relativeY > 0) {
                if (current.absoluteX > 0) {
                    segmentBottom = segmentTop + current.relativeY + segmentHeight;
                } else {
                    segmentBottom = segmentTop + current.relativeY;
                }
            } else {
                segmentBottom = segmentTop + segmentHeight;
            }

            final boolean isVisible = segmentBottom >= 0 && segmentTop <= height;
            if (isVisible) {
                if (lastFirstVisible == null) {
                    lastGlobalY = globalY - relativeY;
                    lastFirstVisible = info;
                }

                pose.pushPose();
                pose.translate(0, globalY, 0);

                final Optional<InteractiveSegment> result = segment.render(graphics, localX, lineHeight, width, mouseX, mouseY - globalY);

                pose.popPose();

                if (isMouseOverDocument && hovered.isEmpty()) {
                    hovered = result;
                }
            }

            // We can stop rendering once we run out the bottom of the visible area.
            if (segmentTop > height) {
                break;
            }

            final boolean isFirstSegmentOnNewLine = current.relativeY > 0;
            if (isFirstSegmentOnNewLine) {
                lineHeight = current.absoluteX > 0 ? segmentHeight : 0;
            } else {
                lineHeight = Math.max(lineHeight, segmentHeight);
            }
        }

        setHoveredSegment(hovered.orElse(null));

        return hovered;
        } finally {
            graphics.disableScissor();
        }
    }

    // ----------------------------------------------------------------------- //

    @SuppressWarnings("PMD.CompareObjectsWithEquals") // Segment identity controls hover transitions.
    private void setHoveredSegment(@Nullable final InteractiveSegment hovered) {
        if (hovered == lastHovered) {
            return;
        }

        if (lastHovered != null) {
            lastHovered.setMouseHovered(false);
        }

        lastHovered = hovered;

        if (lastHovered != null) {
            lastHovered.setMouseHovered(true);
        }
    }

    // ----------------------------------------------------------------------- //

    private static Segment headerSegment(final DocumentRenderer d, final Segment s, final Matcher t) {
        return new HeaderSegment(d, s, t.group(2), t.group(1).length());
    }

    private static Segment codeSegment(final DocumentRenderer d, final Segment s, final Matcher t) {
        return new MonospaceSegment(d, s, t.group(2));
    }

    private static Segment linkSegment(final DocumentRenderer d, final Segment s, final Matcher t) {
        return new LinkSegment(d, s, t.group(1), t.group(2));
    }

    private static Segment boldSegment(final DocumentRenderer d, final Segment s, final Matcher t) {
        return new BoldSegment(d, s, t.group(2));
    }

    private static Segment italicSegment(final DocumentRenderer d, final Segment s, final Matcher t) {
        return new ItalicSegment(d, s, t.group(2));
    }

    private static Segment strikethroughSegment(final DocumentRenderer d, final Segment s, final Matcher t) {
        return new StrikethroughSegment(d, s, t.group(1));
    }

    private static Segment imageSegment(final DocumentRenderer d, final Segment s, final Matcher t) {
        return new RenderSegment(d, s, t.group(1), t.group(2));
    }

    private static final Pattern CODE_FENCE = Pattern.compile("^( {0,3})(`{3,})(.*)$");

    private static final PatternMapping[] SEGMENT_TYPES = {
        new PatternMapping("^(#+)\\s(.*)", DocumentRenderer::headerSegment), // headers: # ...
        new PatternMapping("(`)(.*?)\\1", DocumentRenderer::codeSegment), // code: `...`
        new PatternMapping("!\\[([^\\[]*)\\]\\(([^\\)]+)\\)", DocumentRenderer::imageSegment), // images: ![...](...)
        new PatternMapping("\\[([^\\[]+)\\]\\(([^\\)]+)\\)", DocumentRenderer::linkSegment), // links: [...](...)
        new PatternMapping("(\\*\\*|__)(\\S.*?\\S|$)\\1", DocumentRenderer::boldSegment), // bold: **...** | __...__
        new PatternMapping("(\\*|_)(\\S.*?\\S|$)\\1", DocumentRenderer::italicSegment), // italic: *...* | _..._
        new PatternMapping("~~(\\S.*?\\S|$)~~", DocumentRenderer::strikethroughSegment) // strikethrough: ~~...~~
    };

    private static final class PatternMapping {
        final Pattern pattern;
        final SegmentRefiner refiner;

        PatternMapping(final String pattern, final SegmentRefiner refiner) {
            this.pattern = Pattern.compile(pattern);
            this.refiner = refiner;
        }
    }
}
