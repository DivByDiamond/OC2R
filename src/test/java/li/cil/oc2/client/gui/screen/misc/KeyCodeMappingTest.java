package li.cil.oc2.client.gui.screen.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class KeyCodeMappingTest {
    @Test
    void mapsHostShortcutAndInternationalKeysToLinuxInputCodes() {
        assertEquals(86, KeyCodeMapping.MAPPING.get(GLFW.GLFW_KEY_WORLD_1));
        assertEquals(89, KeyCodeMapping.MAPPING.get(GLFW.GLFW_KEY_WORLD_2));
        assertEquals(99, KeyCodeMapping.MAPPING.get(GLFW.GLFW_KEY_PRINT_SCREEN));
        assertEquals(125, KeyCodeMapping.MAPPING.get(GLFW.GLFW_KEY_LEFT_SUPER));
        assertEquals(126, KeyCodeMapping.MAPPING.get(GLFW.GLFW_KEY_RIGHT_SUPER));
        assertEquals(127, KeyCodeMapping.MAPPING.get(GLFW.GLFW_KEY_MENU));
    }
}
