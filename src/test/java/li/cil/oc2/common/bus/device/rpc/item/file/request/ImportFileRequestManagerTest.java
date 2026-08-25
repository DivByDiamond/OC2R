package li.cil.oc2.common.bus.device.rpc.item.file.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class ImportFileRequestManagerTest {
    @Test
    void sanitizeKeepsPlainNames() {
        assertEquals("hello.txt", ImportFileRequestManager.sanitizeFileName("hello.txt"));
        assertEquals("my file-2.tar.gz", ImportFileRequestManager.sanitizeFileName(" my file-2.tar.gz "));
    }

    @Test
    void sanitizeStripsPathComponents() {
        assertEquals("file.txt", ImportFileRequestManager.sanitizeFileName("../../etc/file.txt"));
        assertEquals("file.txt", ImportFileRequestManager.sanitizeFileName("..\\..\\file.txt"));
        assertEquals("file.txt", ImportFileRequestManager.sanitizeFileName("/abs/path/file.txt"));
    }

    @Test
    void sanitizeRejectsBadNames() {
        assertNull(ImportFileRequestManager.sanitizeFileName(null));
        assertNull(ImportFileRequestManager.sanitizeFileName(""));
        assertNull(ImportFileRequestManager.sanitizeFileName("   "));
        assertNull(ImportFileRequestManager.sanitizeFileName("../"));
        assertNull(ImportFileRequestManager.sanitizeFileName("bad\nname"));
        assertNull(ImportFileRequestManager.sanitizeFileName("x".repeat(256)));
        assertEquals(255, ImportFileRequestManager.sanitizeFileName("x".repeat(255)).length());
    }
}
