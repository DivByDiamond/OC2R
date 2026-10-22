package li.cil.oc2.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Version-26 model data. Geometry authored by hand remains in models/block. */
public final class ModernModelDefinitions {
    private static final String PARENT = "parent";
    private static final List<String> HORIZONTAL_BLOCKS = List.of("charger", "computer", "disk_drive",
            "flash_memory_flasher", "keyboard", "network_hub", "network_switch", "pci_card_cage",
            "projector", "redstone_interface", "vxlan_hub");
    private static final List<String> SIMPLE = List.of("creative_energy", "internet_gateway", "speaker");
    private static final List<String> FACES = List.of("north", "east", "south", "west");
    private static final Map<String, Integer> DYE_DEFAULTS = Map.of(
            "floppy", 0xFF745C42, "floppy_modern", 0xFF745C42,
            "hard_drive_small", 0xFF9D9D97, "hard_drive_medium", 0xFF5E7C16,
            "hard_drive_large", 0xFF169C9C, "hard_drive_extra_large", 0xFFFED83D,
            "hard_drive_onyxos", 0xFFB02E26);

    private ModernModelDefinitions() {}

    /** Fresh mutable JSON trees, keyed by path below assets/oc2r. */
    @SuppressWarnings("PMD.UseConcurrentHashMap") // Local, ordered datagen output; no shared mutable state.
    public static Map<String, JsonObject> create() {
        final Map<String, JsonObject> output = new LinkedHashMap<>();
        final Map<String, JsonObject> items = itemModels();
        items.forEach((name, model) -> {
            output.put("models/item/" + name + ".json", model);
            output.put("items/" + name + ".json", itemDefinition(name));
        });
        HORIZONTAL_BLOCKS.forEach(name -> output.put("blockstates/" + name + ".json", horizontal(name)));
        SIMPLE.forEach(name -> output.put("blockstates/" + name + ".json",
                variants(single("model", "oc2r:block/" + name))));
        output.put("blockstates/monitor.json", variants(single("type", "oc2r:monitor")));
        output.put("blockstates/bus_cable.json", cable());
        output.put("blockstates/network_connector.json", connector());
        output.put("models/block/monitor.json", monitorItemGeometry());
        return Map.copyOf(output);
    }

    // Independent loops fill a declarative catalog.
    @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.NPathComplexity"})
    private static Map<String, JsonObject> itemModels() {
        final Map<String, JsonObject> result = new LinkedHashMap<>();
        flat(result, "wrench", "tools/wrench");
        flat(result, "manual", "tools/manual");
        flat(result, "network_cable", "components/materials/network_cable");
        for (final String size : List.of("small", "medium", "large", "extra_large")) {
            flat(result, "memory_" + size, "components/memory/memory_" + size);
            flat(result, "hard_drive_" + size, "storage/hard_drive/hard_drive_base",
                    "storage/hard_drive/hard_drive_tint");
        }
        for (final String tier : List.of("1", "2", "3", "4", "inf")) {
            flat(result, "cpu_tier_" + tier, "components/cpu/cpu_tier_" + tier);
        }
        for (int tier = 1; tier <= 4; tier++) flat(result, "gpu_tier_" + tier, "components/gpu/gpu_tier_" + tier);
        for (final String name : List.of("silicon", "silicon_blend", "silicon_wafer", "raw_silicon_wafer")) {
            flat(result, name, "components/silicon/" + name);
        }
        flat(result, "hard_drive_onyxos", "storage/hard_drive/hard_drive_base", "storage/hard_drive/hard_drive_tint");
        for (final String name : List.of("flash_memory_small", "flash_memory_medium", "flash_memory",
                "flash_memory_custom", "flash_memory_onyxos")) {
            flat(result, name, "storage/flash_memory/flash_memory");
        }
        for (final String name : List.of("floppy", "floppy_modern")) flat(result, name,
                "storage/floppy/floppy_base", "storage/floppy/floppy_tint");
        for (final String name : List.of("redstone_interface_card", "network_interface_card", "internet_card",
                "file_import_export_card", "sound_card", "network_tunnel_card")) flat(result, name, "cards/" + name);
        for (final String name : List.of("inventory_operations_module", "block_operations_module",
                "scanner_module", "crafting_module", "network_tunnel_module")) {
            flat(result, name, "modules/" + name);
        }
        for (final String name : List.of("transistor", "circuit_board")) flat(result, name,
                "components/materials/" + name);
        for (final String name : HORIZONTAL_BLOCKS) {
            if (!"network_switch".equals(name)) result.put(name, single(PARENT, "oc2r:block/" + name));
        }
        for (final String name : SIMPLE) result.put(name, single(PARENT, "oc2r:block/" + name));
        result.put("monitor", single(PARENT, "oc2r:block/monitor"));
        final JsonObject robot = single(PARENT, "minecraft:item/template_shulker_box");
        robot.add("textures", single("particle", "oc2r:entity/robot/robot"));
        result.put("robot", robot);
        result.put("network_connector", connectorItem());
        result.put("bus_cable", cableItem(false));
        result.put("bus_interface", cableItem(true));
        return result;
    }

