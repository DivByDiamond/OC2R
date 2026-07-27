package li.cil.oc2.common.blockentity.projector;

import li.cil.oc2.jcodec.common.model.Picture;

@FunctionalInterface
public interface FrameConsumer {
    void processFrame(final Picture picture);
}
