package li.cil.oc2.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards against new client-only imports leaking into {@code li.cil.oc2.common}, which is loaded on
 * dedicated servers. New client access from common code must go through a class in
 * {@code li.cil.oc2.client.hooks} that is only reached from client-side code paths.
 *
 * <p>{@link #LEGACY_OFFENDERS} lists files that predate the guard. Remove an entry when the file is
 * cleaned up; never add one.
 */
class ClientImportGuardTest {
    private static final Path COMMON = Path.of("src/main/java/li/cil/oc2/common");
    private static final Pattern CLIENT_IMPORT = Pattern.compile(
            "^import (static )?(net\\.minecraft\\.client|net\\.neoforged\\.neoforge\\.client|li\\.cil\\.oc2\\.client)\\.",
            Pattern.MULTILINE);

    private static final Set<String> LEGACY_OFFENDERS = Set.of(
            "li/cil/oc2/common/blockentity/monitor/MonitorBlockEntity.java",
            "li/cil/oc2/common/blockentity/network/cable/BusCableModelData.java",
            "li/cil/oc2/common/block/keyboard/KeyboardBlock.java",
            "li/cil/oc2/common/container/computer/AbstractComputerContainer.java",
            "li/cil/oc2/common/container/Containers.java",
            "li/cil/oc2/common/container/monitor/AbstractMonitorContainer.java",
            "li/cil/oc2/common/container/robot/AbstractRobotContainer.java",
            "li/cil/oc2/common/container/robot/RobotTerminalContainer.java",
            "li/cil/oc2/common/integration/jei/ExtraGuiAreasJEIPlugin.java",
            "li/cil/oc2/common/item/network/NetworkInterfaceCardItem.java",
            "li/cil/oc2/common/item/storage/FloppyItem.java",
            "li/cil/oc2/common/item/tool/ManualItem.java",
            "li/cil/oc2/common/item/tool/RobotItem.java",
            "li/cil/oc2/common/item/tool/WrenchItem.java",
            "li/cil/oc2/common/Main.java",
            "li/cil/oc2/common/mixin/FrustumMixin.java",
            "li/cil/oc2/common/mixin/LevelRendererMixin.java",
            "li/cil/oc2/common/mixin/MinecraftMixin.java",
            "li/cil/oc2/common/network/message/computer/SoundCardBeepMessage.java",
            "li/cil/oc2/common/network/message/computer/SoundCardPcmMessage.java",
            "li/cil/oc2/common/network/message/file/cancel/ServerCanceledImportFileMessage.java",
            "li/cil/oc2/common/network/message/file/ExportedFileMessage.java",
            "li/cil/oc2/common/network/message/file/RequestImportedFileMessage.java",
            "li/cil/oc2/common/network/util/ClientBlockEntityLookup.java",
            "li/cil/oc2/common/network/util/MessageUtils.java",
            "li/cil/oc2/common/util/text/TooltipRenderer.java",
            "li/cil/oc2/common/vm/terminal/fonts/FontAtlas.java",
            "li/cil/oc2/common/vm/terminal/render/overlay/TerminalCursorRenderer.java",
            "li/cil/oc2/common/vm/terminal/render/TerminalRenderer.java",
            "li/cil/oc2/common/vm/terminal/TerminalClient.java");

    @Test
    void commonCodeHasNoNewClientImports() throws IOException {
        final Set<String> offenders = new TreeSet<>();
        try (Stream<Path> files = Files.walk(COMMON)) {
            final List<Path> sources =
                    files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
            for (final Path source : sources) {
                if (CLIENT_IMPORT.matcher(Files.readString(source)).find()) {
                    offenders.add(Path.of("").toAbsolutePath().relativize(source.toAbsolutePath())
                            .toString().replace('\\', '/').replaceFirst("^src/main/java/", ""));
                }
            }
        }
        offenders.removeAll(LEGACY_OFFENDERS);
        assertTrue(offenders.isEmpty(),
                "New client imports in common code (use li.cil.oc2.client.hooks): " + offenders);
    }

    @Test
    void legacyListHasNoStaleEntries() throws IOException {
        final Set<String> stale = new TreeSet<>();
        for (final String entry : LEGACY_OFFENDERS) {
            final Path source = Path.of("src/main/java").resolve(entry);
            if (!Files.exists(source) || !CLIENT_IMPORT.matcher(Files.readString(source)).find()) {
                stale.add(entry);
            }
        }
        assertTrue(stale.isEmpty(), "Remove cleaned-up files from LEGACY_OFFENDERS: " + stale);
    }
}
