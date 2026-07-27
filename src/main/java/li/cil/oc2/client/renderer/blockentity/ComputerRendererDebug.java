package li.cil.oc2.client.renderer.blockentity;

import java.util.Objects;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

final class ComputerRendererDebug {
    private static final Logger LOGGER = LogManager.getLogger();
    private static long lastDiagnosticLog = 0;

    static void logDiagnostic(
            final ComputerBlockEntity computer,
            final ComputerBlockEntity terminalSource,
            final Matrix4f poseMatrix) {
        final long now = System.currentTimeMillis();
        if (now - lastDiagnosticLog <= 1000) return;
        lastDiagnosticLog = now;

        final VMRunState runState = terminalSource.getVirtualMachine().getRunState();
        final BusState busState = terminalSource.getVirtualMachine().getBusState();
        final Terminal terminal = terminalSource.terminalManager.getTerminal();
        int nonSpaceCount = 0;
        final int visibleStart =
                Math.max(0, (terminal.lastRowToDisplayMax - Terminal.HEIGHT) * Terminal.WIDTH);
        final int visibleEnd =
                Math.min(
                        terminal.buffer.length,
                        visibleStart + Terminal.WIDTH * Terminal.HEIGHT);
        for (int i = visibleStart; i < visibleEnd; i++) {
            if (terminal.buffer[i] != ' ') nonSpaceCount++;
        }
        final net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        final ClientLevel mainLevel = mc.level;
        final Level computerLevel = computer.getLevel();
        LOGGER.info(
                "[ComputerRenderer] BER called for computer at {} (virtualClone={},"
                    + " terminalSourcePos={}). runState={}, busState={}, terminal visible"
                    + " nonSpace={}, computerLevel={}, mainMcLevel={}, sameLevel={}, pos={},"
                    + " poseTranslation=({},{},{})",
                computer.getBlockPos(),
                computer.terminalManager.isContraptionVirtualClone(),
                terminalSource.getBlockPos(),
                runState,
                busState,
                nonSpaceCount,
                computerLevel != null
                        ? computerLevel.getClass().getSimpleName()
                                + "@"
                                + Integer.toHexString(System.identityHashCode(computerLevel))
                        : "null",
                mainLevel != null
                        ? mainLevel.getClass().getSimpleName()
                                + "@"
                                + Integer.toHexString(System.identityHashCode(mainLevel))
                        : "null",
                Objects.equals(computerLevel, mainLevel),
                computer.getBlockPos(),
                poseMatrix.m30(),
                poseMatrix.m31(),
                poseMatrix.m32());
    }
}
