package li.cil.oc2.common.blockentity.gps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** A server-side radio station record. Positions are never returned directly to guest programs. */
public record GpsNetworkNode(UUID id, BlockPos position, UUID network, boolean reference) {
    public static final Codec<GpsNetworkNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(GpsNetworkNode::id),
            BlockPos.CODEC.fieldOf("position").forGetter(GpsNetworkNode::position),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("network").forGetter(GpsNetworkNode::network),
            Codec.BOOL.fieldOf("reference").forGetter(GpsNetworkNode::reference)
    ).apply(instance, GpsNetworkNode::new));

    public GpsNetworkNode {
        position = position.immutable();
    }
}
