package li.cil.oc2.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public record RestrictedContainer(Map<TagKey<Item>, NonNullList<ItemStack>> items) {
    public static final Codec<RestrictedContainer> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.unboundedMap(
                                                            TagKey.codec(Registries.ITEM),
                                                            NonNullList.codecOf(
                                                                    ItemStack.OPTIONAL_CODEC))
                                                    .fieldOf("items")
                                                    .forGetter(RestrictedContainer::items))
                                    .apply(instance, RestrictedContainer::new));

    public RestrictedContainer() {
        this(new HashMap<>());
    }
}
