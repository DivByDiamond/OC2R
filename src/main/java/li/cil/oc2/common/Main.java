package li.cil.oc2.common;

import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.common.block.BlockCodecs;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.ItemGroup;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.RegistryUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.vm.provider.DeviceTreeProviders;
import li.cil.sedna.Sedna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;

@Mod(API.MOD_ID)
public final class Main {
    public static boolean LoadedLibrary = false;

    public Main(IEventBus modBus, ModContainer container) {
        Ceres.initialize();
        Sedna.initialize();
        DeviceTreeProviders.initialize();
        Serializers.initialize();

        container.registerConfig(ModConfig.Type.COMMON, CommonSpec.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientSpec.CLIENT_CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, AsyncConfig.SERVER_SPEC);

        RegistryUtils.begin();

        ItemTags.initialize();
        BlockTags.initialize();
        DataComponents.initialize(modBus);
        Blocks.initialize(modBus);
        BlockCodecs.initialize(modBus);
        Items.initialize(modBus);
        BlockEntities.initialize(modBus);
        Entities.initialize(modBus);
        Containers.initialize(modBus);
        RecipeSerializers.initialize(modBus);
        SoundEvents.initialize(modBus);

        ProviderRegistry.initialize(modBus);

        BlockDeviceDataRegistry.initialize(modBus);
        FirmwareRegistry.initialize(modBus);

        RegistryUtils.finish(modBus);

        modBus.register(CommonSetup.class);
        if (FMLLoader.getDist() == Dist.CLIENT) {
            Manuals.initialize(modBus);
        }

        ItemGroup.TAB_REGISTER.register(modBus);

        NativeLoader.loadLibrary();
    }
}
