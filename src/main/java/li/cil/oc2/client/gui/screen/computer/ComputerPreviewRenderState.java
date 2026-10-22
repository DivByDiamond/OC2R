package li.cil.oc2.client.gui.screen.computer;

//? if >=26.1 {
/*import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.Direction;
import org.joml.Matrix3x2f;

// Captured during GUI extraction; rendering never reads the player's live card or screen.
public record ComputerPreviewRenderState(
        ItemStackRenderState item,
        float pitch, float yaw, float roll,
        int enabledFaces,
        @Nullable Direction focusedSide,
        int x0, int x1, int y0, int y1,
        float scale,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
}
*///?}
