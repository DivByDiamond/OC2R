package li.cil.oc2.client.renderer.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OverlayRendererTest {
    @Test
    void quadUsesSpriteCoordinatesWhenVertexAdditionReturnsTheDelegate() {
        final VertexConsumer delegate = mock(VertexConsumer.class, RETURNS_SELF);
        final TextureAtlasSprite sprite = mock(TextureAtlasSprite.class);
        when(sprite.getU(anyFloat())).thenAnswer(call -> 0.125f + call.<Float>getArgument(0) * 0.25f);
        when(sprite.getV(anyFloat())).thenAnswer(call -> 0.5f + call.<Float>getArgument(0) * 0.125f);
        // The real wrapper returns its delegate from addVertex, so fluent setUv calls bypass it.
        final VertexConsumer wrapped = new SpriteCoordinateExpander(delegate, sprite);

        OverlayRenderer.renderQuad(new Matrix4f(), wrapped);

        final ArgumentCaptor<Float> u = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Float> v = ArgumentCaptor.forClass(Float.class);
        verify(delegate, times(4)).setUv(u.capture(), v.capture());
        assertEquals(List.of(0.125f, 0.125f, 0.375f, 0.375f), u.getAllValues());
        assertEquals(List.of(0.5f, 0.625f, 0.625f, 0.5f), v.getAllValues());
    }
}
