package li.cil.oc2.fabric.gametest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;

/** Exact screen-surface comparisons: draft generation never overwrites the reviewed resources. */
final class TerminalPixelGoldens {
    private TerminalPixelGoldens() {
    }

    // 26.2 moved the F1 state from Options.hideGui into Gui.hud.
    static void hideHud(final net.minecraft.client.Minecraft client) {
        try {
            try {
                client.options.getClass().getField("hideGui").setBoolean(client.options, true);
            } catch (final NoSuchFieldException missingOldOption) {
                final Object hud = client.gui.getClass().getField("hud").get(client.gui);
                if (!(boolean) hud.getClass().getMethod("isHidden").invoke(hud)) {
                    hud.getClass().getMethod("toggle").invoke(hud);
                }
            }
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("cannot hide HUD for pixel acceptance", exception);
        }
    }

    static void check(final Path screenshot, final String name,
                      final int x, final int y, final int width, final int height) {
        try {
            final BufferedImage image = ImageIO.read(screenshot.toFile());
            if (image.getWidth() != 854 || image.getHeight() != 480) {
                throw new AssertionError("pixel acceptance requires an 854x480 client, got "
                        + image.getWidth() + "x" + image.getHeight());
            }
            final BufferedImage actual = image.getSubimage(x, y, width, height);
            final Path evidence = screenshot.resolveSibling(name + ".actual.png");
            ImageIO.write(actual, "png", evidence.toFile());
            if ("true".equals(System.getenv("OC2R_TERMINAL_PIXEL_DRAFTS"))) return;
            final String version = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow()
                    .getMetadata().getVersion().getFriendlyString();
            final String resource = "/terminal-pixels/" + version + "/" + name + ".png";
            try (var input = TerminalPixelGoldens.class.getResourceAsStream(resource)) {
                if (input == null) throw new AssertionError("missing reviewed pixel golden " + resource);
                final BufferedImage expected = ImageIO.read(input);
                if (expected.getWidth() != width || expected.getHeight() != height) {
                    throw new AssertionError("wrong golden geometry: " + resource);
                }
                int differences = 0;
                final BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                for (int row = 0; row < height; row++) {
                    for (int column = 0; column < width; column++) {
                        if ((expected.getRGB(column, row) & 0xFFFFFF) != (actual.getRGB(column, row) & 0xFFFFFF)) {
                            differences++;
                            diff.setRGB(column, row, 0xFF00FF);
                        }
                    }
                }
                if (differences != 0) {
                    final Path diffPath = screenshot.resolveSibling(name + ".diff.png");
                    ImageIO.write(diff, "png", diffPath.toFile());
                    throw new AssertionError(resource + ": " + differences + " differing pixels; " + diffPath);
                }
            }
        } catch (final IOException exception) {
            throw new AssertionError("cannot compare terminal pixels", exception);
        }
    }
}
