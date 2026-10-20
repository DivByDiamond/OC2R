package li.cil.oc2.common.bus.device.rpc.item.module;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.common.bus.device.rpc.item.AbstractItemRPCDevice;
import li.cil.oc2.platform.ItemStackHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class CraftingModuleDevice extends AbstractItemRPCDevice {
    private static final int GRID_SIZE = 3;
    private static final int GRID_SLOTS = GRID_SIZE * GRID_SIZE;

    private final Entity entity;
    private final Robot robot;

    public CraftingModuleDevice(
            final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity, "crafting");
        this.entity = entity;
        this.robot = robot;
    }

    @Callback(
            description =
                    "Tries to craft once from the crafting grid. Fails if the grid matches no recipe or the result does not fit into the bottom row.",
            returnValueDescription = "the number of items crafted, or `0` if crafting failed.")
    public int craft() {
        final Level level = entity.level();
        final ItemStackHandler inventory = robot.getInventory();

        final CraftingInput.Positioned positioned =
                CraftingInput.ofPositioned(GRID_SIZE, GRID_SIZE, gridItems(inventory));
        final CraftingInput input = positioned.input();
        //? if >=26.1 {
        /*final Optional<RecipeHolder<CraftingRecipe>> recipe =
                level.getServer() == null
                        ? Optional.empty()
                        : level.getServer()
                                .getRecipeManager()
                                .getRecipeFor(RecipeType.CRAFTING, input, level);
        *///?} else {
        final Optional<RecipeHolder<CraftingRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        //?}
        if (recipe.isEmpty()) {
            return 0;
        }

        //? if >=26.1 {
        /*final ItemStack result = recipe.get().value().assemble(input);
        *///?} else {
        final ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        //?}
        if (result.isEmpty() || !insertIntoOutput(inventory, result, true).isEmpty()) {
            return 0;
        }

        final NonNullList<ItemStack> remainders = recipe.get().value().getRemainingItems(input);
        result.onCraftedBySystem(level);
        final int crafted = result.getCount();
        insertIntoOutput(inventory, result, false);
        consumeIngredients(inventory, positioned, remainders);

        return crafted;
    }

    private List<ItemStack> gridItems(final ItemStackHandler inventory) {
        return IntStream.range(0, GRID_SLOTS).mapToObj(inventory::getStackInSlot).toList();
    }

    private void consumeIngredients(
            final ItemStackHandler inventory,
            final CraftingInput.Positioned positioned,
            final NonNullList<ItemStack> remainders) {
        final CraftingInput input = positioned.input();
        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                final int slot = positioned.left() + column + (positioned.top() + row) * GRID_SIZE;
                inventory.extractItem(slot, 1, false);

                final ItemStack remainder = remainders.get(column + row * input.width());
                if (remainder.isEmpty()) {
                    continue;
                }

                final ItemStack rejected =
                        insertIntoOutput(
                                inventory, inventory.insertItem(slot, remainder, false), false);
                if (!rejected.isEmpty()) {
                    //? if >=26.1 {
                    /*entity.spawnAtLocation((net.minecraft.server.level.ServerLevel) entity.level(), rejected);
                    *///?} else {
                    entity.spawnAtLocation(rejected);
                    //?}
                }
            }
        }
    }

    private ItemStack insertIntoOutput(
            final ItemStackHandler inventory, final ItemStack stack, final boolean simulate) {
        ItemStack remaining = stack;
        final int outputSlots = inventory.getSlots() - GRID_SLOTS;
        final int start = Math.max(robot.getSelectedSlot() - GRID_SLOTS, 0);
        for (int i = 0; i < outputSlots && !remaining.isEmpty(); i++) {
            remaining =
                    inventory.insertItem(
                            GRID_SLOTS + (start + i) % outputSlots, remaining, simulate);
        }

        return remaining;
    }
}
