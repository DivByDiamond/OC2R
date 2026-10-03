package li.cil.oc2.platform;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.stage.shader.ModShaders;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Registers the mod's shaders on NeoForge. */
@EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID)
public final class NeoForgeShaderEvents {
    private NeoForgeShaderEvents() {}

    @SubscribeEvent
    public static void handleRegisterShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ModShaders.projectorsShaderLocation(),
                        DefaultVertexFormat.POSITION_TEX),
                ModShaders::setProjectorsShader);
    }
}
