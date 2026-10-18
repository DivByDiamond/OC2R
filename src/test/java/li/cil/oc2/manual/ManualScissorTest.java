//? if <26.1 {
package li.cil.oc2.manual;

import static org.mockito.Mockito.*;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.manual.client.util.ManualGraphics;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

class ManualScissorTest {
    @Test
    void clippingFollowsTheTranslatedAndScaledManualDocument() {
        final GuiGraphics graphics = mock(GuiGraphics.class);
        final PoseStack pose = new PoseStack();
        when(graphics.pose()).thenReturn(pose);
        pose.translate(100, 50, 0);
        pose.scale(2, 2, 1);

        ManualGraphics.enableScissor(graphics, 216, 232);

        verify(graphics).enableScissor(100, 50, 532, 514);
    }
}
//?}
