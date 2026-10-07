package li.cil.oc2.common.hooks;

import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

    /** Registers a network connector with the cable renderer. */
    default void addNetworkConnector(final NetworkConnectorBlockEntity entity) {}

    /** Makes the cable renderer rebuild its cached network connections. */
    default void invalidateNetworkCableConnections() {}

    /** Opens the bus interface screen of {@code blockEntity} for {@code side}. */
    default void openBusInterfaceScreen(final BusCableBlockEntity blockEntity, final Direction side) {}

    /** Starts or stops the looping running sound of a computer after its run state changed. */
    default void handleComputerRunStateChange(
            final BlockEntity blockEntity, final VMRunState state, final Level level, final int maxRunningSoundDelay) {}

    /** Creates the GUI renderer of a monitor; the type is opaque to common code. */
    default Object createMonitorRenderer() {
        return null;
    }

    /**
     * Render model data is loader specific and client only, so common code passes it around as an opaque
     * object. The empty value to start from.
     */
    default Object emptyModelData() {
        return null;
    }

    /** Computes the bus cable render model data from {@code current}, see {@link #emptyModelData()}. */
    default Object computeBusCableModelData(final BusCableBlockEntity owner, final Object current) {
        return current;
    }

    /** Computes the monitor render model data for {@code state}, see {@link #emptyModelData()}. */
    default Object computeMonitorModelData(final BlockState state) {
        return emptyModelData();
    }

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
