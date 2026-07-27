package li.cil.oc2.common.integration.jei;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.screen.AbstractMachineInventoryScreen;
import li.cil.oc2.client.gui.screen.AbstractMachineTerminalScreen;
import li.cil.oc2.common.block.ComputerBlockFactory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
@SuppressWarnings("unused")
public class ExtraGuiAreasJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "extra_gui_areas");
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        Set<ItemStack> removals = new HashSet<>();
        removals.add(ComputerBlockFactory.getPreconfiguredComputer());
        registration
                .getIngredientManager()
                .removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, removals);
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(
                AbstractMachineInventoryScreen.class,
                new AbstractMachineInventoryScreenGuiContainerHandler());
        registration.addGenericGuiContainerHandler(
                AbstractMachineTerminalScreen.class,
                new AbstractMachineTerminalScreenGuiContainerHandler());
    }

    private static final class AbstractMachineInventoryScreenGuiContainerHandler
            implements IGuiContainerHandler<AbstractMachineInventoryScreen<?>> {
        @Override
        public List<Rect2i> getGuiExtraAreas(final AbstractMachineInventoryScreen<?> screen) {
            return screen.getExtraAreas();
        }
    }

    private static final class AbstractMachineTerminalScreenGuiContainerHandler
            implements IGuiContainerHandler<AbstractMachineTerminalScreen<?>> {
        @Override
        public List<Rect2i> getGuiExtraAreas(final AbstractMachineTerminalScreen<?> screen) {
            return screen.getExtraAreas();
        }
    }
}