package li.cil.oc2.client.hooks;

import li.cil.oc2.client.gui.screen.keyboard.KeyboardScreen;
import li.cil.oc2.client.gui.screen.network.NetworkInterfaceCardScreen;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The client-side {@link ClientProxy}, delegating to the client hook classes. */
@OnlyIn(Dist.CLIENT)
public final class ClientProxyImpl implements ClientProxy {
    @Override
    public void openKeyboardScreen(final KeyboardBlockEntity keyboard) {
        Minecraft.getInstance().setScreen(new KeyboardScreen(keyboard));
    }

    @Override
    public void openNetworkInterfaceCardScreen(final Player player, final InteractionHand hand) {
        Minecraft.getInstance().setScreen(new NetworkInterfaceCardScreen(player, hand));
    }

    @Override
    public void saveExportedFile(final String name, final byte[] data) {
        FileTransferHooks.saveExportedFile(name, data);
    }

    @Override
    public void requestImportedFile(final int id) {
        FileTransferHooks.requestImportedFile(id);
    }

    @Override
    public void closeFileChooser() {
        FileTransferHooks.closeFileChooser();
    }

    @Override
    public void playTone(final BlockPos pos, final float frequency, final int durationMs) {
        SoundCardMessageHooks.playTone(pos, frequency, durationMs);
    }

    @Override
    public void streamPcm(final BlockPos pos, final byte[] pcm) {
        SoundCardMessageHooks.streamPcm(pos, pcm);
    }

    @Override
    public void addNetworkConnector(final NetworkConnectorBlockEntity entity) {
        NetworkCableRendererHooks.addNetworkConnector(entity);
    }

    @Override
    public void invalidateNetworkCableConnections() {
        NetworkCableRendererHooks.invalidateConnections();
    }

    @Override
    public void openBusInterfaceScreen(final BusCableBlockEntity blockEntity, final Direction side) {
        BusInterfaceScreenHooks.openBusInterfaceScreen(blockEntity, side);
    }

    @Override
    public void handleComputerRunStateChange(
            final BlockEntity blockEntity, final VMRunState state, final Level level, final int maxRunningSoundDelay) {
        ComputerSoundHooks.handleRunStateChange(blockEntity, state, level, maxRunningSoundDelay);
    }

    @Override
    public Object createMonitorRenderer() {
        return MonitorRendererHooks.createMonitorGUIRenderer();
    }

    @Override
    public Object emptyModelData() {
        return ModelData.EMPTY;
    }

    @Override
    public Object computeBusCableModelData(final BusCableBlockEntity owner, final Object current) {
        return BusCableModelHooks.computeModelData(owner, (ModelData) current);
    }

    @Override
    public Object computeMonitorModelData(final BlockState state) {
        return MonitorModelHooks.getModelData(state);
    }
}
