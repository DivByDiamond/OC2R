package li.cil.oc2.common.blockentity.monitor;

import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.HEIGHT;
import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.WIDTH;
import static li.cil.oc2.common.vm.device.SimpleFramebufferDevice.STRIDE;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import javax.annotation.Nullable;
import li.cil.oc2.jcodec.codecs.h264.H264Decoder;
import li.cil.oc2.jcodec.common.model.Picture;

final class MonitorVideoDecoder {

    private final ReentrantLock lock = new ReentrantLock();

    private final H264Decoder decoder = new H264Decoder();
    private final ByteBuffer decoderBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * STRIDE);
    @Nullable private CompletableFuture<?> runningDecode;

    void applyNextFrameClient(
            final ByteBuffer frameData,
            final Picture picture,
            @Nullable final FrameConsumer frameConsumer) {
        final CompletableFuture<?> lastDecode = runningDecode;
        runningDecode =
                CompletableFuture.runAsync(
                        () -> {
                            try {
                                try {
                                    if (lastDecode != null) lastDecode.join();
                                } catch (final CompletionException ignored) {
                                }

                                final Inflater inflater = new Inflater();
                                inflater.setInput(frameData);

                                decoderBuffer.clear();
                                inflater.inflate(decoderBuffer);
                                decoderBuffer.flip();

                                decoder.decodeFrame(decoderBuffer, picture.getData());

                                lock.lock();
                                try {

                                    if (frameConsumer != null) {
                                        frameConsumer.processFrame(picture);
                                    }
                                
                                } finally {
                                    lock.unlock();
                                }
                            } catch (final DataFormatException ignored) {
                            }
                        },
                        MonitorDecoderWorkers.INSTANCE);
    }
}