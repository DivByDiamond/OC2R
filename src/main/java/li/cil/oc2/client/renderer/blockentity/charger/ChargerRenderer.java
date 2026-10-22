package li.cil.oc2.client.renderer.blockentity.charger;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import li.cil.oc2.common.blockentity.energy.ChargerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ChargerRenderer
        implements BlockEntityRenderer<ChargerBlockEntity, ChargerRenderer.State> {
    public static final ResourceLocation EFFECT_LOCATION =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/charger/effect");

    // TextureAtlas.LOCATION_BLOCKS is deprecated on 26.x without a runtime replacement; the blocks
    // atlas texture path is still the SpriteId lookup key (the atlas manager stitches by texture id).
    // This sits in the outer >=26.1 branch, so no nested conditional is needed here.
    private static final ResourceLocation ATLAS_BLOCKS =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");

    private static final SpriteId TEXTURE_EFFECT = new SpriteId(ATLAS_BLOCKS, EFFECT_LOCATION);

    private static final int EFFECT_LAYERS = 3;
    private static final float EFFECT_HEIGHT = 0.5f;
    private static final float EFFECT_SPEED = 0.1f;
    private static final float EFFECT_SCALE_START = 0.6f;
    private static final float EFFECT_SCALE_END = 0.8f;

    private float offset = 0;

    public static final class State extends BlockEntityRenderState {
        float partialTicks;
    }

    public ChargerRenderer(final BlockEntityRendererProvider.Context ignoredContext) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            final ChargerBlockEntity charger,
            final State state,
            final float partialTicks,
            final Vec3 cameraPosition,
            @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                charger, state, partialTicks, cameraPosition, breakProgress);
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(
            final State state,
            final PoseStack stack,
            final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        offset = (offset + EFFECT_SPEED * state.partialTicks / 20f) % (float) (Math.PI * 2);

        final TextureAtlasSprite sprite =
                Minecraft.getInstance().getAtlasManager().get(TEXTURE_EFFECT);

        stack.pushPose();
        stack.translate(0.5, 1.1, 0.5);

        for (int i = 0; i < EFFECT_LAYERS; i++) {
            final float relativeY =
                    (1 + Mth.sin(offset + ((float) Math.PI * 2f * i / EFFECT_LAYERS))) * 0.5f;
            final float y = relativeY * EFFECT_HEIGHT;
            final float scale =
                    EFFECT_SCALE_START + relativeY * (EFFECT_SCALE_END - EFFECT_SCALE_START);

            stack.pushPose();
            stack.translate(0, y, 0);
            stack.scale(scale, scale, scale);
            collector.submitCustomGeometry(
                    stack,
                    ModRenderType.getUnlitBlock(TEXTURE_EFFECT.atlasLocation()),
                    (pose, buffer) -> renderQuad(pose, sprite.wrap(buffer)));
            stack.popPose();
        }

        stack.popPose();
    }

    private static void renderQuad(final PoseStack.Pose pose, final VertexConsumer consumer) {
        consumer.addVertex(pose, -0.5f, 0, -0.5f).setUv(0, 0);

        consumer.addVertex(pose, -0.5f, 0, 0.5f).setUv(0, 1);

        consumer.addVertex(pose, 0.5f, 0, 0.5f).setUv(1, 1);

        consumer.addVertex(pose, 0.5f, 0, -0.5f).setUv(1, 0);
    }

    // NeoForge BlockEntityRenderer hook; vanilla (and Fabric) has no such method, so no @Override.
    @SuppressWarnings({"MissingOverride", "PMD.MissingOverride"})
    public AABB getRenderBoundingBox(ChargerBlockEntity block) {
        return block.getRenderBoundingBox();
    }
}
*///?} else {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import li.cil.oc2.common.blockentity.energy.ChargerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class ChargerRenderer implements BlockEntityRenderer<ChargerBlockEntity> {
    public static final ResourceLocation EFFECT_LOCATION =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/charger/effect");

    private static final Material TEXTURE_EFFECT =
            new Material(InventoryMenu.BLOCK_ATLAS, EFFECT_LOCATION);

    private static final int EFFECT_LAYERS = 3;
    private static final float EFFECT_HEIGHT = 0.5f;
    private static final float EFFECT_SPEED = 0.1f;
    private static final float EFFECT_SCALE_START = 0.6f;
    private static final float EFFECT_SCALE_END = 0.8f;

    private float offset = 0;

    public ChargerRenderer(final BlockEntityRendererProvider.Context ignoredContext) {}

    @Override
    public void render(
            final ChargerBlockEntity charger,
            final float partialTicks,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final int light,
            final int overlay) {
        offset = (offset + EFFECT_SPEED * partialTicks / 20f) % (float) (Math.PI * 2);

        stack.pushPose();
        stack.translate(0.5, 1.1, 0.5);

        final VertexConsumer consumer =
                TEXTURE_EFFECT.buffer(bufferSource, ModRenderType::getUnlitBlock);

        for (int i = 0; i < EFFECT_LAYERS; i++) {
            final float relativeY =
                    (1 + Mth.sin(offset + ((float) Math.PI * 2f * i / EFFECT_LAYERS))) * 0.5f;
            final float y = relativeY * EFFECT_HEIGHT;
            final float scale =
                    EFFECT_SCALE_START + relativeY * (EFFECT_SCALE_END - EFFECT_SCALE_START);

            stack.pushPose();
            stack.translate(0, y, 0);
            renderScaledQuad(stack, consumer, scale);
            stack.popPose();
        }

        stack.popPose();
    }

    private static void renderScaledQuad(
            final PoseStack stack, final VertexConsumer consumer, final float scale) {
        stack.pushPose();
        stack.scale(scale, scale, scale);
        renderQuad(stack.last().pose(), consumer);
        stack.popPose();
    }

    private static void renderQuad(final Matrix4f matrix, final VertexConsumer consumer) {
        consumer.addVertex(matrix, -0.5f, 0, -0.5f).setUv(0, 0);

        consumer.addVertex(matrix, -0.5f, 0, 0.5f).setUv(0, 1);

        consumer.addVertex(matrix, 0.5f, 0, 0.5f).setUv(1, 1);

        consumer.addVertex(matrix, 0.5f, 0, -0.5f).setUv(1, 0);
    }

    // NeoForge BlockEntityRenderer hook; vanilla (and Fabric) has no such method, so no @Override.
    @SuppressWarnings({"MissingOverride", "PMD.MissingOverride"})
    public AABB getRenderBoundingBox(ChargerBlockEntity block) {
        return block.getRenderBoundingBox();
    }
}
//?}
