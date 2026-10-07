package li.cil.oc2.common.setup;

import li.cil.ceres.Ceres;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.event.CommonEventListeners;
import li.cil.oc2.common.item.ItemGroup;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.sound.SoundEvents;
import li.cil.oc2.common.vm.provider.DeviceTreeProviders;
import li.cil.sedna.Sedna;

/**
 * The loader-independent part of mod start-up, shared by the NeoForge {@code Main} and the Fabric
 * entrypoint. Order matters: the registry classes queue their entries on the platform bridge in the
 * order they are initialised here (blocks before items before block entities), and the loader binds
 * the queue afterwards.
 */
public final class ModBootstrap {
    private ModBootstrap() {}

    /** Initialises the emulator and serialization libraries. */
    public static void initializeLibraries() {
        Ceres.initialize();
        Sedna.initialize();
        DeviceTreeProviders.initialize();
        Serializers.initialize();
    }

    /** Queues every registry entry of the mod on the platform's registry bridge. */
    public static void queueRegistrations() {
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

        DeviceTypes.initialize();
        BlockDeviceDataRegistry.initialize();
        FirmwareRegistry.initialize();
    }

    /** Subscribes the common game event listeners. */
    public static void registerListeners() {
        CommonEventListeners.register();
    }
}