    private static void flat(final Map<String, JsonObject> output, final String name, final String... layers) {
        final JsonObject model = single(PARENT, "minecraft:item/generated");
        final JsonObject textures = new JsonObject();
        for (int i = 0; i < layers.length; i++) textures.addProperty("layer" + i, "oc2r:item/" + layers[i]);
        model.add("textures", textures);
        output.put(name, model);
    }

    private static JsonObject itemDefinition(final String name) {
        final JsonObject model;
        if ("robot".equals(name)) {
            model = single("type", "minecraft:special");
            model.addProperty("base", "oc2r:item/robot");
            model.add("model", single("type", "oc2r:robot"));
        } else {
            model = single("type", "minecraft:model");
            model.addProperty("model", "oc2r:item/" + name);
            if (DYE_DEFAULTS.containsKey(name)) {
                final JsonArray tints = new JsonArray();
                final JsonObject base = single("type", "minecraft:constant");
                base.addProperty("value", -1);
                tints.add(base);
                final JsonObject dye = single("type", "minecraft:dye");
                dye.addProperty("default", DYE_DEFAULTS.get(name));
                tints.add(dye);
                model.add("tints", tints);
            }
        }
        final JsonObject definition = new JsonObject();
        definition.add("model", model);
        return definition;
    }

    private static JsonObject horizontal(final String name) {
        final JsonObject choices = new JsonObject();
        for (int index = 0; index < FACES.size(); index++) choices.add("facing=" + FACES.get(index),
                rotated(name, "keyboard".equals(name) ? 270 : 0, index * 90));
        final JsonObject state = new JsonObject();
        state.add("variants", choices);
        return state;
    }

    private static JsonObject connector() {
        final JsonObject choices = new JsonObject();
        for (final String face : List.of("floor", "wall", "ceiling")) {
            final int x = "floor".equals(face) ? 0 : "wall".equals(face) ? 90 : 180;
            for (int i = 0; i < FACES.size(); i++) {
                choices.add("face=" + face + ",facing=" + FACES.get(i),
                        rotated("network_connector", x, (i * 90 + ("ceiling".equals(face) ? 180 : 0)) % 360));
            }
        }
        final JsonObject state = new JsonObject();
        state.add("variants", choices);
        return state;
    }

