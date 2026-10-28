package li.cil.oc2.platform;

//? if <26.1 {
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//?}
//? if >=26.1 {
/*import li.cil.oc2.client.gui.screen.computer.ComputerPreviewRenderer;
*///?}
import li.cil.oc2.client.input.CaptureKeyBinding;
import li.cil.oc2.client.model.FabricModels;
//? if >=26.1 {
/*import li.cil.oc2.client.renderer.entity.RobotSpecialModelRenderer;
*///?}
//? if <26.1 {
import li.cil.oc2.client.renderer.entity.RobotWithoutLevelRenderer;
import li.cil.oc2.client.renderer.stage.shader.ModShaders;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.item.Items;
//?}
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if <26.1 {
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
//?}
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?} else {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}
//? if <26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
//?}
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
*///?}
//? if <26.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
//?}
//? if >=26.1 {
/*import net.minecraft.client.renderer.special.SpecialModelRenderers;
*///?}

/**
 * The Fabric client registrations that have no loader independent description in
 * {@link ClientRegistrar}: models, shaders, the robot item renderer and block render layers.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientSetup {
    //? if <26.1 {
    private static RobotWithoutLevelRenderer robotItemRenderer;
    //?}

    private FabricClientSetup() {}

    /** Registers the client callbacks and renderers for this loader. */
    public static void register() {
        //? if >=26.1 {
        /*KeyMappingHelper.registerKeyMapping(CaptureKeyBinding.RELEASE);
        *///?} else {
        KeyBindingHelper.registerKeyBinding(CaptureKeyBinding.RELEASE);
        //?}
        FabricModels.register();

        //? if >=26.1 {
/*        SpecialModelRenderers.ID_MAPPER.put(
                RobotSpecialModelRenderer.ID, RobotSpecialModelRenderer.Unbaked.MAP_CODEC);
        PictureInPictureRendererRegistry.register(context ->
*///?}
        //? if >=26.2 {
/*                new ComputerPreviewRenderer());
*///?} else if >=26.1 {
/*                new ComputerPreviewRenderer(context.minecraft().renderBuffers().bufferSource()));
*///?} else {
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
        //?}
    }
}
