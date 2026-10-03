package li.cil.oc2.platform;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import li.cil.oc2.client.model.FabricModels;
import li.cil.oc2.client.renderer.entity.RobotWithoutLevelRenderer;
import li.cil.oc2.client.renderer.stage.shader.ModShaders;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.item.Items;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

/**
 * The Fabric client registrations that have no loader independent description in
 * {@link ClientRegistrar}: models, shaders, the robot item renderer and block render layers.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientSetup {
    private static RobotWithoutLevelRenderer robotItemRenderer;

    private FabricClientSetup() {}

    public static void register() {
        FabricModels.register();

        // NeoForge takes this from the "render_type" of the cable model JSON, vanilla ignores it.
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BUS_CABLE.get(), RenderType.cutout());

        CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(
                        ModShaders.projectorsShaderLocation(),
                        DefaultVertexFormat.POSITION_TEX,
                        ModShaders::setProjectorsShader));

        // The robot item renders its entity model instead of a flat item model.
        BuiltinItemRendererRegistry.INSTANCE.register(
                Items.ROBOT.get(),
                (stack, mode, matrices, buffers, light, overlay) -> {
                    if (robotItemRenderer == null) {
                        final Minecraft minecraft = Minecraft.getInstance();
                        robotItemRenderer = new RobotWithoutLevelRenderer(
                                minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                    }
                    robotItemRenderer.renderByItem(stack, mode, matrices, buffers, light, overlay);
                });
    }
}
