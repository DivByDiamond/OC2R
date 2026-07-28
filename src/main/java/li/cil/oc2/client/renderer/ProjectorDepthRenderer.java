package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class ProjectorDepthRenderer {
    record VisibleProjector(ProjectorBlockEntity projector, Vec3 worldPos, float yRot) {}

    static final List<VisibleProjector> VISIBLE_PROJECTORS = new ArrayList<>();
    static final DepthOnlyRenderTarget[] PROJECTOR_DEPTH_TARGETS =
            new DepthOnlyRenderTarget[ModShaders.MAX_PROJECTORS];
    static final DynamicTexture[] PROJECTOR_COLOR_TARGETS =
            new DynamicTexture[ModShaders.MAX_PROJECTORS];
    static final Matrix4f[] PROJECTOR_CAMERA_MATRICES = new Matrix4f[ModShaders.MAX_PROJECTORS];
    static DepthOnlyRenderTarget MAIN_CAMERA_DEPTH = null;
    static boolean isRenderingProjectorDepth;

    private static final Cache<ProjectorBlockEntity, ProjectorDepthRenderInfo> RENDER_INFO =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(Duration.ofSeconds(5))
                    .removalListener(ProjectorDepthRenderer::handleProjectorNoLongerRendering)
                    .build();

    static {
        RenderSystem.recordRenderCall(
                () -> {
                    MAIN_CAMERA_DEPTH =
                            new DepthOnlyRenderTarget(
                                    MainTarget.DEFAULT_WIDTH, MainTarget.DEFAULT_HEIGHT);
                    for (int i = 0; i < ModShaders.MAX_PROJECTORS; i++) {
                        PROJECTOR_DEPTH_TARGETS[i] = new DepthOnlyRenderTarget(512, 512);
                        PROJECTOR_CAMERA_MATRICES[i] = new Matrix4f();
                    }
                });
    }

    private static void handleProjectorNoLongerRendering(
            final RemovalNotification<ProjectorBlockEntity, ProjectorDepthRenderInfo>
                    notification) {
        final ProjectorBlockEntity projector = notification.getKey();
        if (projector != null) {
            projector.setFrameConsumer(null);
        }
        final ProjectorDepthRenderInfo renderInfo = notification.getValue();
        if (renderInfo != null) {
            renderInfo.close();
        }
    }

    public static void addProjector(
            final ProjectorBlockEntity projector, final Vec3 worldPos, final float yRot) {
        VISIBLE_PROJECTORS.add(new VisibleProjector(projector, worldPos, yRot));
    }

    public static boolean willRenderProjectorDepth() {
        return !VISIBLE_PROJECTORS.isEmpty();
    }

    public static boolean isIsRenderingProjectorDepth() {
        return isRenderingProjectorDepth;
    }

    public static void captureMainCameraDepth() {
        final RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainRenderTarget.width != MAIN_CAMERA_DEPTH.width
                || mainRenderTarget.height != MAIN_CAMERA_DEPTH.height) {
            MAIN_CAMERA_DEPTH.resize(
                    mainRenderTarget.width, mainRenderTarget.height, Minecraft.ON_OSX);
        }
        if (mainRenderTarget.isStencilEnabled()) {
            MAIN_CAMERA_DEPTH.enableStencil();
        } else if (MAIN_CAMERA_DEPTH.isStencilEnabled()) {
            MAIN_CAMERA_DEPTH.destroyBuffers();
            MAIN_CAMERA_DEPTH =
                    new DepthOnlyRenderTarget(mainRenderTarget.width, mainRenderTarget.height);
        }
        MAIN_CAMERA_DEPTH.copyDepthFrom(mainRenderTarget);
        mainRenderTarget.bindWrite(false);
    }

    @SubscribeEvent
    public static void renderProjectors(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (isIsRenderingProjectorDepth()) {
            return;
        }
        if (VISIBLE_PROJECTORS.isEmpty()) {
            return;
        }
        try {
            final Minecraft minecraft = Minecraft.getInstance();
            final ClientLevel level = minecraft.level;
            final LocalPlayer player = minecraft.player;
            if (level == null || player == null) {
                return;
            }

            VISIBLE_PROJECTORS.sort(
                    (p1, p2) -> {
                        final double distance1 = player.distanceToSqr(p1.worldPos());
                        final double distance2 = player.distanceToSqr(p2.worldPos());
                        return Double.compare(distance1, distance2);
                    });

            final int projectorCount =
                    Math.min(VISIBLE_PROJECTORS.size(), ModShaders.MAX_PROJECTORS);
            DepthBufferStage.renderProjectorDepths(
                    minecraft, level, event.getPartialTick(), projectorCount);
            ColorCompositingStage.renderProjectorColors(
                    minecraft,
                    event.getPoseStack().last().pose(),
                    event.getProjectionMatrix(),
                    projectorCount);
        } finally {
            VISIBLE_PROJECTORS.clear();
            Arrays.fill(PROJECTOR_COLOR_TARGETS, null);
        }
    }

    @SubscribeEvent
    public static void handleFog(final ViewportEvent.RenderFog event) {
        if (isRenderingProjectorDepth) {
            FogRenderer.setupNoFog();
        }
    }

    @SubscribeEvent
    public static void handleNameplate(final RenderNameTagEvent event) {
        if (isRenderingProjectorDepth) {
            event.setCanRender(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void handleClientTick(final ClientTickEvent.Pre event) {
        RENDER_INFO.cleanUp();
    }

    static DynamicTexture getColorBuffer(final ProjectorBlockEntity projector) {
        try {
            return RENDER_INFO
                    .get(
                            projector,
                            () -> {
                                final DynamicTexture texture =
                                        new DynamicTexture(
                                                ProjectorDevice.WIDTH,
                                                ProjectorDevice.HEIGHT,
                                                false);
                                texture.upload();
                                final ProjectorDepthRenderInfo renderInfo =
                                        new ProjectorDepthRenderInfo(texture);
                                projector.setFrameConsumer(renderInfo);
                                return renderInfo;
                            })
                    .texture();
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}