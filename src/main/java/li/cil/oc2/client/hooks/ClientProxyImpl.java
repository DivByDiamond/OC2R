package li.cil.oc2.client.hooks;

import li.cil.oc2.client.gui.screen.keyboard.KeyboardScreen;
import li.cil.oc2.client.gui.screen.network.NetworkInterfaceCardScreen;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.hooks.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
}
