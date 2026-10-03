package li.cil.oc2.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import li.cil.oc2.api.API;
import li.cil.oc2.common.capabilities.CapabilityProviders;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.config.ConfigManager;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.setup.CommonSetup;
import li.cil.oc2.common.setup.ModBootstrap;
import li.cil.oc2.common.setup.NativeLoader;
import li.cil.oc2.platform.FabricCapabilityRegistrar;
import li.cil.oc2.platform.FabricCommonEvents;
import li.cil.oc2.platform.FabricMessageRegistrar;
import li.cil.oc2.platform.FabricRegistryBridge;
import net.fabricmc.api.ModInitializer;
import net.neoforged.fml.config.ModConfig;

/** Fabric entrypoint: runs the shared start-up sequence and then binds the Fabric bridges. */
public final class OC2RFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModBootstrap.initializeLibraries();

        NeoForgeConfigRegistry.INSTANCE.register(API.MOD_ID, ModConfig.Type.COMMON, CommonSpec.CONFIG_SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(API.MOD_ID, ModConfig.Type.CLIENT, ClientSpec.CLIENT_CONFIG_SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(API.MOD_ID, ModConfig.Type.SERVER, AsyncConfig.SERVER_SPEC);
        NeoForgeModConfigEvents.loading(API.MOD_ID)
                .register(config -> ConfigManager.handleConfigLoaded(config.getType() == ModConfig.Type.CLIENT));
        NeoForgeModConfigEvents.reloading(API.MOD_ID)
                .register(config -> ConfigManager.handleConfigLoaded(config.getType() == ModConfig.Type.CLIENT));

        ModBootstrap.queueRegistrations();
        ModBootstrap.registerListeners();
        NativeLoader.loadLibrary();

        FabricRegistryBridge.instance().bind();
        Network.initialize(new FabricMessageRegistrar());
        CapabilityProviders.registerAll(new FabricCapabilityRegistrar());

        FabricCommonEvents.register();
        FabricBlockEntityHooks.register();

        CommonSetup.run();
    }
}
