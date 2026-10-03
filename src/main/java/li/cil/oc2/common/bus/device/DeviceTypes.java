package li.cil.oc2.common.bus.device;

import static li.cil.oc2.common.util.text.TranslationUtils.text;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.util.info.DeviceTypeImpl;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class DeviceTypes {
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

    /**
     * Makes sure {@link DeviceType#REGISTRY} exists and the entries above are queued before the loader
     * binds the registries. The registry itself is created by the {@link DeviceType} interface field
     * (public API), the entries are registered into it by id.
     */
    public static void initialize() {
        // Touching the field creates the registry through the platform bridge.
        Objects.requireNonNull(DeviceType.REGISTRY, "Device type registry missing");
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
        Platform.registries().register(DeviceType.REGISTRY_KEY.location().toString(), API.MOD_ID, id, supplier);
        return supplier.get();
    }
}