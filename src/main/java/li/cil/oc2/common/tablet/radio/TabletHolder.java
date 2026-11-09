package li.cil.oc2.common.tablet.radio;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The player and the live item stack that make a tablet active right now. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The holder names the live player and stack on purpose.")
public record TabletHolder(ServerPlayer player, ItemStack stack) {
    /** The level the player is in, which decides the radio dimension of the tablet. */
    public ServerLevel level() {
        return (ServerLevel) player.level();
    }
}
