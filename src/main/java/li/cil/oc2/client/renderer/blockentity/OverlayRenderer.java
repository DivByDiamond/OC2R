package li.cil.oc2.client.renderer.blockentity;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.client.renderer.blockentity.computer.ComputerRenderer;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;

public final class OverlayRenderer {
    // TextureAtlas.LOCATION_BLOCKS is deprecated on 26.x without a runtime replacement; the blocks
    // atlas texture path is still the SpriteId lookup key (the atlas manager stitches by texture id).
    // This sits in the outer >=26.1 branch, so no nested conditional is needed here.
    private static final net.minecraft.resources.ResourceLocation ATLAS_BLOCKS =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");

    static final SpriteId TEXTURE_POWER = new SpriteId(ATLAS_BLOCKS, ComputerRenderer.OVERLAY_POWER_LOCATION);
    static final SpriteId TEXTURE_STATUS =
            new SpriteId(ATLAS_BLOCKS, ComputerRenderer.OVERLAY_STATUS_LOCATION);
    static final SpriteId TEXTURE_TERMINAL =
            new SpriteId(ATLAS_BLOCKS, ComputerRenderer.OVERLAY_TERMINAL_LOCATION);

    public static void renderStatus(final PoseStack stack, final SubmitNodeCollector collector) {
        renderStatus(stack, collector, 0);
    }

    public static void renderStatus(
            final PoseStack stack, final SubmitNodeCollector collector, final int frequency) {
        if (frequency <= 0 || System.currentTimeMillis() / frequency % 2 == 1) {
            renderQuad(stack, collector, TEXTURE_STATUS);
        }
    }

    public static void renderPower(final PoseStack stack, final SubmitNodeCollector collector) {
        renderQuad(stack, collector, TEXTURE_POWER);
    }

    static void renderQuad(
            final PoseStack stack, final SubmitNodeCollector collector, final SpriteId sprite) {
        final TextureAtlasSprite atlasSprite = Minecraft.getInstance().getAtlasManager().get(sprite);
        collector.submitCustomGeometry(
                stack,
                ModRenderType.getUnlitBlock(sprite.atlasLocation()),
                (pose, buffer) -> {
                    final VertexConsumer consumer = atlasSprite.wrap(buffer);
                    consumer.addVertex(pose, 0, 0, 0).setUv(0, 0);
                    consumer.addVertex(pose, 0, 16, 0).setUv(0, 1);
                    consumer.addVertex(pose, 16, 16, 0).setUv(1, 1);
                    consumer.addVertex(pose, 16, 0, 0).setUv(1, 0);
                });
    }
}
*///?} else {
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.client.renderer.blockentity.computer.ComputerRenderer;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public final class OverlayRenderer {
    static final Material TEXTURE_POWER =
            new Material(InventoryMenu.BLOCK_ATLAS, ComputerRenderer.OVERLAY_POWER_LOCATION);
    static final Material TEXTURE_STATUS =
            new Material(InventoryMenu.BLOCK_ATLAS, ComputerRenderer.OVERLAY_STATUS_LOCATION);
    static final Material TEXTURE_TERMINAL =
            new Material(InventoryMenu.BLOCK_ATLAS, ComputerRenderer.OVERLAY_TERMINAL_LOCATION);

    public static void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderStatus(matrix, bufferSource, 0);
    }

    public static void renderStatus(
            final Matrix4f matrix, final MultiBufferSource bufferSource, final int frequency) {
        if (frequency <= 0 || System.currentTimeMillis() / frequency % 2 == 1) {
            renderQuad(matrix, TEXTURE_STATUS.buffer(bufferSource, ModRenderType::getUnlitBlock));
        }
    }

    public static void renderPower(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderQuad(matrix, TEXTURE_POWER.buffer(bufferSource, ModRenderType::getUnlitBlock));
    }

    static void renderQuad(final Matrix4f matrix, final VertexConsumer consumer) {
        consumer.addVertex(matrix, 0, 0, 0).setUv(0, 0);
        consumer.addVertex(matrix, 0, 16, 0).setUv(0, 1);
        consumer.addVertex(matrix, 16, 16, 0).setUv(1, 1);
        consumer.addVertex(matrix, 16, 0, 0).setUv(1, 0);
    }
}
//?}
