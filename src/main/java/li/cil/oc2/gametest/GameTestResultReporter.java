/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import li.cil.oc2.api.API;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Records one TSV line per finished game test so CI can convert the run into a JUnit XML
 * report (vanilla only prints per-test lines for failures). Registered via
 * {@link RegisterGameTestsEvent}, which only fires when the game test server is running,
 * so a normal game/server launch never installs it. The {@code gameTest} Gradle task fails
 * when the file is absent or empty, closing the vacuous-green window where the server
 * exits 0 without running anything.
 */
@EventBusSubscriber(modid = API.MOD_ID)
public final class GameTestResultReporter implements TestReporter {
    public static final String RESULTS_FILE_NAME = "gameTestResults.tsv";

    private final TestReporter delegate = new LogTestReporter();
    private final BufferedWriter writer;

    @SubscribeEvent
    public static void onRegisterGameTests(final RegisterGameTestsEvent event) {
        GlobalTestReporter.replaceWith(new GameTestResultReporter());
    }

    private GameTestResultReporter() {
        try {
            writer = Files.newBufferedWriter(
                Path.of(RESULTS_FILE_NAME), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to open game test results file", e);
        }
    }

    @Override
    public void onTestFailed(final GameTestInfo testInfo) {
        delegate.onTestFailed(testInfo);
        record(testInfo, "failed", describe(testInfo));
    }

    @Override
    public void onTestSuccess(final GameTestInfo testInfo) {
        delegate.onTestSuccess(testInfo);
        record(testInfo, "passed", "");
    }

    @Override
    public void finish() {
        delegate.finish();
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to close game test results file", e);
        }
    }

    private void record(final GameTestInfo testInfo, final String status, final String message) {
        final String line = String.join("\t",
            sanitize(testInfo.getTestName()), status,
            testInfo.isRequired() ? "required" : "optional", sanitize(message));
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write game test result", e);
        }
    }

    private String describe(final GameTestInfo testInfo) {
        final Throwable error = testInfo.getError();
        if (error == null) {
            return "";
        }
        final String message = error.getMessage();
        return message == null ? error.getClass().getSimpleName() : message;
    }

    private String sanitize(final String value) {
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
