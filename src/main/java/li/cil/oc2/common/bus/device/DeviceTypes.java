package li.cil.oc2.common.bus.device;

import static li.cil.oc2.common.util.text.TranslationUtils.text;

import java.util.Map;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.util.info.DeviceTypeImpl;
import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class DeviceTypes {
    private static final DeferredRegister<DeviceType> DEVICE_TYPES =
            DeferredRegister.create(DeviceType.REGISTRY, API.MOD_ID);

    // MUST be declared before the DeviceType fields below: register() is invoked by the
    // static initializers and reads this map.
    private static final Map<String, String> SLOT_ICON_CATEGORIES = Map.ofEntries(
            Map.entry("memory", "components/memory"),
            Map.entry("hard_drive", "storage/hard_drive"),
            Map.entry("flash_memory", "storage/flash_memory"),
            Map.entry("card", "cards"),
            Map.entry("robot_module", "modules"),
            Map.entry("floppy", "storage/floppy"),
            Map.entry("network_tunnel", "modules"),
            Map.entry("cpu", "components/cpu"),
            Map.entry("gpu", "components/gpu"));

    public static final DeviceType MEMORY = register(ItemTags.DEVICES_MEMORY);
    public static final DeviceType HARD_DRIVE = register(ItemTags.DEVICES_HARD_DRIVE);
    public static final DeviceType FLASH_MEMORY = register(ItemTags.DEVICES_FLASH_MEMORY);
    public static final DeviceType CARD = register(ItemTags.DEVICES_CARD);
    public static final DeviceType ROBOT_MODULE = register(ItemTags.DEVICES_ROBOT_MODULE);
    public static final DeviceType FLOPPY = register(ItemTags.DEVICES_FLOPPY);
    public static final DeviceType NETWORK_TUNNEL = register(ItemTags.DEVICES_NETWORK_TUNNEL);
    public static final DeviceType CPU = register(ItemTags.DEVICES_CPU);
    public static final DeviceType GPU = register(ItemTags.DEVICES_GPU);

    @SubscribeEvent // on the mod event bus
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(DeviceType.REGISTRY);
    }

    private static DeviceType register(final TagKey<Item> tag) {
        final String id = tag.location().getPath().replaceFirst("^devices/", "");
        final String iconPath = "item/" + SLOT_ICON_CATEGORIES.get(id) + "/" + id + "_slot";
        Supplier<DeviceType> supplier =
                () ->
                        new DeviceTypeImpl(
                                tag,
                                ResourceLocation.fromNamespaceAndPath(API.MOD_ID, iconPath),
                                text("gui.{mod}.device_type." + id));
        DEVICE_TYPES.register(id, supplier);
        return supplier.get();
    }
}