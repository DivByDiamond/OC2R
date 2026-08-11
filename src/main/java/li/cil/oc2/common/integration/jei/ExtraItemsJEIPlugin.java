package li.cil.oc2.common.integration.jei;

import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import li.cil.oc2.api.API;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.block.AbstractBlockDeviceItem;
import li.cil.oc2.common.util.item.ItemStackUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
@SuppressWarnings({"unused", "removal"})
public class ExtraItemsJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "extra_items");
    }

    @Override
    public void registerItemSubtypes(final ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(
                VanillaTypes.ITEM_STACK, Items.COMPUTER.get(), new ComputerSubtypeInterpreter());
        registration.registerSubtypeInterpreter(
                VanillaTypes.ITEM_STACK, Items.ROBOT.get(), new RobotSubtypeInterpreter());
        registration.registerSubtypeInterpreter(
                VanillaTypes.ITEM_STACK,
                Items.FLASH_MEMORY_CUSTOM.get(),
                new BlockDeviceSubtypeInterpreter());
    }

    private static final class ComputerSubtypeInterpreter
            implements IIngredientSubtypeInterpreter<ItemStack> {
        @Override
        public String apply(final ItemStack ingredient, final UidContext context) {
            var container =
                    ingredient.get(
                            li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER);
            return container == null ? NONE : stableRestrictedContainerToString(container);
        }
    }

    private static final class RobotSubtypeInterpreter
            implements IIngredientSubtypeInterpreter<ItemStack> {
        @Override
        public String apply(final ItemStack ingredient, final UidContext context) {
            var container =
                    ingredient.get(
                            li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER);
            return container == null ? NONE : stableRestrictedContainerToString(container);
        }
    }

    private static final class BlockDeviceSubtypeInterpreter
            implements IIngredientSubtypeInterpreter<ItemStack> {
        @Override
        public String apply(final ItemStack ingredient, final UidContext context) {
            final String registryName =
                    ItemStackUtils.getModDataTag(ingredient)
                            .getString(AbstractBlockDeviceItem.DATA_TAG_NAME);
            return Strings.isNullOrEmpty(registryName) ? NONE : registryName;
        }
    }

    private static String stableRestrictedContainerToString(final RestrictedContainer container) {
        final StringBuilder stringBuilder = new StringBuilder();
        stableRestrictedContainerToString(container, stringBuilder);
        return stringBuilder.toString();
    }

    private static void stableRestrictedContainerToString(
            final RestrictedContainer container, final StringBuilder stringBuilder) {
        stringBuilder.append('{');
        container.items().keySet().stream()
                .sorted(Comparator.comparing(TagKey::location))
                .forEach(
                        key -> {
                            var values = container.items().get(key);
                            if (values.isEmpty()) return;
                            stringBuilder.append(key).append(':');
                            stableItemStackListToString(values, stringBuilder);
                            stringBuilder.append(',');
                        });
        if (stringBuilder.length() > 1) {
            stringBuilder.setLength(stringBuilder.length() - 1); // remove last comma
        }
        stringBuilder.append('}');
    }

    private static void stableItemStackListToString(
            final List<ItemStack> items, final StringBuilder stringBuilder) {
        stringBuilder.append('[');
        for (final ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            stringBuilder.append(stack.getCount()).append("x ").append(stack.getDisplayName()).append(',');
        }
        if (stringBuilder.length() > 1) {
            stringBuilder.setLength(stringBuilder.length() - 1); // remove last comma
        }
        stringBuilder.append(']');
    }
}