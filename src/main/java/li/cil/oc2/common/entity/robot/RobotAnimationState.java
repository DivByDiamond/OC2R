package li.cil.oc2.common.entity.robot;

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

    public float topRenderOffsetY = TOP_IDLE_Y;
    public float baseRenderOffsetY = BASE_IDLE_Y;
    public float topRenderRotationY;
    public float topRenderTargetRotationY;
    public float topRenderRotationSpeed;
    public float topRenderHover;

    private final VirtualMachine virtualMachine;
    private final Supplier<Boolean> hasQueuedActions;

    public RobotAnimationState(
            final VirtualMachine virtualMachine, final Supplier<Boolean> hasQueuedActions) {
        this.virtualMachine = virtualMachine;
        this.hasQueuedActions = hasQueuedActions;
        topRenderHover = -(hashCode() & 0xFFFF);
    }

    public void update(final float deltaTime, final RandomSource random) {
        if (virtualMachine.isRunning() || hasQueuedActions.get()) {
            topRenderHover = topRenderHover + deltaTime * HOVER_ANIMATION_SPEED;
            final float topOffsetY = Mth.sin(topRenderHover) / 32f;

            topRenderOffsetY =
                    lerpClamped(topRenderOffsetY, topOffsetY, deltaTime * TRANSLATION_SPEED);
            baseRenderOffsetY =
                    lerpClamped(baseRenderOffsetY, topOffsetY, deltaTime * TRANSLATION_SPEED);

            topRenderRotationY =
                    lerpClamped(
                            topRenderRotationY,
                            topRenderTargetRotationY,
                            deltaTime * topRenderRotationSpeed);
            if (topRenderRotationY == topRenderTargetRotationY) {
                topRenderTargetRotationY =
                        remapFrom01To(random.nextFloat(), -MAX_ROTATION, MAX_ROTATION);
                topRenderRotationSpeed =
                        remapFrom01To(random.nextFloat(), MIN_ROTATION_SPEED, MAX_ROTATION_SPEED);
            }
        } else {
            topRenderOffsetY =
                    lerpClamped(topRenderOffsetY, TOP_IDLE_Y, deltaTime * TRANSLATION_SPEED * 2);
            baseRenderOffsetY =
                    lerpClamped(baseRenderOffsetY, BASE_IDLE_Y, deltaTime * TRANSLATION_SPEED);

            topRenderRotationY = lerpClamped(topRenderRotationY, 0, deltaTime * ROTATION_SPEED);
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