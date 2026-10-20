package li.cil.oc2.client.renderer.stage.shader;

//? if >=26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.ResourceLocation;

//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
//?}

//? if <26.1 {
import net.neoforged.api.distmarker.OnlyIn;
//?}

@OnlyIn(Dist.CLIENT)
public final class ModRenderType {
    // 26.x has no per-draw state shards: every render type is a pipeline plus a render setup.
    // Vertex lighting through the lightmap is not available to the mod pipelines, so lit geometry
    // bakes its light into the vertex color instead.
    private static final RenderPipeline NETWORK_CABLE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_network_cable")
                    .withCull(false)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .build();

    private static final RenderPipeline PROJECTOR_LIGHT_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_projector_light")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withCull(false)
                    .build();

    private static final RenderPipeline GATEWAY_PARTICLE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_gateway_particle")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .build();

    private static final RenderPipeline ADDITIVE_TEXTURE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withLocation("pipeline/oc2r_additive_texture")
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withCull(false)
                    .build();

    private static final RenderPipeline FRAMEBUFFER_PIPELINE =
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withLocation("pipeline/oc2r_framebuffer")
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withCull(false)
                    .build();

    // Terminal glyphs on a block face: the font atlas, alpha blended, depth tested but without
    // depth writes, matching the immediate 1.21.1 path (enableDepthTest + depthMask(false)).
    private static final RenderPipeline TERMINAL_TEXT_PIPELINE =
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/oc2r_terminal_text")
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .build();

    // The cursor is untextured colored quads: the debug filled pipeline already is exactly
    // that (alpha blended, depth test without depth writes, no culling once asked for).
    private static final RenderPipeline TERMINAL_CURSOR_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_terminal_cursor")
                    .withCull(false)
                    .build();

    private static final RenderType NETWORK_CABLE =
            RenderType.create(
                    API.MOD_ID + "/network_cable",
                    RenderSetup.builder(NETWORK_CABLE_PIPELINE).createRenderSetup());

    private static final RenderType PROJECTOR_LIGHT =
            RenderType.create(
                    API.MOD_ID + "/projector_light",
                    RenderSetup.builder(PROJECTOR_LIGHT_PIPELINE).createRenderSetup());

    private static final RenderType GATEWAY_PARTICLE =
            RenderType.create(
                    API.MOD_ID + "/gateway_particle",
                    RenderSetup.builder(GATEWAY_PARTICLE_PIPELINE).createRenderSetup());

    private static final RenderType TERMINAL_CURSOR =
            RenderType.create(
                    API.MOD_ID + "/terminal_cursor",
                    RenderSetup.builder(TERMINAL_CURSOR_PIPELINE).createRenderSetup());

    private static final Map<ResourceLocation, RenderType> ADDITIVE_TEXTURES =
            new ConcurrentHashMap<>();

    private static final Map<ResourceLocation, RenderType> FRAMEBUFFERS = new ConcurrentHashMap<>();

    // The font atlas texture only exists after the client built it, so this one is created on
    // first use, like the framebuffers above.
    private static RenderType terminalText;

    private ModRenderType() {}

    public static RenderType getNetworkCable() {
        return NETWORK_CABLE;
    }

    public static RenderType getProjectorLight() {
        return PROJECTOR_LIGHT;
    }

    public static RenderType getUnlitBlock(final ResourceLocation location) {
        return ADDITIVE_TEXTURES.computeIfAbsent(
                location,
                key ->
                        RenderType.create(
                                API.MOD_ID + "/unlit_block",
                                RenderSetup.builder(ADDITIVE_TEXTURE_PIPELINE)
                                        .withTexture("Sampler0", key)
                                        .createRenderSetup()));
    }

    public static RenderType getOverlay(final ResourceLocation location) {
        return getUnlitBlock(location);
    }

