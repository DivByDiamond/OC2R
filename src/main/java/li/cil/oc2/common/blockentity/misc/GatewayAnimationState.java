package li.cil.oc2.common.blockentity.misc;

public class GatewayAnimationState {
    public static final int EMITTER_SIDE_PIXELS = 4;
    public float animProgress[];
    public boolean animReversed[];
    public int inboundCount = 0;
    public int outboundCount = 0;
    public int handledInboundCount = 0;
    public int handledOutboundCount = 0;
    public long lastRender = 0;
    public int pointer = 0;

    public GatewayAnimationState() {
        animProgress = new float[EMITTER_SIDE_PIXELS * EMITTER_SIDE_PIXELS];
        animReversed = new boolean[EMITTER_SIDE_PIXELS * EMITTER_SIDE_PIXELS];
    }
}
