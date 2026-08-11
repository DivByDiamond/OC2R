package li.cil.oc2.common.entity.robot.misc;

import java.util.Objects;
import java.util.function.Consumer;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class RobotEventHandler {
    private final Robot robot;
    private final AbstractVirtualMachine virtualMachine;

    private final Consumer<ChunkEvent.Unload> chunkUnloadListener = this::handleChunkUnload;
    private final Consumer<LevelEvent.Unload> worldUnloadListener = this::handleWorldUnload;

    public RobotEventHandler(final Robot robot, final AbstractVirtualMachine virtualMachine) {
        this.robot = robot;
        this.virtualMachine = virtualMachine;
    }

    public void register() {
        NeoForge.EVENT_BUS.addListener(chunkUnloadListener);
        NeoForge.EVENT_BUS.addListener(worldUnloadListener);
    }

    public void unregister() {
        NeoForge.EVENT_BUS.unregister(chunkUnloadListener);
        NeoForge.EVENT_BUS.unregister(worldUnloadListener);
    }

    private void handleChunkUnload(final ChunkEvent.Unload event) {
        if (!event.getLevel().equals(robot.level())) {
            return;
        }

        final ChunkPos chunkPos = new ChunkPos(robot.blockPosition());
        if (!Objects.equals(chunkPos, event.getChunk().getPos())) {
            return;
        }

        unregister();
        virtualMachine.suspend();
        virtualMachine.dispose();
    }

    private void handleWorldUnload(final LevelEvent.Unload event) {
        if (!event.getLevel().equals(robot.level())) {
            return;
        }

        unregister();
        virtualMachine.suspend();
        virtualMachine.dispose();
    }
}