    // A plain, alpha-blended textured quad for the monitor framebuffer textures.
    public static RenderType getFramebuffer(final ResourceLocation location) {
        return FRAMEBUFFERS.computeIfAbsent(
                location,
                key ->
                        RenderType.create(
                                API.MOD_ID + "/framebuffer",
                                RenderSetup.builder(FRAMEBUFFER_PIPELINE)
                                        .withTexture("Sampler0", key)
                                        .createRenderSetup()));
    }

    public static RenderType getGateWayParticle() {
        return GATEWAY_PARTICLE;
    }

    // Terminal rows in the world: the font atlas with alpha blending, depth tested without
    // depth writes, like the immediate 1.21.1 path (enableDepthTest + depthMask(false)).
    public static RenderType getTerminalText() {
        if (terminalText == null) {
            terminalText =
                    RenderType.create(
                            API.MOD_ID + "/terminal_text",
                            RenderSetup.builder(TERMINAL_TEXT_PIPELINE)
                                    .withTexture("Sampler0", FontHandling.getAtlas())
                                    .createRenderSetup());
        }
        return terminalText;
    }

    public static RenderType getTerminalCursor() {
        return TERMINAL_CURSOR;
    }
}
*///?} else if >=26.1 {
/*import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.ResourceLocation;

//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
//?}

//? if <26.1 {
import net.neoforged.api.distmarker.OnlyIn;
//?}

@OnlyIn(Dist.CLIENT)
public final class ModRenderType {
    // 26.x has no per-draw state shards: every render type is a pipeline plus a render setup.
    // Vertex lighting through the lightmap is not available to the mod pipelines, so lit geometry
    // bakes its light into the vertex color instead.
    private static final RenderPipeline NETWORK_CABLE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_network_cable")
                    .withCull(false)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .build();

    private static final RenderPipeline PROJECTOR_LIGHT_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_projector_light")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withCull(false)
                    .build();

    private static final RenderPipeline GATEWAY_PARTICLE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_gateway_particle")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .build();

    private static final RenderPipeline ADDITIVE_TEXTURE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/oc2r_additive_texture")
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withSampler("Sampler0")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withCull(false)
                    .build();

    private static final RenderPipeline FRAMEBUFFER_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/oc2r_framebuffer")
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withSampler("Sampler0")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withCull(false)
                    .build();

    // Terminal glyphs on a block face: the font atlas, alpha blended, depth tested but without
    // depth writes, matching the immediate 1.21.1 path (enableDepthTest + depthMask(false)).
    private static final RenderPipeline TERMINAL_TEXT_PIPELINE =
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/oc2r_terminal_text")
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build();

    // The cursor is untextured colored quads: the debug filled pipeline already is exactly
    // that (alpha blended, depth test without depth writes, no culling once asked for).
    private static final RenderPipeline TERMINAL_CURSOR_PIPELINE =
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pipeline/oc2r_terminal_cursor")
                    .withCull(false)
                    .build();

    private static final RenderType NETWORK_CABLE =
            RenderType.create(
                    API.MOD_ID + "/network_cable",
                    RenderSetup.builder(NETWORK_CABLE_PIPELINE).createRenderSetup());

    private static final RenderType PROJECTOR_LIGHT =
            RenderType.create(
                    API.MOD_ID + "/projector_light",
                    RenderSetup.builder(PROJECTOR_LIGHT_PIPELINE).createRenderSetup());

    private static final RenderType GATEWAY_PARTICLE =
            RenderType.create(
                    API.MOD_ID + "/gateway_particle",
                    RenderSetup.builder(GATEWAY_PARTICLE_PIPELINE).createRenderSetup());

    private static final RenderType TERMINAL_CURSOR =
            RenderType.create(
                    API.MOD_ID + "/terminal_cursor",
                    RenderSetup.builder(TERMINAL_CURSOR_PIPELINE).createRenderSetup());

    private static final Map<ResourceLocation, RenderType> ADDITIVE_TEXTURES =
            new ConcurrentHashMap<>();

    private static final Map<ResourceLocation, RenderType> FRAMEBUFFERS = new ConcurrentHashMap<>();

    // The font atlas texture only exists after the client built it, so this one is created on
    // first use, like the framebuffers above.
    private static RenderType terminalText;

    private ModRenderType() {}

    public static RenderType getNetworkCable() {
        return NETWORK_CABLE;
    }

    public static RenderType getProjectorLight() {
        return PROJECTOR_LIGHT;
    }

    public static RenderType getUnlitBlock(final ResourceLocation location) {
        return ADDITIVE_TEXTURES.computeIfAbsent(
                location,
                key ->
                        RenderType.create(
                                API.MOD_ID + "/unlit_block",
                                RenderSetup.builder(ADDITIVE_TEXTURE_PIPELINE)
                                        .withTexture("Sampler0", key)
                                        .createRenderSetup()));
    }

    public static RenderType getOverlay(final ResourceLocation location) {
        return getUnlitBlock(location);
    }

    // A plain, alpha-blended textured quad for the monitor framebuffer textures.
    public static RenderType getFramebuffer(final ResourceLocation location) {
        return FRAMEBUFFERS.computeIfAbsent(
                location,
                key ->
                        RenderType.create(
                                API.MOD_ID + "/framebuffer",
                                RenderSetup.builder(FRAMEBUFFER_PIPELINE)
                                        .withTexture("Sampler0", key)
                                        .createRenderSetup()));
    }

    public static RenderType getGateWayParticle() {
        return GATEWAY_PARTICLE;
    }

    // Terminal rows in the world: the font atlas with alpha blending, depth tested without
    // depth writes, like the immediate 1.21.1 path (enableDepthTest + depthMask(false)).
    public static RenderType getTerminalText() {
        if (terminalText == null) {
            terminalText =
                    RenderType.create(
                            API.MOD_ID + "/terminal_text",
                            RenderSetup.builder(TERMINAL_TEXT_PIPELINE)
                                    .withTexture("Sampler0", FontHandling.getAtlas())
                                    .createRenderSetup());
        }
        return terminalText;
    }

    public static RenderType getTerminalCursor() {
        return TERMINAL_CURSOR;
    }
}
*///?} else {
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

