package li.cil.oc2.common.util.sound;

import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.computer.SoundCardBeepMessage;
import li.cil.oc2.common.network.message.computer.SoundCardPcmMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class SoundClientMessages {
    private SoundClientMessages() {
    }

    public static void sendBeep(
            final Level level, final BlockPos pos, final float frequency, final int durationMs) {
        if (level == null || level.isClientSide()) {
            return;
        }
        final ServerLevel serverLevel = (ServerLevel) level;
        NetworkMessages.sendToClientsTrackingChunk(
                new SoundCardBeepMessage(pos, frequency, durationMs),
                serverLevel.getChunkAt(pos));
    }

    public static void sendPcm(final Level level, final BlockPos pos, final byte[] pcm) {
        if (level == null || level.isClientSide()) {
            return;
        }
        final ServerLevel serverLevel = (ServerLevel) level;
        NetworkMessages.sendToClientsTrackingChunk(
                new SoundCardPcmMessage(pos, pcm), serverLevel.getChunkAt(pos));
    }
}