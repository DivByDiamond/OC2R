package li.cil.oc2.common.firmware;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Bounded, ordered image transfer. Bytes are published only after finish verifies the digest. */
public final class FlashImageUpload {
    public static final int MAX_IMAGE_SIZE = 64 * 1024 * 1024;
    public static final int CHUNK_SIZE = 16 * 1024;
    private final int length;
    private final String expectedDigest;
    private final MessageDigest digest;
    private final OutputStream output;
    private int offset;
    private boolean finished;

    public FlashImageUpload(final int length, final int capacity, final String expectedDigest,
                            final OutputStream output) {
        if (length <= 0 || length > capacity || length > MAX_IMAGE_SIZE
                || !expectedDigest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid_image");
        }
        this.length = length;
        this.expectedDigest = expectedDigest;
        this.output = output;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    public void append(final int position, final byte[] bytes) throws IOException {
        if (finished || position != offset || bytes.length == 0 || bytes.length > CHUNK_SIZE
                || bytes.length > length - offset) {
            throw new IllegalArgumentException("invalid_chunk");
        }
        output.write(bytes);
        digest.update(bytes);
        offset += bytes.length;
    }

    public void finish() throws IOException {
        if (finished || offset != length) throw new IllegalArgumentException("incomplete_image");
        finished = true;
        if (!expectedDigest.equals(HexFormat.of().formatHex(digest.digest()))) {
            throw new IllegalArgumentException("checksum_mismatch");
        }
        output.flush();
    }

    public int offset() {
        return offset;
    }
}
