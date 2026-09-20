package li.cil.oc2.common.vm.terminal.fonts;

import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled Monocraft builds must carry the glyphs the terminal's charset mapping can
 * produce but that have no procedural fallback — today that is the DEC Special Graphics
 * medium shade (U+2592), which lives in the font via the patched-Monocraft build (upstream
 * HEAD 4.2 + the shade matrix; see todo.md §44.5). A font swap that silently drops it
 * regresses the render to .notdef tofu while every buffer-level test stays green (they
 * assert codepoints, not ink) — this test is the guard for exactly that blind spot.
 *
 * <p>Pure AWT against the packaged TTFs when the runtime can parse them; falls back to a
 * hand-rolled cmap scanner so the check stays environment-independent (the pocketprobe-kube
 * self-hosted runner historically threw {@code IOException: Problem reading font data} from
 * {@code Font.createFont} even though the TTF is valid — see CI runs 35491074912 /
 * 35513644180). No Minecraft classes, headless-safe.
 */
@SuppressWarnings("PMD")
public class MonocraftFontCoverageTest {
    private static final String[] FONT_FILES = {
            "/assets/oc2r/fonts/monocraft-r.ttf",
            "/assets/oc2r/fonts/monocraft-b.ttf",
            "/assets/oc2r/fonts/monocraft-i.ttf",
            "/assets/oc2r/fonts/monocraft-bi.ttf",
    };

    private static final int SHADE_CODEPOINT = 0x2592;

