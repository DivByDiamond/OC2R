package li.cil.oc2.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//? if >=26.1 {
/*import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
*///?}
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RestrictedContainer {
    //? if >=26.1 {
    /*// Recipe data is parsed before item default components are bound, so an ItemStack cannot be
    // built while decoding. The codec reads ItemStackTemplates and they are turned into
    // ItemStacks on first access of items(), when the components are available.
    public static final Codec<RestrictedContainer> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                        Codec.unboundedMap(
                                                        TagKey.codec(Registries.ITEM),
                                                        Codec.list(
                                                                ExtraCodecs.optionalEmptyMap(
                                                                        ItemStackTemplate.CODEC)))
                                                .fieldOf("items")
                                                .forGetter(RestrictedContainer::toTemplates))
                                    .apply(instance, RestrictedContainer::fromTemplates));

    private Map<TagKey<Item>, List<ItemStack>> items;
    private Map<TagKey<Item>, List<Optional<ItemStackTemplate>>> pending;
    *///?} else {
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

    private final Map<TagKey<Item>, List<ItemStack>> items;
    //?}

    public RestrictedContainer() {
        this(new ConcurrentHashMap<>());
    }

    public RestrictedContainer(final Map<TagKey<Item>, List<ItemStack>> items) {
        this.items = items;
    }

    //? if >=26.1 {
    /*private static RestrictedContainer fromTemplates(
            final Map<TagKey<Item>, List<Optional<ItemStackTemplate>>> templates) {
        final var container = new RestrictedContainer(null);
        container.pending = templates;
        return container;
    }

    private Map<TagKey<Item>, List<Optional<ItemStackTemplate>>> toTemplates() {
        final Map<TagKey<Item>, List<Optional<ItemStackTemplate>>> result = new ConcurrentHashMap<>();
        items().forEach(
                        (tag, stacks) -> {
                            final List<Optional<ItemStackTemplate>> templates = new ArrayList<>();
                            for (final ItemStack stack : stacks) {
                                templates.add(
                                        stack.isEmpty()
                                                ? Optional.empty()
                                                : Optional.of(ItemStackTemplate.fromNonEmptyStack(stack)));
                            }
                            result.put(tag, templates);
                        });
        return result;
    }

    public synchronized Map<TagKey<Item>, List<ItemStack>> items() {
        if (pending != null) {
            final Map<TagKey<Item>, List<ItemStack>> resolved = new ConcurrentHashMap<>();
            pending.forEach(
                    (tag, templates) -> {
                        final List<ItemStack> stacks = new ArrayList<>(templates.size());
                        for (final Optional<ItemStackTemplate> template : templates) {
                            stacks.add(template.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY));
                        }
                        resolved.put(tag, stacks);
                    });
            items = resolved;
            pending = null;
        }
        return items;
    }
    *///?} else {
    public Map<TagKey<Item>, List<ItemStack>> items() {
        return items;
    }
    //?}

    @Override
    public boolean equals(final Object obj) {
        return obj == this
                || (obj instanceof RestrictedContainer other && items().equals(other.items()));
    }

    @Override
    public int hashCode() {
        return items().hashCode();
    }

    @Override
    public String toString() {
        return "RestrictedContainer[items=" + items() + "]";
    }
}
