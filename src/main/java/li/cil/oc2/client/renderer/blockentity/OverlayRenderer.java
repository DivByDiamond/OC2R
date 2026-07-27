package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.VertexConsumer;

import li.cil.oc2.client.renderer.ModRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix4f;

final class OverlayRenderer {
    static final Material TEXTURE_POWER =
            new Material(InventoryMenu.BLOCK_ATLAS, ComputerRenderer.OVERLAY_POWER_LOCATION);
    static final Material TEXTURE_STATUS =
            new Material(InventoryMenu.BLOCK_ATLAS, ComputerRenderer.OVERLAY_STATUS_LOCATION);
    static final Material TEXTURE_TERMINAL =
            new Material(InventoryMenu.BLOCK_ATLAS, ComputerRenderer.OVERLAY_TERMINAL_LOCATION);

    static void renderStatus(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderStatus(matrix, bufferSource, 0);
    }

    static void renderStatus(
            final Matrix4f matrix, final MultiBufferSource bufferSource, final int frequency) {
        if (frequency <= 0 || ((System.currentTimeMillis() / frequency) % 2) == 1) {
            renderQuad(matrix, TEXTURE_STATUS.buffer(bufferSource, ModRenderType::getUnlitBlock));
        }
    }

    static void renderPower(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        renderQuad(matrix, TEXTURE_POWER.buffer(bufferSource, ModRenderType::getUnlitBlock));
    }

    static void renderQuad(final Matrix4f matrix, final VertexConsumer consumer) {
        consumer.addVertex(matrix, 0, 0, 0).setUv(0, 0);
        consumer.addVertex(matrix, 0, 16, 0).setUv(0, 1);
        consumer.addVertex(matrix, 16, 16, 0).setUv(1, 1);
        consumer.addVertex(matrix, 16, 0, 0).setUv(1, 0);
    }
}