    @Test
    void bundledFontsCarryDecSpecialGraphicsShade() throws Exception {
        for (final String path : FONT_FILES) {
            final byte[] bytes;
            try (InputStream is = MonocraftFontCoverageTest.class.getResourceAsStream(path)) {
                assertTrue(is != null, "font resource missing: " + path);
                bytes = readAll(is);
                assertTrue(bytes.length > 100_000,
                        path + " looks truncated: " + bytes.length + " bytes");
                assertTrue(bytes.length < 2_000_000, path + " implausibly large: " + bytes.length);
                // TTF magic: 0x00010000 or 'OTTO' for CFF; Monocraft is TrueType.
                assertTrue(bytes[0] == 0x00 && bytes[1] == 0x01 && bytes[2] == 0x00 && bytes[3] == 0x00,
                        path + " bad TTF header: " + String.format("%02X %02X %02X %02X", bytes[0], bytes[1], bytes[2], bytes[3]));
            }

            // Preferred path: AWT canDisplay — exercises the same rasterizer the game will use.
            boolean awtChecked = false;
            try (InputStream is2 = MonocraftFontCoverageTest.class.getResourceAsStream(path)) {
                assertTrue(is2 != null, "font resource missing on second open: " + path);
                final Font font = Font.createFont(Font.TRUETYPE_FONT, is2);
                assertTrue(font.canDisplay(SHADE_CODEPOINT),
                        path + " (" + font.getFontName() + ") must map U+2592 (DEC medium shade)");
                awtChecked = true;
            } catch (IOException e) {
                // Fall through to cmap fallback — log for diagnostics but don't fail yet.
                System.err.println("[MonocraftFontCoverageTest] Font.createFont failed for " + path // NOPMD
                        + " (" + e.getMessage() + "); falling back to cmap scan"); // NOPMD
            }

            if (!awtChecked) {
                assertTrue(cmapContains(bytes, SHADE_CODEPOINT),
                        path + " cmap must map U+2592 (DEC medium shade) — "
                                + "AWT unavailable, verified via raw cmap scan");
            }
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int count;
        while (true) {
            count = is.read(buf);
            if (count == -1) break;
            out.write(buf, 0, count);
        }
        return out.toByteArray();
    }

    /**
     * Minimal TTF cmap scanner: looks for any subtable that maps {@code codepoint} to a
     * non-zero glyph id. Handles cmap formats 4 (BMP) and 12 (32-bit) which cover Monocraft.
     * Returns false if the file is not a plausible TTF or the cmap is malformed.
     */
    public static boolean cmapContains(byte[] data, int codepoint) {
        try {
            final ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
            if (bb.remaining() < 12) return false;
            bb.getInt(); // sfnt version
            final int numTables = bb.getShort() & 0xFFFF;
            bb.getShort(); // searchRange
            bb.getShort(); // entrySelector
            bb.getShort(); // rangeShift

            int cmapOffset = -1;
            int cmapLength = -1;
            for (int i = 0; i < numTables; i++) {
                if (bb.remaining() < 16) return false;
                final int tag = bb.getInt();
                bb.getInt(); // checkSum
                final int offset = bb.getInt();
                final int length = bb.getInt();
                if (tag == 0x636D6170) { // 'cmap'
                    cmapOffset = offset;
                    cmapLength = length;
                }
            }
            if (cmapOffset < 0 || cmapOffset + cmapLength > data.length) return false;

            bb.position(cmapOffset);
            if (bb.remaining() < 4) return false;
            bb.getShort(); // version
            final int numSubtables = bb.getShort() & 0xFFFF;
            if (numSubtables <= 0 || numSubtables > 32) return false;

            final int[] subOffsets = new int[numSubtables];
            for (int i = 0; i < numSubtables; i++) {
                if (bb.remaining() < 8) return false;
                bb.getShort(); // platformID
                bb.getShort(); // encodingID
                subOffsets[i] = bb.getInt();
            }

            for (int subOff : subOffsets) {
                final int absOff = cmapOffset + subOff;
                if (absOff < 0 || absOff + 4 > data.length) continue;
                bb.position(absOff);
                final int format = bb.getShort() & 0xFFFF;
                if (format == 4 && cmapFormat4Contains(bb, absOff, data.length, codepoint)) return true;
                if (format == 12 && cmapFormat12Contains(bb, absOff, data.length, codepoint)) return true;
                // Other formats (0, 6, 10) not expected for Monocraft; ignore.
            }
            return false;
        } catch (Exception e) {
            System.err.println("[MonocraftFontCoverageTest] cmap scan failed: " + e); // NOPMD
            return false;
        }
    }

    private static boolean cmapFormat4Contains(ByteBuffer bb, int tableStart, int fileLen, int codepoint) {
        // format 4 header: format(2) length(2) language(2) segCountX2(2) searchRange(2) entrySelector(2) rangeShift(2)
        if (tableStart + 14 > fileLen) return false;
        bb.position(tableStart);
        bb.getShort(); // format
        final int length = bb.getShort() & 0xFFFF;
        if (tableStart + length > fileLen) return false;
        bb.getShort(); // language
        final int segCountX2 = bb.getShort() & 0xFFFF;
        final int segCount = segCountX2 / 2;
        if (segCount <= 0 || segCount > 256) return false;
        bb.getShort(); // searchRange
        bb.getShort(); // entrySelector
        bb.getShort(); // rangeShift

        final int[] endCode = new int[segCount];
        for (int i = 0; i < segCount; i++) endCode[i] = bb.getShort() & 0xFFFF;
        bb.getShort(); // reservedPad
        final int[] startCode = new int[segCount];
        for (int i = 0; i < segCount; i++) startCode[i] = bb.getShort() & 0xFFFF;
        final int[] idDelta = new int[segCount];
        for (int i = 0; i < segCount; i++) idDelta[i] = bb.getShort(); // signed
        final int idRangeOffsetPos = bb.position();
        final int[] idRangeOffset = new int[segCount];
        for (int i = 0; i < segCount; i++) idRangeOffset[i] = bb.getShort() & 0xFFFF;

        for (int i = 0; i < segCount; i++) {
            if (codepoint < startCode[i] || codepoint > endCode[i]) continue;
            if (idRangeOffset[i] == 0) {
                final int glyphId = (codepoint + idDelta[i]) & 0xFFFF;
                return glyphId != 0;
            } else {
                // idRangeOffset is byte offset from its own position to the glyphIdArray entry.
                final int offset = idRangeOffsetPos + i * 2 + idRangeOffset[i] + (codepoint - startCode[i]) * 2;
                if (offset + 1 >= tableStart + length) return false;
                final int glyphId = ((dataAt(bb, offset) & 0xFF) << 8) | (dataAt(bb, offset + 1) & 0xFF);
                final int resolved = (glyphId == 0) ? 0 : (glyphId + idDelta[i]) & 0xFFFF;
                return resolved != 0;
            }
        }
        return false;
    }

    private static boolean cmapFormat12Contains(ByteBuffer bb, int tableStart, int fileLen, int codepoint) {
        // format 12 header: format(2) reserved(2) length(4) language(4) numGroups(4) then groups(start,end,glyphID)
        if (tableStart + 16 > fileLen) return false;
        bb.position(tableStart);
        bb.getShort(); // format
        bb.getShort(); // reserved
        final int length = bb.getInt();
        if (tableStart + length > fileLen) return false;
        bb.getInt(); // language
        final int numGroups = bb.getInt();
        if (numGroups < 0 || numGroups > 100000) return false;
        for (int i = 0; i < numGroups; i++) {
            if (bb.remaining() < 12) return false;
            final int start = bb.getInt();
            final int end = bb.getInt();
            final int glyphStart = bb.getInt();
            if (codepoint >= start && codepoint <= end) {
                final int glyphId = glyphStart + codepoint - start;
                return glyphId != 0;
            }
        }
        return false;
    }

    private static byte dataAt(ByteBuffer bb, int offset) {
        return bb.get(offset);
    }
}
