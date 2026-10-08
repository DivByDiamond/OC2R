package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.platform.event.ClientEvents;
import li.cil.oc2.platform.event.LevelRenderContext;
//? if >=26.1 {
/*import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
*///?}
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

//? if >=26.1 {
/*    // 26.x split the stage event into one class per stage; the mapping follows the old stage
    // names (cutout blocks are the opaque blocks pass, particles the translucent particles pass).
    @SubscribeEvent
    public static void onAfterOpaqueBlocks(final RenderLevelStageEvent.AfterOpaqueBlocks event) {
        fire(LevelRenderContext.Stage.AFTER_CUTOUT_BLOCKS, event);
    }

    @SubscribeEvent
    public static void onAfterTranslucentBlocks(
            final RenderLevelStageEvent.AfterTranslucentBlocks event) {
        fire(LevelRenderContext.Stage.AFTER_TRANSLUCENT_BLOCKS, event);
    }

    @SubscribeEvent
    public static void onAfterTranslucentParticles(
            final RenderLevelStageEvent.AfterTranslucentParticles event) {
        fire(LevelRenderContext.Stage.AFTER_PARTICLES, event);
    }

    private static void fire(
            final LevelRenderContext.Stage stage, final RenderLevelStageEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LevelRenderContext context = new LevelRenderContext(
                stage,
                event.getPoseStack(),
                minecraft.gameRenderer.getMainCamera(),
                minecraft.getDeltaTracker(),
                new Matrix4f(event.getModelViewMatrix()),
                event.getLevelRenderState().cameraRenderState.projectionMatrix);
        ClientEvents.RENDER_LEVEL.fire(listener -> listener.accept(context));
    }
*///?} else {
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
//?}
}
