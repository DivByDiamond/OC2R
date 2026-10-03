package li.cil.oc2.common;

import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientEventListeners;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.event.CommonEventListeners;
import li.cil.oc2.common.item.ItemGroup;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.oc2.common.setup.CommonSetup;
import li.cil.oc2.common.setup.NativeLoader;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.sound.SoundEvents;
import li.cil.oc2.common.vm.provider.DeviceTreeProviders;
import li.cil.oc2.platform.NeoForgeClientRegistrar;
import li.cil.oc2.platform.NeoForgeMessageRegistrar;
import li.cil.oc2.platform.NeoForgeRegistryBridge;
import li.cil.sedna.Sedna;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(API.MOD_ID)
public final class Main {
    public Main(IEventBus modBus, ModContainer container) {
        Ceres.initialize();
        Sedna.initialize();
        DeviceTreeProviders.initialize();
        Serializers.initialize();

        container.registerConfig(ModConfig.Type.COMMON, CommonSpec.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientSpec.CLIENT_CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, AsyncConfig.SERVER_SPEC);

        ItemTags.initialize();
        BlockTags.initialize();
        DataComponents.initialize();
        Blocks.initialize();
        BlockCodecs.initialize();
        Items.initialize();
        ItemGroup.initialize();
        BlockEntities.initialize();
        Entities.initialize();
        Containers.initialize();
        RecipeSerializers.initialize();
        SoundEvents.initialize();

        ProviderRegistry.initialize();

        DeviceTypes.initialize(modBus);
        BlockDeviceDataRegistry.initialize();
        FirmwareRegistry.initialize();

        CommonEventListeners.register();

        modBus.register(CommonSetup.class);
        if (FMLLoader.getDist() == Dist.CLIENT) {
            Manuals.initialize();
            ClientEventListeners.register();
            ClientSetup.register(NeoForgeClientRegistrar.instance());
        }

        NeoForgeRegistryBridge.instance().bind(modBus);
        modBus.addListener(Main::registerPayloads);

        NativeLoader.loadLibrary();
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        Network.initialize(new NeoForgeMessageRegistrar(event.registrar("1")));
    }
}