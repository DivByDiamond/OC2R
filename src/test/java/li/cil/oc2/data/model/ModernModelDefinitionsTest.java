package li.cil.oc2.data.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ModernModelDefinitionsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/oc2r");

    @Test
    void allRegisteredItemModelsKeepTheirExistingTexturesAndDisplayTransforms() throws Exception {
        final Map<String, JsonObject> generated = ModernModelDefinitions.create();
        try (var paths = Files.list(ASSETS.resolve("models/item"))) {
            final Set<String> existing = paths.map(path -> path.getFileName().toString())
                    .filter(name -> !name.equals("network_switch.json")).collect(Collectors.toSet());
            final Set<String> actual = generated.keySet().stream().filter(name -> name.startsWith("models/item/"))
                    .map(name -> name.substring("models/item/".length())).collect(Collectors.toSet());
            assertEquals(existing, actual, "The network switch has no registered item; every other model must be generated");
            for (final String name : existing) {
                assertEquals(normalizeModel(read("models/item/" + name)),
                        normalizeModel(generated.get("models/item/" + name).deepCopy()), name);
                assertTrue(generated.containsKey("items/" + name), "Missing modern item definition: " + name);
            }
        }
    }

    @Test
    void cableMultipartKeepsAllSixInterfacesFacadeConditionsAndRotations() throws Exception {
        final JsonObject expected = read("blockstates/bus_cable.json");
        final JsonObject core = expected.getAsJsonArray("multipart").get(0).getAsJsonObject().getAsJsonObject("apply");
        core.remove("model");
        core.addProperty("type", "oc2r:bus_cable");
        for (final var part : expected.getAsJsonArray("multipart")) normalizeRotation(part.getAsJsonObject().getAsJsonObject("apply"));
        final JsonObject actual = ModernModelDefinitions.create().get("blockstates/bus_cable.json");
        for (final var part : actual.getAsJsonArray("multipart")) normalizeRotation(part.getAsJsonObject().getAsJsonObject("apply"));
        assertEquals(expected, actual);
    }

    @Test
    void ordinaryBlockstatesPreserveEveryFacingAndConnectorMount() throws Exception {
        final Map<String, JsonObject> generated = ModernModelDefinitions.create();
        try (var files = Files.list(ASSETS.resolve("blockstates"))) {
            for (final Path file : files.toList()) {
                final String name = file.getFileName().toString();
                if (name.equals("bus_cable.json") || name.equals("monitor.json")) continue;
                final JsonObject expected = read("blockstates/" + name).getAsJsonObject("variants");
                final JsonObject actual = generated.get("blockstates/" + name).getAsJsonObject("variants");
                for (final var entry : expected.entrySet()) {
                    final String selector = entry.getKey().replace(",lit=false", "").replace(",lit=true", "");
                    assertTrue(actual.has(selector), name + ": " + selector);
                    assertEquals(normalizeRotation(entry.getValue().getAsJsonObject()),
                            normalizeRotation(actual.getAsJsonObject(selector)), name + ": " + selector);
                }
            }
        }
    }

    @Test
    void dynamicMonitorRobotAndStorageUseTheirModernModelCodecs() {
        final Map<String, JsonObject> generated = ModernModelDefinitions.create();
        assertEquals("oc2r:monitor", generated.get("blockstates/monitor.json")
                .getAsJsonObject("variants").getAsJsonObject("").get("type").getAsString());
        final JsonObject robot = generated.get("items/robot.json").getAsJsonObject("model");
        assertEquals("minecraft:special", robot.get("type").getAsString());
        assertEquals("oc2r:robot", robot.getAsJsonObject("model").get("type").getAsString());
        final JsonArray tints = generated.get("items/hard_drive_medium.json").getAsJsonObject("model").getAsJsonArray("tints");
        assertEquals(-1, tints.get(0).getAsJsonObject().get("value").getAsInt());
        assertEquals("minecraft:dye", tints.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals(0xFF5E7C16, tints.get(1).getAsJsonObject().get("default").getAsInt());
        assertEquals(0xFFB02E26, generated.get("items/hard_drive_onyxos.json").getAsJsonObject("model")
                .getAsJsonArray("tints").get(1).getAsJsonObject().get("default").getAsInt());
    }

    @Test
    void robotParticleUsesItsActualSkinAndThatSkinIsIncludedInTheBlockAtlas() throws Exception {
        final JsonObject model = ModernModelDefinitions.create().get("models/item/robot.json");
        final String particle = model.getAsJsonObject("textures").get("particle").getAsString();
        assertEquals("oc2r:entity/robot/robot", particle);
        assertEquals(particle, read("models/item/robot.json").getAsJsonObject("textures").get("particle").getAsString());
        final byte[] png = Files.readAllBytes(ASSETS.resolve("textures/entity/robot/robot.png"));
        assertArrayEquals(new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}, java.util.Arrays.copyOf(png, 8));
        final JsonArray sources = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/minecraft/atlases/blocks.json"))).getAsJsonObject().getAsJsonArray("sources");
        assertTrue(java.util.stream.StreamSupport.stream(sources.spliterator(), false)
                .map(source -> source.getAsJsonObject())
                .anyMatch(source -> "minecraft:single".equals(source.get("type").getAsString())
                        && particle.equals(source.get("resource").getAsString())),
                "An entity skin is outside the default block/item atlas directories and needs an explicit sprite source");
    }

    private static JsonObject read(final String path) throws Exception {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static JsonObject normalizeRotation(final JsonObject model) {
        for (final String axis : List.of("x", "y")) model.addProperty(axis, Math.floorMod(model.has(axis) ? model.get(axis).getAsInt() : 0, 360));
        return model;
    }

    private static JsonObject normalizeModel(final JsonObject model) {
        if (model.has("display")) {
            for (final var entry : model.getAsJsonObject("display").entrySet()) {
                final JsonObject transform = entry.getValue().getAsJsonObject();
                for (final String field : List.of("rotation", "translation", "scale")) {
                    if (!transform.has(field)) transform.add(field, JsonParser.parseString(field.equals("scale") ? "[1,1,1]" : "[0,0,0]"));
                }
            }
        }
        return model;
    }
}
