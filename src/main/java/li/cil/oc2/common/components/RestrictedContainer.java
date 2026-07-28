package li.cil.oc2.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record RestrictedContainer(Map<TagKey<Item>, List<ItemStack>> items) {
    public static final Codec<RestrictedContainer> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                        Codec.unboundedMap(
                                                        TagKey.codec(Registries.ITEM),
                                                        Codec.list(
                                                                ItemStack.OPTIONAL_CODEC))
                                                .fieldOf("items")
                                                    .forGetter(RestrictedContainer::items))
                                    .apply(instance, RestrictedContainer::new));

    public RestrictedContainer() {
        this(new ConcurrentHashMap<>());
    }
}