    // Each multipart entry owns an independent mutable JSON tree.
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static JsonObject cable() {
        final JsonArray parts = new JsonArray();
        final JsonObject base = new JsonObject();
        base.add("apply", single("type", "oc2r:bus_cable"));
        parts.add(base);
        final List<String> directions = List.of("down", "up", "north", "south", "west", "east");
        final int[] x = {270, 90, 0, 0, 0, 0};
        final int[] y = {270, 270, 180, 0, 90, 270};
        for (int i = 0; i < directions.size(); i++) {
            for (final String connection : List.of("cable", "interface")) {
                final JsonObject when = single("connection_" + directions.get(i), connection);
                when.addProperty("has_facade", "false");
                final JsonObject part = new JsonObject();
                part.add("when", when);
                part.add("apply", rotated("cable".equals(connection) ? "cable_link" : "cable_plug", x[i], y[i]));
                parts.add(part);
            }
        }
        final JsonObject state = new JsonObject();
        state.add("multipart", parts);
        return state;
    }

    private static JsonObject rotated(final String name, final int x, final int y) {
        final JsonObject model = single("model", "oc2r:block/" + name);
        if (x != 0) model.addProperty("x", x);
        if (y != 0) model.addProperty("y", y);
        return model;
    }

    private static JsonObject variants(final JsonObject model) {
        final JsonObject choices = new JsonObject();
        choices.add("", model);
        final JsonObject state = new JsonObject();
        state.add("variants", choices);
        return state;
    }

    private static JsonObject connectorItem() {
        final JsonObject item = single(PARENT, "oc2r:block/network_connector");
        final JsonObject display = new JsonObject();
        display.add("gui", transform(30, 315, 0, 0, 2, 0, 0.75f));
        display.add("fixed", transform(270, 0, 0, 0, 0, -5, 1));
        item.add("display", display);
        return item;
    }

    private static JsonObject cableItem(final boolean plug) {
        final JsonObject item = single(PARENT, "oc2r:block/" + (plug ? "cable_plug" : "cable_straight"));
        final JsonObject display = new JsonObject();
        display.add("gui", plug ? transform(30, 315, 0, 2, 1, 0, .75f) : transform(30, 225, 0, 0, 0, 0, .75f));
        display.add("ground", transform(0, 0, 0, 0, 3, plug ? -5 : 0, .75f));
        display.add("fixed", transform(0, 180, 0, 0, 0, plug ? 4 : 0, 1));
        display.add("thirdperson_righthand", plug
                ? transform(75, 180, 0, 0, -1, 0, .75f)
                : transform(75, 45, 0, 0, 2.5f, 0, .75f));
        display.add("firstperson_righthand", plug
                ? transform(0, 180, 0, 0, 0, 2, .75f)
                : transform(0, 45, 0, 0, 0, 0, .75f));
        display.add("firstperson_lefthand", plug
                ? transform(0, 180, 0, 0, 0, 2, .75f)
                : transform(0, 225, 0, 0, 0, 0, .75f));
        item.add("display", display);
        return item;
    }

    private static JsonObject transform(final float rx, final float ry, final float rz,
                                        final float tx, final float ty, final float tz, final float scale) {
        final JsonObject result = new JsonObject();
        result.add("rotation", vector(rx, ry, rz));
        result.add("translation", vector(tx, ty, tz));
        result.add("scale", vector(scale, scale, scale));
        return result;
    }

    private static JsonArray vector(final float x, final float y, final float z) {
        final JsonArray result = new JsonArray();
        result.add(x);
        result.add(y);
        result.add(z);
        return result;
    }

    private static JsonObject monitorItemGeometry() {
        final JsonObject model = single(PARENT, "minecraft:block/cube");
        final JsonObject textures = single("particle", "oc2r:block/monitor_oc/bmm");
        for (final String face : List.of("down", "up", "north", "south", "east", "west")) {
            textures.addProperty(face, "oc2r:block/monitor_oc/"
                    + ("north".equals(face) ? "f" : "up".equals(face) || "down".equals(face) ? "b" : "b2"));
        }
        model.add("textures", textures);
        return model;
    }

    private static JsonObject single(final String key, final String value) {
        final JsonObject result = new JsonObject();
        result.addProperty(key, value);
        return result;
    }
}
