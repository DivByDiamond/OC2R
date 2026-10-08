package li.cil.oc2.common;

import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientEventListeners;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.common.integration.Integrations;
import li.cil.oc2.common.integration.projectred.BundledCableHandler;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.setup.CommonSetup;
import li.cil.oc2.common.setup.ModBootstrap;
import li.cil.oc2.common.setup.NativeLoader;
import li.cil.oc2.platform.NeoForgeClientProxy;
import li.cil.oc2.platform.NeoForgeClientRegistrar;
import li.cil.oc2.platform.NeoForgeMessageRegistrar;
import li.cil.oc2.platform.NeoForgeRegistryBridge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(API.MOD_ID)
public final class Main {
    // The lambda is deliberate: a method reference would load BundledCableHandler (and fail without ProjectRed).
    @SuppressWarnings("PMD.LambdaCanBeMethodReference")
    public Main(IEventBus modBus, ModContainer container) {
        ModBootstrap.initializeLibraries();

        container.registerConfig(ModConfig.Type.COMMON, CommonSpec.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientSpec.CLIENT_CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, AsyncConfig.SERVER_SPEC);

        ModBootstrap.queueRegistrations();

        Integrations.registerModIntegration("projectred_transmission", () -> BundledCableHandler.initialize());

        ModBootstrap.registerListeners();

        modBus.addListener((FMLCommonSetupEvent event) -> CommonSetup.run());
        //? if >=26.1 {
        /*if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
        *///?} else {
        if (FMLLoader.getDist() == Dist.CLIENT) {
        //?}
            Manuals.initialize();
            ClientEventListeners.register();
            ClientProxy.set(new NeoForgeClientProxy());
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