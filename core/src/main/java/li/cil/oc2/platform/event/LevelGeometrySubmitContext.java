package li.cil.oc2.platform.event;

import com.mojang.blaze3d.vertex.PoseStack;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Context for submitting custom geometry to the level render's submit pipeline (26.2+).
 * Provides a submit collector and a {@link PoseStack} so mods can submit
 * geometry outside of {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}s.
 */
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "render-pass context: borrowed components are not retained by listeners")
public record LevelGeometrySubmitContext(
        //? if >=26.1 {
        /*SubmitNodeCollector collector,
        *///?}
        PoseStack poseStack,
        Level level,
        Vec3 cameraPosition) {
}
