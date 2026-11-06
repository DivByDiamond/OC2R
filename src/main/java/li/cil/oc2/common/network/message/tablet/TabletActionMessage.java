package li.cil.oc2.common.network.message.tablet;

import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.common.vm.PowerAction;
import li.cil.oc2.network.StreamCodecs;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Power buttons and screen switching of a tablet menu. */
public record TabletActionMessage(UUID tabletId, Action action) implements AbstractMessage {
    /** What the player asked for. */
    public enum Action {
        START,
        SHUTDOWN,
        REBOOT,
        POWER_OFF,
        OPEN_INVENTORY,
        OPEN_TERMINAL;

        public static Action of(final PowerAction power) {
            return switch (power) {
                case START -> START;
                case SHUTDOWN -> SHUTDOWN;
                case REBOOT -> REBOOT;
                case POWER_OFF -> POWER_OFF;
            };
        }
    }

    public static final StreamCodec<FriendlyByteBuf, TabletActionMessage> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    TabletActionMessage::tabletId,
                    StreamCodecs.enumCodec(Action.class),
                    TabletActionMessage::action,
                    TabletActionMessage::new);

    public static final CustomPacketPayload.Type<TabletActionMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "tablet_action_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final MessageContext context) {
        if (!(context.player() instanceof final ServerPlayer player)) return;
        TabletManager.viewedMachine(player, tabletId)
                .ifPresent(
                        machine -> {
                            final var vm = machine.virtualMachine();
                            switch (action) {
                                case START -> vm.start();
                                case SHUTDOWN -> vm.shutdown();
                                case REBOOT -> vm.reboot();
                                case POWER_OFF -> vm.stop();
                                case OPEN_INVENTORY -> TabletManager.openInventory(player, tabletId);
                                case OPEN_TERMINAL -> TabletManager.openTerminal(player, tabletId);
                                default -> throw new AssertionError(action);
                            }
                        });
    }
}
