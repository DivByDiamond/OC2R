
package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import javax.annotation.Nullable;
import java.io.IOException;

@SuppressWarnings("unused")
@EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID)
public final class ModShaders {
    // Bumped from 3 to 8 — when projectors are scattered across multiple
    // physical builds (Create: Aeronautics airships, VS2 ships, mixed
    // static builds), three is way too few. Each projector is sorted by
    // distance to the player, so the closest ones win when there are more
    // than MAX_PROJECTORS visible.
    public static final int MAX_PROJECTORS = 8;

    private static final ResourceLocation PROJECTORS_SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "projectors");
    private static final String[] PROJECTOR_COLOR_NAMES = {
        "ProjectorColor0", "ProjectorColor1", "ProjectorColor2", "ProjectorColor3",
        "ProjectorColor4", "ProjectorColor5", "ProjectorColor6", "ProjectorColor7",
    };
    private static final String[] PROJECTOR_DEPTH_NAMES = {
        "ProjectorDepth0", "ProjectorDepth1", "ProjectorDepth2", "ProjectorDepth3",
        "ProjectorDepth4", "ProjectorDepth5", "ProjectorDepth6", "ProjectorDepth7",
    };
    private static final String[] PROJECTOR_CAMERA_NAMES = {
        "ProjectorCamera0", "ProjectorCamera1", "ProjectorCamera2", "ProjectorCamera3",
        "ProjectorCamera4", "ProjectorCamera5", "ProjectorCamera6", "ProjectorCamera7",
    };


    private static ShaderInstance projectorsShader;


    @Nullable
    public static ShaderInstance getProjectorsShader() {
        return projectorsShader;
    }

    @SuppressWarnings("ConstantConditions") // Setting samples to null to clear them is fine.
    public static void configureProjectorsShader(
        final RenderTarget target,
        final Matrix4f inverseCameraMatrix,
        final DynamicTexture[] colors,
        final RenderTarget[] depths,
        final Matrix4f[] projectorCameraMatrices,
        final int count
    ) {
        final int projectorCount = Math.min(count, MAX_PROJECTORS);
        projectorsShader.safeGetUniform("Count").set(projectorCount);

        projectorsShader.setSampler("MainCameraDepth", target.getDepthTextureId());
        projectorsShader.safeGetUniform("InverseMainCamera").set(inverseCameraMatrix);

        for (int i = 0; i < MAX_PROJECTORS; i++) {
            if (i < projectorCount) {
                projectorsShader.setSampler(PROJECTOR_COLOR_NAMES[i], colors[i].getId());
                projectorsShader.setSampler(PROJECTOR_DEPTH_NAMES[i], depths[i].getDepthTextureId());
                projectorsShader.safeGetUniform(PROJECTOR_CAMERA_NAMES[i]).set(projectorCameraMatrices[i]);
            } else {
                projectorsShader.setSampler(PROJECTOR_COLOR_NAMES[i], null);
                projectorsShader.setSampler(PROJECTOR_DEPTH_NAMES[i], null);
            }
        }
    }

    @SubscribeEvent
    public static void handleRegisterShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(
            event.getResourceProvider(),
            PROJECTORS_SHADER_LOCATION,
            DefaultVertexFormat.POSITION_TEX
        ), instance -> projectorsShader = instance);
    }
}
