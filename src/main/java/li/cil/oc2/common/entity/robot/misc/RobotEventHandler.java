package li.cil.oc2.common.entity.robot.misc;

import java.util.Objects;
import java.util.function.Consumer;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class RobotEventHandler {
    private final Robot robot;
    private final AbstractVirtualMachine virtualMachine;

    private final CommonEvents.ChunkListener chunkUnloadListener = this::handleChunkUnload;
    private final Consumer<LevelAccessor> worldUnloadListener = this::handleWorldUnload;

    public RobotEventHandler(final Robot robot, final AbstractVirtualMachine virtualMachine) {
        this.robot = robot;
        this.virtualMachine = virtualMachine;
    }

    public void register() {
        CommonEvents.CHUNK_UNLOAD.register(chunkUnloadListener);
        CommonEvents.LEVEL_UNLOAD.register(worldUnloadListener);
    }

    public void unregister() {
        CommonEvents.CHUNK_UNLOAD.unregister(chunkUnloadListener);
        CommonEvents.LEVEL_UNLOAD.unregister(worldUnloadListener);
    }

    private void handleChunkUnload(final LevelAccessor level, final ChunkAccess chunk) {
        if (!level.equals(robot.level())) {
            return;
        }

        final ChunkPos chunkPos = new ChunkPos(robot.blockPosition());
        if (!Objects.equals(chunkPos, chunk.getPos())) {
            return;
        }

        unregister();
        virtualMachine.suspend();
        virtualMachine.dispose();
    }

    private void handleWorldUnload(final LevelAccessor level) {
        if (!level.equals(robot.level())) {
            return;
        }

        unregister();
        virtualMachine.suspend();
        virtualMachine.dispose();
    }
}
