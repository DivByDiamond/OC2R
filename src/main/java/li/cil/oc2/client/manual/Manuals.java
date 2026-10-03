package li.cil.oc2.client.manual;

import java.util.function.Supplier;
import li.cil.manual.api.ManualModel;
import li.cil.manual.api.prefab.Manual;
import li.cil.manual.api.prefab.provider.NamespaceDocumentProvider;
import li.cil.manual.api.prefab.provider.NamespacePathProvider;
import li.cil.manual.api.prefab.tab.ItemStackTab;
import li.cil.manual.api.prefab.tab.TextureTab;
import li.cil.manual.api.util.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class Manuals {
    private static final String MANUAL_REGISTRY = Constants.MANUAL_REGISTRY.location().toString();
    private static final String PATH_PROVIDERS = Constants.PATH_PROVIDER_REGISTRY.location().toString();
    private static final String CONTENT_PROVIDERS =
            Constants.DOCUMENT_PROVIDER_REGISTRY.location().toString();
    private static final String TABS = Constants.TAB_REGISTRY.location().toString();

    public static final Supplier<Manual> MANUAL = register(MANUAL_REGISTRY, "manual", Manual::new);

    public static void initialize() {
        register(PATH_PROVIDERS, "path_provider", () -> new NamespacePathProvider(API.MOD_ID));
        register(
                CONTENT_PROVIDERS, "content_provider", () -> new NamespaceDocumentProvider(API.MOD_ID, "doc"));

        register(
                TABS,
                "home",
                () ->
                        new TextureTab(
                                ManualModel.LANGUAGE_KEY + "/index.md",
                                Component.translatable("manual." + API.MOD_ID + ".home"),
                                ResourceLocation.fromNamespaceAndPath(
                                        API.MOD_ID, "textures/gui/manual/home.png")));
        register(
                TABS,
                "blocks",
                () ->
                        new ItemStackTab(
                                ManualModel.LANGUAGE_KEY + "/block/index.md",
                                Component.translatable("manual." + API.MOD_ID + ".blocks"),
                                new ItemStack(Blocks.COMPUTER.get())));
        register(
                TABS,
                "modules",
                () ->
                        new ItemStackTab(
                                ManualModel.LANGUAGE_KEY + "/item/index.md",
                                Component.translatable("manual." + API.MOD_ID + ".items"),
                                new ItemStack(Items.TRANSISTOR.get())));
    }

    private static <T> Supplier<T> register(
            final String registryId, final String name, final Supplier<? extends T> factory) {
        return Platform.registries().register(registryId, Constants.MOD_ID, name, factory);
    }
}