@OnlyIn(Dist.CLIENT)
public abstract class ModRenderType extends RenderType {
    private static final RenderType NETWORK_CABLE =
            create(
                    API.MOD_ID + "/network_cable",
                    DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    CompositeState.builder()
                            .setShaderState(POSITION_COLOR_LIGHTMAP_SHADER)
                            .setTextureState(NO_TEXTURE)
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .createCompositeState(false));

    private static final RenderType PROJECTOR_LIGHT =
            create(
                    API.MOD_ID + "/projector_light",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .createCompositeState(false));

    private static final RenderType GATEWAY_PARTICLE =
            create(
                    API.MOD_ID + "/gateway_particle",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(CULL)
                            .createCompositeState(false));

    public static RenderType getNetworkCable() {
        return NETWORK_CABLE;
    }

    public static RenderType getProjectorLight() {
        return PROJECTOR_LIGHT;
    }

    public static RenderType getUnlitBlock(final ResourceLocation location) {
        final TextureStateShard texture = new TextureStateShard(location, false, true);
        final RenderType.CompositeState state =
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_TEX_SHADER)
                        .setTextureState(texture)
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .createCompositeState(false);
        return create(
                API.MOD_ID + "/unlit_block",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                state);
    }

    public static RenderType getOverlay(final ResourceLocation location) {
        final TextureStateShard texture = new TextureStateShard(location, false, true);
        final RenderType.CompositeState state =
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_TEX_SHADER)
                        .setTextureState(texture)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .createCompositeState(false);
        return create(
                API.MOD_ID + "/overlay",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                state);
    }

    public static RenderType getGateWayParticle() {
        return GATEWAY_PARTICLE;
    }

    private ModRenderType(
            final String name,
            final VertexFormat format,
            final VertexFormat.Mode drawMode,
            final int bufferSize,
            final boolean useDelegate,
            final boolean needsSorting,
            final Runnable setupTask,
            final Runnable clearTask) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
    }
}
//?}
