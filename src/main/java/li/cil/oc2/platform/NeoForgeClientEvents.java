package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.platform.event.ClientEvents;
import li.cil.oc2.platform.event.LevelRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Fires the loader-independent {@link ClientEvents} from NeoForge's game event bus. */
@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientEvents {
    private NeoForgeClientEvents() {}

    @SubscribeEvent
    public static void onClientTickStart(final ClientTickEvent.Pre event) {
        ClientEvents.CLIENT_TICK_START.fire(Runnable::run);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(final RenderLevelStageEvent event) {
        final LevelRenderContext.Stage stage = stageOf(event.getStage());
        if (stage == null) {
            return;
        }
        final LevelRenderContext context = new LevelRenderContext(
                stage,
                event.getPoseStack(),
                event.getCamera(),
                event.getPartialTick(),
                event.getModelViewMatrix(),
                event.getProjectionMatrix());
        ClientEvents.RENDER_LEVEL.fire(listener -> listener.accept(context));
    }

    private static LevelRenderContext.Stage stageOf(final RenderLevelStageEvent.Stage stage) {
        if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return LevelRenderContext.Stage.AFTER_CUTOUT_BLOCKS;
        }
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return LevelRenderContext.Stage.AFTER_TRANSLUCENT_BLOCKS;
        }
        if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return LevelRenderContext.Stage.AFTER_PARTICLES;
        }
        return null;
    }
}
