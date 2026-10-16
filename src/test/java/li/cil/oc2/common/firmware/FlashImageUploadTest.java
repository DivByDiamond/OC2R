package li.cil.oc2.common.firmware;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class FlashImageUploadTest {
    @Test
    void orderedChunksReproduceImageAndVerifyDigest() throws Exception {
        final byte[] image = new byte[FlashImageUpload.CHUNK_SIZE + 7];
        for (int index = 0; index < image.length; index++) image[index] = (byte) index;
        final ByteArrayOutputStream destination = new ByteArrayOutputStream();
        final FlashImageUpload upload = new FlashImageUpload(image.length, image.length, hash(image), destination);

        upload.append(0, Arrays.copyOf(image, FlashImageUpload.CHUNK_SIZE));
        upload.append(FlashImageUpload.CHUNK_SIZE, Arrays.copyOfRange(image, FlashImageUpload.CHUNK_SIZE, image.length));
        upload.finish();

        assertEquals(image.length, upload.offset());
        assertArrayEquals(image, destination.toByteArray());
        assertThrows(IllegalArgumentException.class, upload::finish);
    }

    @Test
    void oversizeAndInvalidDigestAreRejectedBeforeWriting() {
        final ByteArrayOutputStream destination = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class, () -> new FlashImageUpload(9, 8, "0".repeat(64), destination));
        assertThrows(IllegalArgumentException.class, () -> new FlashImageUpload(0, 8, "0".repeat(64), destination));
        assertThrows(IllegalArgumentException.class, () -> new FlashImageUpload(
                FlashImageUpload.MAX_IMAGE_SIZE + 1, Integer.MAX_VALUE, "0".repeat(64), destination));
        assertThrows(IllegalArgumentException.class, () -> new FlashImageUpload(8, 8, "not a digest", destination));
        assertEquals(0, destination.size());
    }

    @Test
    void gapsOverlapAndOversizedChunksDoNotChangeProgress() throws Exception {
        final byte[] image = new byte[8];
        final FlashImageUpload upload = new FlashImageUpload(8, 8, hash(image), new ByteArrayOutputStream());
        assertThrows(IllegalArgumentException.class, () -> upload.append(1, new byte[2]));
        assertThrows(IllegalArgumentException.class, () -> upload.append(0, new byte[FlashImageUpload.CHUNK_SIZE + 1]));
        upload.append(0, new byte[4]);
        assertThrows(IllegalArgumentException.class, () -> upload.append(0, new byte[4]));
        assertThrows(IllegalArgumentException.class, () -> upload.append(4, new byte[5]));
        assertThrows(IllegalArgumentException.class, upload::finish);
        assertEquals(4, upload.offset());
    }

    @Test
    void alteredImageFailsCompletionAndCannotBeAppendedAfterward() throws Exception {
        final FlashImageUpload upload = new FlashImageUpload(2, 2, hash(new byte[]{1, 2}), new ByteArrayOutputStream());
        upload.append(0, new byte[]{1, 3});
        assertThrows(IllegalArgumentException.class, upload::finish);
        assertThrows(IllegalArgumentException.class, () -> upload.append(2, new byte[]{4}));
    }

    private static String hash(final byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
