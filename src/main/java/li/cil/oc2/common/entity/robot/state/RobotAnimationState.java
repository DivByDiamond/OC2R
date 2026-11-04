package li.cil.oc2.common.entity.robot.state;

import java.util.function.Supplier;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class RobotAnimationState {
    private static final float TOP_IDLE_Y = -2f / 16f;
    private static final float BASE_IDLE_Y = -1f / 16f;

    private static final float TRANSLATION_SPEED = 0.005f;
    private static final float ROTATION_SPEED = 1f;
    private static final float MAX_ROTATION = 5f;
    private static final float MIN_ROTATION_SPEED = 0.055f;
    private static final float MAX_ROTATION_SPEED = 0.060f;
    private static final float HOVER_ANIMATION_SPEED = 0.01f;

    private float topRenderOffsetY = TOP_IDLE_Y;
    private float baseRenderOffsetY = BASE_IDLE_Y;
    private float topRenderRotationY;
    private float topRenderOffsetYO = TOP_IDLE_Y;
    private float baseRenderOffsetYO = BASE_IDLE_Y;
    private float topRenderRotationYO;
    private float topRenderTargetRotationY;
    private float topRenderRotationSpeed;
    private float topRenderHover;

    private final VirtualMachine virtualMachine;
    private final Supplier<Boolean> hasQueuedActions;

    public RobotAnimationState(
            final VirtualMachine virtualMachine, final Supplier<Boolean> hasQueuedActions) {
        this.virtualMachine = virtualMachine;
        this.hasQueuedActions = hasQueuedActions;
        topRenderHover = -(hashCode() & 0xFFFF);
    }

    public float getTopRenderOffsetY(final float partialTicks) {
        return Mth.lerp(partialTicks, topRenderOffsetYO, topRenderOffsetY);
    }

    public float getBaseRenderOffsetY(final float partialTicks) {
        return Mth.lerp(partialTicks, baseRenderOffsetYO, baseRenderOffsetY);
    }

    public float getTopRenderRotationY(final float partialTicks) {
        return Mth.lerp(partialTicks, topRenderRotationYO, topRenderRotationY);
    }

    /** Advances the animation by one game tick; never call this from a render pass. */
    public void tick(final RandomSource random) {
        topRenderOffsetYO = topRenderOffsetY;
        baseRenderOffsetYO = baseRenderOffsetY;
        topRenderRotationYO = topRenderRotationY;

        if (virtualMachine.isRunning() || hasQueuedActions.get()) {
            topRenderHover = topRenderHover + HOVER_ANIMATION_SPEED;
            final float topOffsetY = Mth.sin(topRenderHover) / 32f;

            topRenderOffsetY = lerpClamped(topRenderOffsetY, topOffsetY, TRANSLATION_SPEED);
            baseRenderOffsetY = lerpClamped(baseRenderOffsetY, topOffsetY, TRANSLATION_SPEED);

            topRenderRotationY =
                    lerpClamped(topRenderRotationY, topRenderTargetRotationY, topRenderRotationSpeed);
            if (topRenderRotationY == topRenderTargetRotationY) {
                topRenderTargetRotationY =
                        remapFrom01To(random.nextFloat(), -MAX_ROTATION, MAX_ROTATION);
                topRenderRotationSpeed =
                        remapFrom01To(random.nextFloat(), MIN_ROTATION_SPEED, MAX_ROTATION_SPEED);
            }
        } else {
            topRenderOffsetY = lerpClamped(topRenderOffsetY, TOP_IDLE_Y, TRANSLATION_SPEED * 2);
            baseRenderOffsetY = lerpClamped(baseRenderOffsetY, BASE_IDLE_Y, TRANSLATION_SPEED);
            topRenderRotationY = lerpClamped(topRenderRotationY, 0, ROTATION_SPEED);
        }
    }

    private static float lerpClamped(final float from, final float to, final float delta) {
        if (from < to) {
            return Math.min(from + delta, to);
        } else if (from > to) {
            return Math.max(from - delta, to);
        } else {
            return from;
        }
    }

    private static float remapFrom01To(final float x, final float a1, final float b1) {
        if (a1 == b1) {
            return a1;
        } else {
            return x * (b1 - a1) + a1;
        }
    }
}