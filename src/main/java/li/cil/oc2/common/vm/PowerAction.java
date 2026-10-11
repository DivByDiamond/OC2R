package li.cil.oc2.common.vm;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum PowerAction {
    START(0),
    SHUTDOWN(1),
    REBOOT(2),
    POWER_OFF(3);

    public static final StreamCodec<ByteBuf, PowerAction> STREAM_CODEC =
            ByteBufCodecs.idMapper(PowerAction::byId, PowerAction::id);

    private final int id;

    PowerAction(final int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    private static PowerAction byId(final int id) {
        for (final PowerAction action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        return POWER_OFF;
    }
}
