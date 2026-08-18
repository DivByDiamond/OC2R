package li.cil.oc2.client.gui.screen.computer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import li.cil.oc2.client.gui.Textures;
import li.cil.oc2.client.gui.screen.network.NetworkInterfaceCardScreen;
import li.cil.oc2.client.gui.widget.Texture;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ComputerBlockItemRenderer {
    public static final int BLOCK_RENDER_SIZE = 48;

    private final ItemStack computerItemStack = new ItemStack(Items.COMPUTER.get());
    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
    private final BakedModel model = itemRenderer.getModel(computerItemStack, null, null, 0);

    @Nullable
    public Direction getFocusedSide(final float mouseX, final float mouseY, final Vector3f rotation) {
        final Quaternionf quaternion =
                new Quaternionf().rotateXYZ(rotation.x, rotation.y, rotation.z);
        quaternion.conjugate();

        final float relMouseX = -mouseX / BLOCK_RENDER_SIZE;
        final float relMouseY = -mouseY / BLOCK_RENDER_SIZE;

        final Vector3f source = new Vector3f();
        source.add(relMouseX, relMouseY, 1);
        source.rotate(quaternion);

        final Vector3f target = new Vector3f();
        target.add(relMouseX, relMouseY, -1);
        target.rotate(quaternion);

        final AABB aabb = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        return aabb.clip(new Vec3(source), new Vec3(target))
                .map(hit -> Direction.getNearest(hit.x, -hit.y(), hit.z()))
                .filter(side -> side != Direction.SOUTH)
                .orElse(null);
    }

    public void render(
            final int x,
            final int y,
            final Vector3f rotation,
            @Nullable final Direction focusedSide,
            final NetworkInterfaceCardScreen screen) {
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        final Vector3f renderRotation = new Vector3f(rotation.x, rotation.y, rotation.z);
        renderRotation.add(0, 180, 0);

        final Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.translate(x, y, 0);
        stack.rotate(
                new Quaternionf().rotateXYZ(renderRotation.x, renderRotation.y, renderRotation.z));
        stack.scale(BLOCK_RENDER_SIZE, -BLOCK_RENDER_SIZE, BLOCK_RENDER_SIZE);
        RenderSystem.applyModelViewMatrix();

        final MultiBufferSource.BufferSource bufferSource =
                Minecraft.getInstance().renderBuffers().bufferSource();
        renderBlock(bufferSource);
        renderOverlays(stack, bufferSource, focusedSide, screen);
        bufferSource.endBatch();

        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private void renderBlock(final MultiBufferSource.BufferSource bufferSource) {
        itemRenderer.render(
                computerItemStack,
                ItemDisplayContext.NONE,
                false,
                new PoseStack(),
                bufferSource,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                model);
    }

    private void renderOverlays(
            final Matrix4fStack poseStack,
            final MultiBufferSource.BufferSource bufferSource,
            @Nullable final Direction focusedSide,
            final NetworkInterfaceCardScreen screen) {
        for (final Direction side : Direction.values()) {
            if (side == Direction.SOUTH) {
                continue;
            }

            poseStack.pushMatrix();
            poseStack.identity();

            poseStack.translate(
                    -side.getStepX() * 0.51f, side.getStepY() * 0.51f, -side.getStepZ() * 0.51f);

            final Vector3f sideRotation = getSideRotation(side);
            sideRotation.mul((float) Math.PI / 180.0f);
            // NOPMD - depends on loop variable (per-face rotation)
            poseStack.rotate(
                    new Quaternionf().rotateXYZ(sideRotation.x, sideRotation.y, sideRotation.z)); // NOPMD allocation depends on loop iteration / per-item state

            poseStack.translate(-0.5f, -0.5f, 0f);

            if (screen.getConfiguration(side)) {
                renderOverlay(poseStack, bufferSource, Textures.BLOCK_FACE_ENABLED_TEXTURE);
            } else {
                renderOverlay(poseStack, bufferSource, Textures.BLOCK_FACE_DISABLED_TEXTURE);
            }

            if (side == focusedSide) {
                renderOverlay(poseStack, bufferSource, Textures.BLOCK_FACE_FOCUSED_TEXTURE);
            }

            poseStack.popMatrix();
        }
    }

    private static Vector3f getSideRotation(final Direction side) {
        return switch (side) {
            case DOWN -> new Vector3f(-90, 0, 0);
            case UP -> new Vector3f(90, 0, 0);
            case NORTH -> new Vector3f(0, 180, 0);
            case WEST -> new Vector3f(0, -90, 0);
            case EAST -> new Vector3f(0, 90, 0);
            default -> throw new IllegalStateException("Unexpected value: " + side);
        };
    }

    private void renderOverlay(
            final Matrix4fStack poseStack,
            final MultiBufferSource.BufferSource bufferSource,
            final Texture texture) {
        final VertexConsumer buffer =
                bufferSource.getBuffer(ModRenderType.getOverlay(texture.location));

        buffer.addVertex(poseStack, 0, 0, 0).setUv(0, 0);
        buffer.addVertex(poseStack, 0, 1, 0).setUv(0, 1);
        buffer.addVertex(poseStack, 1, 1, 0).setUv(1, 1);
        buffer.addVertex(poseStack, 1, 0, 0).setUv(1, 0);
    }
}