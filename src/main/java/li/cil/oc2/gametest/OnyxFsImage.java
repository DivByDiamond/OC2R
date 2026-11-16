/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import li.cil.sedna.api.device.BlockDevice;

/**
 * A read-only host-side reader and consistency checker for the OnyxFS v2 on-disk format (4096 byte
 * blocks, 128 byte inodes, 40 byte directory entries, ten direct blocks plus single and double
 * indirect). It exists so a test can judge a guest disk image after a simulated power cut without
 * trusting the guest kernel that did the recovering.
 */
final class OnyxFsImage {
    static final int BLOCK = 4096;
    private static final int INODE_SIZE = 128;
    private static final int DIRENT_SIZE = 40;
    private static final int DIRECT = 10;
    private static final int POINTERS = BLOCK / 4;
    private static final int MODE_TYPE_MASK = 0170000;
    private static final int MODE_DIR = 0040000;
    private static final int ONY2 = 0x32594E4F;

    /** One reachable name. */
    record Node(String path, int inode, boolean directory, long size) {
    }

    private final ByteBuffer bytes;
    private final int totalBlocks;
    private final int inodeCount;
    private final int inodeTableStart;
    private final int dataBitmapStart;
    private final int dataStart;
    private final int rootInode;
    private final int journalStart;
    private final int journalSize;
    private final Map<String, Node> tree = new TreeMap<>();
    private final List<String> anomalies = new ArrayList<>();
    private final BitSet owned = new BitSet();
    private final BitSet reached = new BitSet();
    private final int[] references;
    private boolean walked;

