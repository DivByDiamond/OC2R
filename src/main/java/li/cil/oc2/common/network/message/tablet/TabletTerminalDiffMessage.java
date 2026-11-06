package li.cil.oc2.common.network.message.tablet;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.tablet.TabletClientView;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative terminal screen diff for the tablet whose screen the player has open. */
public record TabletTerminalDiffMessage(UUID tabletId, TerminalDiff.Snapshot snapshot)
        implements AbstractMessage {
    public static final StreamCodec<ByteBuf, TabletTerminalDiffMessage> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    TabletTerminalDiffMessage::tabletId,
                    TerminalDiff.STREAM_CODEC,
                    TabletTerminalDiffMessage::snapshot,
                    TabletTerminalDiffMessage::new);

    public static final CustomPacketPayload.Type<TabletTerminalDiffMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "tablet_terminal_diff_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final MessageContext context) {
        TabletClientView.find(tabletId)
                .ifPresent(view -> TerminalDiff.apply(view.terminal(), snapshot));
    }
}
