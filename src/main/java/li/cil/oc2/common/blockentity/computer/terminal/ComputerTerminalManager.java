package li.cil.oc2.common.blockentity.computer.terminal;

import java.util.*;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.computer.contraption.ComputerContraptionHandler;
import li.cil.oc2.common.bus.controller.AfterDeviceScanEvent;
import li.cil.oc2.common.container.computer.ComputerInventoryContainer;
import li.cil.oc2.common.container.computer.ComputerTerminalContainer;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ComputerTerminalManager implements TerminalUserProvider, ICaptureInputStateStorage {
    public final Terminal terminal = new Terminal();
    public volatile LevelChunk chunk;
    final Set<Player> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean captureInputState;
    private final ComputerBlockEntity computer;

    public ComputerTerminalManager(final ComputerBlockEntity computer) {
        this.computer = computer;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void start() {
        if (computer.getLevel() != null && !computer.getLevel().isClientSide()) computer.virtualMachine.start();
    }

    public void stop() {
        if (computer.getLevel() != null && !computer.getLevel().isClientSide()) computer.virtualMachine.stop();
    }

    public void openTerminalScreen(final ServerPlayer player) {
        ComputerTerminalContainer.createServer(computer, computer.energy, computer.virtualMachine.busController, player);
    }

    public void openInventoryScreen(final ServerPlayer player) {
        ComputerInventoryContainer.createServer(computer, computer.energy, computer.virtualMachine.busController, player);
    }

    public void addTerminalUser(final Player player) {
        terminalUsers.add(player);
    }

    public void removeTerminalUser(final Player player) {
        terminalUsers.remove(player);
    }

    @Override
    public Iterable<Player> getTerminalUsers() {
        return terminalUsers;
    }

    @Override
    public boolean getCaptureInputState() {
        return captureInputState;
    }

    @Override
    public void setCaptureInputState(final boolean value) {
        this.captureInputState = value;
    }

    public void handleNeighborChanged() {
        if (computer.getLevel() != null && !computer.getLevel().isClientSide()) computer.virtualMachine.busController.scheduleBusScan();
    }

    public void onAfterDeviceScan(final AfterDeviceScanEvent event) {
        if (event.didDevicesChange()) computer.getLevel().invalidateCapabilities(computer.getBlockPos());
    }

    @SuppressWarnings("unchecked")
    public <T extends Device> @Nullable T getFirstDevice(Class<T> cls) {
        for (final Device device : computer.virtualMachine.busController.getDevices())
            if (cls.isAssignableFrom(device.getClass())) return (T) device;
        return null;
    }

    public UUID getDeviceId() {
        return computer.busElement.deviceId;
    }

    public boolean isContraptionVirtualClone() {
        return ComputerContraptionHandler.isContraptionVirtualClone(computer);
    }

    @Nullable
    public ComputerBlockEntity getPrimaryForContraptionRendering() {
        return ComputerContraptionHandler.getPrimaryForContraptionRendering(computer);
    }

    public void sendToClientsTrackingComputer(final CustomPacketPayload message) {
        if (chunk != null) NetworkMessages.sendToClientsTrackingChunk(message, chunk);
    }
}