    OnyxFsImage(final byte[] image) {
        bytes = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN);
        if (image.length < BLOCK || bytes.getInt(0) != ONY2) {
            throw new IllegalStateException("not an OnyxFS v2 image");
        }
        totalBlocks = bytes.getInt(12);
        inodeCount = bytes.getInt(16);
        inodeTableStart = bytes.getInt(20);
        dataBitmapStart = bytes.getInt(24);
        dataStart = bytes.getInt(28);
        rootInode = bytes.getInt(32);
        journalStart = bytes.getInt(44);
        journalSize = bytes.getInt(48);
        if ((long) totalBlocks * BLOCK > image.length) {
            throw new IllegalStateException("superblock claims " + totalBlocks + " blocks, device holds "
                + image.length / BLOCK);
        }
        references = new int[inodeCount + 1];
    }

    static OnyxFsImage read(final BlockDevice device) throws IOException {
        final byte[] image = new byte[(int) device.getCapacity()];
        try (InputStream in = device.getInputStream(0)) {
            int at = 0;
            while (at < image.length) {
                final int n = in.read(image, at, image.length - at);
                if (n < 0) {
                    throw new IOException("short read at " + at + " of " + image.length);
                }
                at += n;
            }
        }
        return new OnyxFsImage(image);
    }

    /** Number of before-images the journal header currently promises (0 when committed or cleared). */
    int journalCount() {
        final int header = journalStart * BLOCK;
        return bytes.getInt(header) == 0x554A4E4F ? bytes.getInt(header + 8) : -1;
    }

    /**
     * Host-side judgement of the undo journal: whether the header passes its own checksum and whether
     * every before-image it promises is intact. Lets a test tell "the header sector was torn" from
     * "the undo images themselves are gone", which the guest distinguishes only by failing to mount.
     */
    List<String> journalProblems() {
        final List<String> problems = new ArrayList<>();
        final int header = journalStart * BLOCK;
        final int magic = bytes.getInt(header);
        if (magic != 0x554A4E4F) {
            problems.add("no ONJU header (magic 0x" + Integer.toHexString(magic) + ")");
            return problems;
        }
        final int version = bytes.getInt(header + 4);
        final int count = bytes.getInt(header + 8);
        if (version != 1) {
            problems.add("journal version " + version);
        }
        if (count < 0 || count > 31) {
            problems.add("journal count " + count + " out of range");
            return problems;
        }
        final int sum = bytes.getInt(header + 508);
        if (sum != fnv(0x811C9DC5, bytes.array(), header, 508)) {
            problems.add("header checksum mismatch");
        }
        for (int i = 0; i < count; i++) {
            final int target = bytes.getInt(header + 16 + i * 8);
            final int expected = bytes.getInt(header + 20 + i * 8);
            final int image = (journalStart + 1 + i) * BLOCK;
            if (image + BLOCK > bytes.capacity()) {
                problems.add("entry " + i + " image out of range");
                continue;
            }
            if (expected != fnv(0x811C9DC5, bytes.array(), image, BLOCK)) {
                problems.add("entry " + i + " (block " + target + ") checksum mismatch");
            }
        }
        return problems;
    }

    /** FNV-1a as the kernel computes it for the journal header and its undo images. */
    private static int fnv(final int seed, final byte[] data, final int from, final int length) {
        int hash = seed;
        for (int i = from; i < from + length; i++) {
            hash = (hash ^ (data[i] & 0xFF)) * 16777619;
        }
        return hash;
    }

    int journalStartBlock() {
        return journalStart;
    }

    int journalBlocks() {
        return journalSize;
    }

    int totalBlocks() {
        return totalBlocks;
    }

    int dataStartBlock() {
        return dataStart;
    }

    byte[] block(final int index) {
        return Arrays.copyOfRange(bytes.array(), index * BLOCK, (index + 1) * BLOCK);
    }

    /** The whole device image this reader was built from; callers must not mutate it. */
    byte[] raw() {
        return bytes.array();
    }

    /** All reachable names keyed by absolute path; the root is "/". */
    Map<String, Node> tree() {
        walk();
        return tree;
    }

    /** Structural problems found while walking, in discovery order; empty means consistent. */
    List<String> anomalies() {
        walk();
        return anomalies;
    }

    /** Content of a regular file; a hole reads as zeros. */
    byte[] content(final Node node) {
        final long size = node.size();
        final byte[] out = new byte[(int) size];
        final int blocks = (int) ((size + BLOCK - 1) / BLOCK);
        for (int logical = 0; logical < blocks; logical++) {
            final int physical = mapped(inodeOffset(node.inode()), logical);
            if (physical == 0 || physical < dataStart || physical >= totalBlocks) {
                continue;
            }
            final int length = (int) Math.min(BLOCK, size - (long) logical * BLOCK);
            System.arraycopy(bytes.array(), physical * BLOCK, out, logical * BLOCK, length);
        }
        return out;
    }

    // --------------------------------------------------------------------- //

    private void walk() {
        if (walked) {
            return;
        }
        walked = true;
        if (rootInode != 1 || inodeTableStart != 3 || dataBitmapStart != 2) {
            anomalies.add("unexpected superblock geometry root=" + rootInode + " itable=" + inodeTableStart
                + " dbitmap=" + dataBitmapStart);
        }
        final int rootOffset = inodeOffset(rootInode);
        if (!inodeInRange(rootInode) || (bytes.getInt(rootOffset) & MODE_TYPE_MASK) != MODE_DIR) {
            anomalies.add("root inode is not a directory");
            return;
        }
        tree.put("/", new Node("/", rootInode, true, 0));
        reached.set(rootInode);
        walkDirectory("/", rootInode, rootInode, 0);
        for (int ino = 1; ino <= inodeCount; ino++) {
            final boolean allocated = bit(1, ino - 1);
            if (allocated && !reached.get(ino)) {
                anomalies.add("inode " + ino + " is allocated but unreachable (leak)");
            }
        }
        final int dataBlocks = totalBlocks - dataStart;
        for (int index = 0; index < dataBlocks; index++) {
            if (bit(dataBitmapStart, index) && !owned.get(dataStart + index)) {
                anomalies.add("data block " + (dataStart + index) + " is allocated but unowned (leak)");
            }
        }
        for (final Node node : tree.values()) {
            if (!node.directory() && references[node.inode()] != nlink(node.inode())) {
                anomalies.add(node.path() + ": inode " + node.inode() + " has " + references[node.inode()]
                    + " names but nlink " + nlink(node.inode()));
            }
        }
    }

    private void walkDirectory(final String path, final int inode, final int parent, final int depth) {
        if (depth > 16) {
            anomalies.add(path + ": directory nesting too deep, loop?");
            return;
        }
        final int base = inodeOffset(inode);
        claimBlocks(path, base, true);
        boolean sawDot = false;
        boolean sawDotDot = false;
        for (int slot = 0; slot < DIRECT; slot++) {
            final int block = bytes.getInt(base + 16 + slot * 4);
            if (block == 0 || !dataBlock(block)) {
                continue;
            }
            for (int entry = 0; entry < BLOCK / DIRENT_SIZE; entry++) {
                final int at = block * BLOCK + entry * DIRENT_SIZE;
                final int child = bytes.getInt(at + 32);
                if (child == 0) {
                    continue;
                }
                final int nameLength = bytes.get(at + 37) & 0xFF;
                int length = nameLength;
                if (nameLength == 0 || nameLength > 32) {
                    length = 0;
                    while (length < 32 && bytes.get(at + length) != 0) {
                        length++;
                    }
                }
                final String name = new String(bytes.array(), at, length, StandardCharsets.ISO_8859_1);
                if (name.equals(".")) {
                    sawDot = true;
                    if (child != inode) {
                        anomalies.add(path + ": '.' points to " + child);
                    }
                    continue;
                }
                if (name.equals("..")) {
                    sawDotDot = true;
                    if (child != parent) {
                        anomalies.add(path + ": '..' points to " + child + ", expected " + parent);
                    }
                    continue;
                }
                final String childPath = (path.equals("/") ? "" : path) + "/" + name;
                if (!inodeInRange(child)) {
                    anomalies.add(childPath + ": inode " + child + " out of range");
                    continue;
                }
                if (tree.containsKey(childPath)) {
                    anomalies.add(childPath + ": name appears twice in its directory");
                    continue;
                }
                if (!bit(1, child - 1)) {
                    anomalies.add(childPath + ": inode " + child + " is referenced but free in the bitmap");
                }
                final int childBase = inodeOffset(child);
                final int mode = bytes.getInt(childBase) & MODE_TYPE_MASK;
                // Inodes the guest creates carry no type bits and its dirent types follow another
                // numbering than mkimage, so only "directory or not" is trusted.
                final boolean directory = mode == MODE_DIR;
                references[child]++;
                final long size = bytes.getLong(childBase + 8);
                tree.put(childPath, new Node(childPath, child, directory, size));
                if (directory) {
                    if (reached.get(child)) {
                        anomalies.add(childPath + ": directory inode " + child + " is reachable twice");
                        continue;
                    }
                    reached.set(child);
                    walkDirectory(childPath, child, inode, depth + 1);
                } else {
                    reached.set(child);
                    if (references[child] == 1) {
                        claimBlocks(childPath, childBase, false);
                        checkSize(childPath, childBase, size);
                    }
                }
            }
        }
        if (!sawDot || !sawDotDot) {
            anomalies.add(path + ": missing '.' or '..' entry");
        }
    }

    private void checkSize(final String path, final int inodeBase, final long size) {
        if (size > (long) Integer.MAX_VALUE) {
            anomalies.add(path + ": absurd size " + size);
            return;
        }
        final int needed = (int) ((size + BLOCK - 1) / BLOCK);
        for (int logical = 0; logical < needed; logical++) {
            if (mapped(inodeBase, logical) == 0) {
                anomalies.add(path + ": hole at logical block " + logical + " inside size " + size);
                return;
            }
        }
        if (needed < DIRECT + POINTERS && mapped(inodeBase, needed) != 0) {
            anomalies.add(path + ": block mapped beyond end of file at logical block " + needed);
        }
    }

    /** Marks every block the inode owns, flagging duplicates, bad pointers and unset bitmap bits. */
    private void claimBlocks(final String path, final int inodeBase, final boolean directory) {
        for (int slot = 0; slot < DIRECT; slot++) {
            claim(path, bytes.getInt(inodeBase + 16 + slot * 4));
        }
        final int indirect = bytes.getInt(inodeBase + 96);
        if (indirect != 0 && claim(path, indirect)) {
            for (int slot = 0; slot < POINTERS; slot++) {
                claim(path, bytes.getInt(indirect * BLOCK + slot * 4));
            }
        }
        final int dbl = bytes.getInt(inodeBase + 100);
        if (dbl != 0 && claim(path, dbl)) {
            for (int outer = 0; outer < POINTERS; outer++) {
                final int inner = bytes.getInt(dbl * BLOCK + outer * 4);
                if (inner != 0 && claim(path, inner)) {
                    for (int slot = 0; slot < POINTERS; slot++) {
                        claim(path, bytes.getInt(inner * BLOCK + slot * 4));
                    }
                }
            }
        }
    }

    private boolean claim(final String path, final int block) {
        if (block == 0) {
            return false;
        }
        if (!dataBlock(block)) {
            anomalies.add(path + ": pointer to block " + block + " outside the data area");
            return false;
        }
        if (owned.get(block)) {
            anomalies.add(path + ": block " + block + " is owned twice");
            return false;
        }
        owned.set(block);
        if (!bit(dataBitmapStart, block - dataStart)) {
            anomalies.add(path + ": block " + block + " is in use but free in the bitmap");
        }
        return true;
    }

    private int mapped(final int inodeBase, final int logical) {
        if (logical < DIRECT) {
            return bytes.getInt(inodeBase + 16 + logical * 4);
        }
        int rest = logical - DIRECT;
        if (rest < POINTERS) {
            final int indirect = bytes.getInt(inodeBase + 96);
            return dataBlock(indirect) ? bytes.getInt(indirect * BLOCK + rest * 4) : 0;
        }
        rest -= POINTERS;
        final int dbl = bytes.getInt(inodeBase + 100);
        if (!dataBlock(dbl) || rest / POINTERS >= POINTERS) {
            return 0;
        }
        final int inner = bytes.getInt(dbl * BLOCK + rest / POINTERS * 4);
        return dataBlock(inner) ? bytes.getInt(inner * BLOCK + rest % POINTERS * 4) : 0;
    }

    private boolean dataBlock(final int block) {
        return block >= dataStart && block < totalBlocks;
    }

    private boolean inodeInRange(final int inode) {
        return inode >= 1 && inode <= inodeCount;
    }

    private int inodeOffset(final int inode) {
        return inodeTableStart * BLOCK + (inode - 1) * INODE_SIZE;
    }

    private int nlink(final int inode) {
        return bytes.getInt(inodeOffset(inode) + 64);
    }

    private boolean bit(final int bitmapBlock, final int index) {
        return (bytes.get(bitmapBlock * BLOCK + index / 8) & (1 << (index % 8))) != 0;
    }
}
