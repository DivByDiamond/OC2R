package li.cil.oc2.common.network.message.tablet;

import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.tablet.TabletClientView;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.network.StreamCodecs;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Run, bus and boot error state of a tablet, sent to players with its screen open. */
public record TabletStateMessage(
        UUID tabletId, VMRunState runState, BusState busState, Component bootError)
        implements AbstractMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, TabletStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    TabletStateMessage::tabletId,
                    StreamCodecs.enumCodec(VMRunState.class),
                    TabletStateMessage::runState,
                    StreamCodecs.enumCodec(BusState.class),
                    TabletStateMessage::busState,
                    ComponentSerialization.STREAM_CODEC,
                    TabletStateMessage::bootError,
                    TabletStateMessage::new);

    public static final CustomPacketPayload.Type<TabletStateMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "tablet_state_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final MessageContext context) {
        TabletClientView.find(tabletId)
                .ifPresent(
                        view -> {
                            view.setBusStateClient(busState);
                            view.setRunStateClient(runState);
                            view.setBootErrorClient(bootError);
                        });
    }
}
