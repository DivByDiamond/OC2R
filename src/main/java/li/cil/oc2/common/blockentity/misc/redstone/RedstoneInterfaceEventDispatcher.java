package li.cil.oc2.common.blockentity.misc.redstone;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.bus.device.rpc.IEventSink;
import li.cil.oc2.api.util.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class RedstoneInterfaceEventDispatcher {
    private final Map<IEventSink, UUID> subscribers = new ConcurrentHashMap<>();

    public void subscribe(final IEventSink sink, final UUID id) {
        subscribers.put(sink, id);
    }

    public void unsubscribe(final IEventSink sink) {
        subscribers.remove(sink);
    }

    public void neighborChanged(
            final Level level,
            final BlockPos pos,
            final BlockState blockState,
            final BlockPos fromPos) {
        final Direction direction = Side.relativeDirection(pos, fromPos);
        assert direction != null;

        final ChunkPos chunkPos = new ChunkPos(fromPos);
        final int sl =
                level.hasChunk(chunkPos.x, chunkPos.z) ? level.getSignal(fromPos, direction) : 0;

        final JsonObject msg = new JsonObject();
        msg.addProperty("event", "redstone");
        msg.addProperty("side", direction.toString());
        msg.addProperty("level", sl);

        for (final Map.Entry<IEventSink, UUID> subscriber : subscribers.entrySet()) {
            subscriber.getKey().postEvent(subscriber.getValue(), msg);
        }
    }
}