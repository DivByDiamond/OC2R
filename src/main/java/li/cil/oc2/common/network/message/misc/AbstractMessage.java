package li.cil.oc2.common.network.message.misc;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface AbstractMessage extends CustomPacketPayload {
    void handleMessage(final IPayloadContext context);
}