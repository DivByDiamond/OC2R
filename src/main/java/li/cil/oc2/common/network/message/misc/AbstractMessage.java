package li.cil.oc2.common.network.message.misc;

import li.cil.oc2.platform.MessageContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface AbstractMessage extends CustomPacketPayload {
    void handleMessage(final MessageContext context);
}