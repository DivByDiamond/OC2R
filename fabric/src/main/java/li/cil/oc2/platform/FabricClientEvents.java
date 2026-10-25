package li.cil.oc2.platform;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.RenderCompat;
*///?}
import li.cil.oc2.platform.event.ClientEvents;
import li.cil.oc2.platform.event.CommonEvents;
import li.cil.oc2.platform.event.LevelRenderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//? if <26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//?}
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.level.AbstractLevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
*///?}
import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import org.joml.Matrix4f;
*///?}

/**
 * Fires {@link ClientEvents}, and the client halves of {@link CommonEvents} (client level tick,
 * chunk and level unload), from Fabric API client callbacks. Only loaded on the physical client.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientEvents {
    private FabricClientEvents() {}

    /** Registers the client callbacks and renderers for this loader. */
    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client ->
                ClientEvents.CLIENT_TICK_START.fire(Runnable::run));
        //? if >=26.1 {
/*        ClientTickEvents.START_LEVEL_TICK.register(level ->
                CommonEvents.LEVEL_TICK_START.fire(listener -> listener.accept(level)));
*///?} else {
        ClientTickEvents.START_WORLD_TICK.register(level ->
                CommonEvents.LEVEL_TICK_START.fire(listener -> listener.accept(level)));
        //?}
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                CommonEvents.CHUNK_LOAD.fire(listener -> listener.onChunk(level, chunk)));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                CommonEvents.CHUNK_UNLOAD.fire(listener -> listener.onChunk(level, chunk)));
        //? if >=26.1 {
/*        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(
                context -> fireRender(LevelRenderContext.Stage.AFTER_CUTOUT_BLOCKS, context));
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                context -> fireRender(LevelRenderContext.Stage.AFTER_TRANSLUCENT_BLOCKS, context));
        LevelRenderEvents.END_MAIN.register(
                context -> fireRender(LevelRenderContext.Stage.AFTER_PARTICLES, context));
*///?} else {
        // Closest Fabric equivalents of the NeoForge level render stages.
        WorldRenderEvents.AFTER_ENTITIES.register(
                context -> fireRender(LevelRenderContext.Stage.AFTER_CUTOUT_BLOCKS, context));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(
                context -> fireRender(LevelRenderContext.Stage.AFTER_TRANSLUCENT_BLOCKS, context));
        WorldRenderEvents.LAST.register(
                context -> fireRender(LevelRenderContext.Stage.AFTER_PARTICLES, context));
        //?}
        //? if >=26.2 {
/*        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            final Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                final var submit = new li.cil.oc2.platform.event.LevelGeometrySubmitContext(
                        context.submitNodeCollector(), context.poseStack(), client.level,
                        context.levelState().cameraRenderState.pos);
                ClientEvents.SUBMIT_LEVEL_GEOMETRY.fire(listener -> listener.accept(submit));
            }
        });
*///?}
        // Fabric has no per-level unload callback on the client; leaving the world is the point
        // where the client level goes away.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (Minecraft.getInstance().level != null) {
                CommonEvents.LEVEL_UNLOAD.fire(
                        listener -> listener.accept(Minecraft.getInstance().level));
            }
        });
    }

    //? if >=26.1 {
/*    private static void fireRender(final LevelRenderContext.Stage stage, final AbstractLevelRenderContext context) {
        final Minecraft client = Minecraft.getInstance();
        final var camera = context.levelState().cameraRenderState;
        final LevelRenderContext render = new LevelRenderContext(
                stage, new PoseStack(), RenderCompat.mainCamera(), client.getDeltaTracker(),
                new Matrix4f(camera.viewRotationMatrix), new Matrix4f(camera.projectionMatrix));
        ClientEvents.RENDER_LEVEL.fire(listener -> listener.accept(render));
    }
*///?} else {
    private static void fireRender(final LevelRenderContext.Stage stage, final WorldRenderContext context) {
        final LevelRenderContext render = new LevelRenderContext(
                stage,
                context.matrixStack(),
                context.camera(),
                context.tickCounter(),
                context.positionMatrix(),
                context.projectionMatrix());
        ClientEvents.RENDER_LEVEL.fire(listener -> listener.accept(render));
    }
    //?}
}
