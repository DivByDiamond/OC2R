package li.cil.oc2.data.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PeripheralAnimationResourcesTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/oc2r");

    @Test
    void connectedAccessPointUsesEveryDistinctSuppliedFrameInOrder() throws Exception {
        final BufferedImage strip = ImageIO.read(ROOT.resolve("textures/block/wireless_access_point/on.png").toFile());
        assertEquals(16, strip.getWidth());
        assertEquals(48, strip.getHeight(), "three full square animation frames");
        final JsonObject animation = read("textures/block/wireless_access_point/on.png.mcmeta")
                .getAsJsonObject("animation");
        assertEquals(6, animation.get("frametime").getAsInt());
        assertFalse(animation.get("interpolate").getAsBoolean(), "pixel art must not blur between frames");
        assertEquals(JsonParser.parseString("[0,1,2]"), animation.get("frames"));
        final Set<Integer> frameHashes = new HashSet<>();
        for (int frame = 0; frame < 3; frame++) {
            int hash = 1;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) hash = 31 * hash + strip.getRGB(x, y + 16 * frame);
            }
            frameHashes.add(hash);
        }
        assertEquals(3, frameHashes.size(), "all three original frames have visible differences");
        assertTrue(read("models/block/wireless_access_point_on.json").getAsJsonObject("textures")
                .asMap().values().stream().anyMatch(value -> "oc2r:block/wireless_access_point/on".equals(value.getAsString())));
        assertFalse(Files.exists(ROOT.resolve("textures/block/wireless_access_point/off.png.mcmeta")),
                "disconnected state is a static supplied off texture");
    }

    @Test
    void sensorUsesTheTwoOriginalStaticActivityStates() throws Exception {
        final BufferedImage off = ImageIO.read(ROOT.resolve("textures/block/environment_sensor/off.png").toFile());
        final BufferedImage on = ImageIO.read(ROOT.resolve("textures/block/environment_sensor/on.png").toFile());
        assertEquals(off.getWidth(), on.getWidth());
        assertEquals(off.getHeight(), on.getHeight());
        boolean different = false;
        for (int y = 0; y < off.getHeight(); y++) {
            for (int x = 0; x < off.getWidth(); x++) different |= off.getRGB(x, y) != on.getRGB(x, y);
        }
        assertTrue(different, "active sensor visibly differs from disconnected sensor");
        for (final String state : Set.of("off", "on")) {
            assertTrue(read("models/block/environment_sensor_" + state + ".json").getAsJsonObject("textures")
                    .asMap().values().stream().anyMatch(value -> value.getAsString()
                            .equals("oc2r:block/environment_sensor/" + state)));
        }
    }

    private static JsonObject read(final String name) throws Exception {
        return JsonParser.parseString(Files.readString(ROOT.resolve(name))).getAsJsonObject();
    }
}
