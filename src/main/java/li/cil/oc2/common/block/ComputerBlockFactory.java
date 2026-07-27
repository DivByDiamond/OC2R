package li.cil.oc2.common.block;

import static li.cil.oc2.common.util.TranslationUtils.text;

import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tags.ItemTags;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class ComputerBlockFactory {
    private static RestrictedContainer emptyRestrictedContainer() {
        var container = new RestrictedContainer();
        container
                .items()
                .put(ItemTags.DEVICES_FLASH_MEMORY, NonNullList.withSize(1, ItemStack.EMPTY));
        container.items().put(ItemTags.DEVICES_CPU, NonNullList.withSize(1, ItemStack.EMPTY));
        container.items().put(ItemTags.DEVICES_MEMORY, NonNullList.withSize(4, ItemStack.EMPTY));
        container.items().put(ItemTags.DEVICES_CARD, NonNullList.withSize(4, ItemStack.EMPTY));
        container
                .items()
                .put(ItemTags.DEVICES_HARD_DRIVE, NonNullList.withSize(4, ItemStack.EMPTY));
        return container;
    }

    public static ItemStack getComputerWithFlash() {
        final ItemStack computer = new ItemStack(Items.COMPUTER.get());
        var container = emptyRestrictedContainer();
        container
                .items()
                .get(ItemTags.DEVICES_FLASH_MEMORY)
                .set(0, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()));
        computer.set(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER, container);
        return computer;
    }

    public static ItemStack getPreconfiguredComputer() {
        final ItemStack computer = new ItemStack(Items.COMPUTER.get());
        var container = emptyRestrictedContainer();
        container
                .items()
                .get(ItemTags.DEVICES_FLASH_MEMORY)
                .set(0, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()));
        container.items().get(ItemTags.DEVICES_CPU).set(0, new ItemStack(Items.CPU_TIER_3.get()));
        container
                .items()
                .get(ItemTags.DEVICES_MEMORY)
                .replaceAll(ignored -> new ItemStack(Items.MEMORY_LARGE.get()));
        container
                .items()
                .get(ItemTags.DEVICES_CARD)
                .set(0, new ItemStack(Items.NETWORK_INTERFACE_CARD.get()));
        container
                .items()
                .get(ItemTags.DEVICES_HARD_DRIVE)
                .set(0, new ItemStack(Items.HARD_DRIVE_LARGE.get()));
        computer.set(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER, container);
        computer.set(DataComponents.CUSTOM_NAME, text("block.{mod}.computer.preconfigured"));
        return computer;
    }
}
