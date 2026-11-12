package li.cil.oc2.common.setup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import li.cil.oc2.common.inet.DefaultSessionLayer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The bundled native libraries export their entry point under the class name {@code li.cil.oc2.common.inet.
 * DefaultSessionLayer}. When the Java side declared the method elsewhere the symbol was never found, the
 * loader fell back silently, and ping never used the native path on any platform.
 */
class NativeIcmpBindingTest {
    @AfterAll
    static void removeTheExtractedLibrary() throws IOException {
        // The loader writes the library into the working directory; do not leave a binary in the project.
        Files.deleteIfExists(Path.of(System.getProperty("user.dir"), System.mapLibraryName("oc2rnet")));
    }

    @Test
    @EnabledOnOs(value = OS.LINUX, architectures = {"amd64", "x86_64", "aarch64"})
    void theEntryPointOfTheBundledLibraryIsFound() {
        NativeLoader.loadLibrary();
        // Without the binding this is an UnsatisfiedLinkError; with it the call answers or reports no reply.
        assertDoesNotThrow(() -> DefaultSessionLayer.sendICMP(new byte[] {127, 0, 0, 1}, new byte[8], 8, 200));
    }
}
