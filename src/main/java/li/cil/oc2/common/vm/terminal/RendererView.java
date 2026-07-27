package li.cil.oc2.common.vm.terminal;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

public interface RendererView {
    void render(final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock);
}