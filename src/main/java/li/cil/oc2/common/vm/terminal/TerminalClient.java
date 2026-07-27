package li.cil.oc2.common.vm.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

class TerminalClient {
    private final Terminal terminal;

    TerminalClient(final Terminal terminal) {
        this.terminal = terminal;
    }

    @OnlyIn(Dist.CLIENT)
    public RendererView getRenderer() {
        final TerminalRenderer renderer = new TerminalRenderer(terminal);
        terminal.renderers.add(renderer);
        return renderer;
    }

    @OnlyIn(Dist.CLIENT)
    public void setDisplayOnly(final boolean value) {
        terminal.displayOnly = value;
    }

    @OnlyIn(Dist.CLIENT)
    public void releaseRenderer(final RendererView renderer) {
        if (renderer instanceof final RendererModel rendererModel) {
            rendererModel.close();
            terminal.renderers.remove(rendererModel);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        if (terminal.hasPendingBell) {
            terminal.hasPendingBell = false;
            final Minecraft client = Minecraft.getInstance();
            client.execute(
                    () ->
                            client.getSoundManager()
                                    .play(
                                            SimpleSoundInstance.forUI(
                                                    NoteBlockInstrument.PLING.getSoundEvent(), 1)));
        }
    }
}
