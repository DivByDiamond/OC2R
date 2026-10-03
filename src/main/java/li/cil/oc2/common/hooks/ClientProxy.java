package li.cil.oc2.common.hooks;

import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Entry points from common code into client-only behaviour (screens, file choosers, sound). Common
 * code is loaded on dedicated servers and must not name client classes, so it calls this interface
 * instead; the client installs the real implementation during setup and a dedicated server keeps
 * the no-op default, which is never reached because these are only called on the client.
 */
public interface ClientProxy {
    /** Opens the on-screen keyboard for {@code keyboard}. */
    default void openKeyboardScreen(final KeyboardBlockEntity keyboard) {}

    /** Opens the network interface card configuration screen for the card in {@code hand}. */
    default void openNetworkInterfaceCardScreen(final Player player, final InteractionHand hand) {}

    /** Asks the player where to save a file exported by a computer. */
    default void saveExportedFile(final String name, final byte[] data) {}

    /** Asks the player for a file to import for request {@code id}. */
    default void requestImportedFile(final int id) {}

    /** Closes the file chooser opened for an import the server has already settled. */
    default void closeFileChooser() {}

    /** Plays a tone of a sound card at {@code pos}. */
    default void playTone(final BlockPos pos, final float frequency, final int durationMs) {}

    /** Streams PCM audio of a sound card at {@code pos}. */
    default void streamPcm(final BlockPos pos, final byte[] pcm) {}

    /** The proxy in use. */
    static ClientProxy get() {
        return Holder.proxy;
    }

    /** Installs the client implementation. */
    static void set(final ClientProxy proxy) {
        Holder.proxy = proxy;
    }

    /** Holder so the default is available without class-initialisation ordering concerns. */
    final class Holder {
        private static volatile ClientProxy proxy = new ClientProxy() {};

        private Holder() {}
    }
}
