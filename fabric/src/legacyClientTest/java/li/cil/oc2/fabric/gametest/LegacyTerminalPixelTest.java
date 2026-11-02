package li.cil.oc2.fabric.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.render.RendererView;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

/** Real legacy GL client: exercise GUI and retained world-buffer branches in a deterministic screen. */
public final class LegacyTerminalPixelTest implements ClientModInitializer {
    private boolean opened;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!opened && client.getOverlay() == null && client.screen != null) {
                opened = true;
                client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                client.options.guiScale().set(1);
                client.options.pauseOnLostFocus = false;
                client.getWindow().setWindowed(854, 480);
                client.resizeDisplay();
                client.setScreen(new PixelScreen());
            }
        });
    }

    private static final class PixelScreen extends Screen {
        private final Terminal terminal = new Terminal();
        private final RendererView renderer;
        private int frames;

        private PixelScreen() {
            super(Component.literal("OC2R terminal pixel acceptance"));
            final String pattern = "\033[2J\033[H\033#3DOUBLE HEIGHT\r\n\033#4DOUBLE HEIGHT\r\n"
                    + "\033#6Double width \033[1;31mred\033[0m \033[4mul\033[0m \033[9mstrike\033[0m\r\n"
                    + "\033#6\033(0lqqqqqk\033(B boxes\r\n\033#5Normal 0123456789 END"
                    + "\033[3;40H\033[2 q";
            terminal.io.putOutput(ByteBuffer.wrap(pattern.getBytes(StandardCharsets.UTF_8)));
            renderer = terminal.getRenderer();
        }

        @Override
        public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            graphics.flush();
            final PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(106, 48, 0);
            renderer.render(pose, new org.joml.Matrix4f().setOrtho(0, width, height, 0, -10f, 10f), frames >= 40);
            pose.popPose();
            frames++;
            if (frames == 30) capture("legacy-gui");
            if (frames == 70) {
                capture("legacy-world-path");
                try {
                    Files.writeString(Path.of("terminal-pixels.success"), "passed\n");
                } catch (final java.io.IOException exception) {
                    throw new AssertionError(exception);
                }
                Minecraft.getInstance().stop();
            }
        }

        private void capture(final String name) {
            final Path path = Path.of(name + ".png");
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(path);
            } catch (final java.io.IOException exception) {
                throw new AssertionError(exception);
            }
            TerminalPixelGoldens.check(path, name, 106, 48, 640, 384);
        }

        @Override
        public void removed() {
            terminal.releaseRenderer(renderer);
        }
    }
}
