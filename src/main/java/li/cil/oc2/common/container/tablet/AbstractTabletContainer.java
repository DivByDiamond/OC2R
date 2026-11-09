package li.cil.oc2.common.container.tablet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.util.UUID;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.config.client.GUISpec;
import li.cil.oc2.common.container.base.AbstractMachineTerminalContainer;
import li.cil.oc2.common.container.data.IntPrecisionContainerData;
import li.cil.oc2.common.item.tablet.TabletItem;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.tablet.TabletActionMessage;
import li.cil.oc2.common.network.message.tablet.TabletInputMessage;
import li.cil.oc2.common.tablet.TabletClientView;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.common.util.GlobalInputCapture;
import li.cil.oc2.common.vm.PowerAction;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Shared behaviour of the two tablet screens; both only name the tablet by its id. */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // The field backs its same-named accessor.
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"},
        justification = "The screen shows the live view; capture state is shared by all menus like in the robot menu.")
public abstract class AbstractTabletContainer extends AbstractMachineTerminalContainer {
    private static boolean captureInputState = Config.captureInputDefaultState;

    private final UUID tabletId;
    private final Player player;
    private final TabletClientView view;

    protected AbstractTabletContainer(
            final MenuType<?> type, final int id, final Player player, final UUID tabletId) {
        super(type, id, createEnergyInfo(player));
        this.tabletId = tabletId;
        this.player = player;
        if (player instanceof final ServerPlayer serverPlayer) {
            view = null;
            TabletManager.addViewer(serverPlayer, tabletId);
        } else {
            view = TabletClientView.acquire(tabletId);
        }
    }

    public UUID tabletId() {
        return tabletId;
    }

    @Override
    public VirtualMachine getVirtualMachine() {
        return view;
    }

    @Override
    public Terminal getTerminal() {
        return view.terminal();
    }

    @Override
    public void switchToInventory() {
        NetworkMessages.sendToServer(
                new TabletActionMessage(tabletId, TabletActionMessage.Action.OPEN_INVENTORY));
    }

    @Override
    public void switchToTerminal() {
        NetworkMessages.sendToServer(
                new TabletActionMessage(tabletId, TabletActionMessage.Action.OPEN_TERMINAL));
    }

    @Override
    public void sendPowerStateToServer(final boolean value) {
        sendPowerActionToServer(value ? PowerAction.START : PowerAction.POWER_OFF);
    }

    @Override
    public void sendPowerActionToServer(final PowerAction action) {
        NetworkMessages.sendToServer(
                new TabletActionMessage(tabletId, TabletActionMessage.Action.of(action)));
    }

    @Override
    public boolean getCaptureInputState() {
        return Config.captureInputMode == GUISpec.CaptureInputMode.GLOBAL_CAPTURE
                ? GlobalInputCapture.isCaptured()
                : captureInputState;
    }

    @Override
    public void setCaptureInputState(final boolean state) {
        if (Config.captureInputMode == GUISpec.CaptureInputMode.GLOBAL_CAPTURE) {
            GlobalInputCapture.set(state);
        } else {
            captureInputState = state;
        }
    }

    @Override
    public void sendTerminalInputToServer(final ByteBuffer input) {
        NetworkMessages.sendToServer(new TabletInputMessage(tabletId, input));
    }

    @Override
    public boolean stillValid(final Player player) {
        return isHolding(player, tabletId)
                && (!(player instanceof final ServerPlayer serverPlayer)
                        || TabletManager.canUse(serverPlayer, tabletId));
    }

    /** Whether the player holds the tablet with this id in either hand. */
    public static boolean isHolding(final Player player, final UUID id) {
        for (final ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (TabletItem.getId(stack).filter(id::equals).isPresent()) return true;
        }
        return false;
    }

    @Override
    public void removed(final Player removedBy) {
        super.removed(removedBy);
        if (player instanceof final ServerPlayer serverPlayer) {
            TabletManager.removeViewer(serverPlayer, tabletId);
        } else {
            TabletClientView.release(tabletId);
        }
    }

    private static IntPrecisionContainerData createEnergyInfo(final Player player) {
        if (!(player instanceof ServerPlayer)) return createClientEnergyInfo();
        // Tablets use no energy; the screens hide the energy bar when the capacity is zero.
        return new IntPrecisionContainerData.Server() {
            @Override
            public int getInt(final int index) {
                return 0;
            }

            @Override
            public int getIntCount() {
                return ENERGY_INFO_SIZE;
            }
        };
    }
}
