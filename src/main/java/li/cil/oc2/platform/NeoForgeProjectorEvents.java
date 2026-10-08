package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.projector.ProjectorDepthRenderer;
//? if <26.1 {
import net.minecraft.client.renderer.FogRenderer;
//?}
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
//? if >=26.1 {
/*import net.minecraft.util.TriState;
*///?} else {
import net.neoforged.neoforge.common.util.TriState;
//?}

/**
 * Disables fog and name tags while the projector depth pass renders the level. Fabric has no such
 * events; its counterparts are the fog and entity renderer mixins.
 */
@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeProjectorEvents {
    private NeoForgeProjectorEvents() {}

    @SubscribeEvent
    public static void handleFog(final ViewportEvent.RenderFog event) {
        if (ProjectorDepthRenderer.isRenderingProjectorDepth) {
            //? if >=26.1 {
/*            // FogRenderer.setupNoFog() is gone: push the fog planes out of range instead.
            final var fog = event.getFogData();
            fog.environmentalStart = Float.MAX_VALUE;
            fog.environmentalEnd = Float.MAX_VALUE;
            fog.renderDistanceStart = Float.MAX_VALUE;
            fog.renderDistanceEnd = Float.MAX_VALUE;
*///?} else {
            FogRenderer.setupNoFog();
            //?}
        }
    }

    @SubscribeEvent
    //? if >=26.1 {
    /*public static void handleNameplate(final RenderNameTagEvent.CanRender event) {
    *///?} else {
    public static void handleNameplate(final RenderNameTagEvent event) {
    //?}
        if (ProjectorDepthRenderer.isRenderingProjectorDepth) {
            event.setCanRender(TriState.FALSE);
        }
    }
}
