package li.cil.oc2.common.util.sound;

import java.util.function.LongSupplier;

/**
 * Server side model of one speaker's playback queue.
 *
 * <p>The server cannot see the client's sound buffer, so it assumes that audio plays back in real time
 * from the moment it is sent. This tells a guest how much audio is still queued and lets the speaker refuse
 * more while the queue is long, instead of flooding every client that tracks the chunk.
 */
public final class AudioPlayback {
    /** How much audio may be queued ahead of the playback position. */
    public static final long MAX_BUFFER_MS = 2000;
    /** After this much silence the next write is a new stream and the decoder starts from scratch. */
    public static final long IDLE_RESET_MS = 1000;
    /** Largest DFPWM chunk accepted by one call, about 1.5 seconds of audio. */
    public static final int MAX_DFPWM_BYTES = 8192;

    private static final byte[] NOTHING = new byte[0];
    private static final long NANOS_PER_MS = 1_000_000L;
    private static final int PCM_BYTES_PER_SECOND = DfpwmDecoder.OUTPUT_RATE * 2;

    private final LongSupplier clock;
    private final DfpwmDecoder decoder = new DfpwmDecoder();
    private long playheadNanos;

    public AudioPlayback() {
        this(System::nanoTime);
    }

    public AudioPlayback(final LongSupplier nanoClock) {
        this.clock = nanoClock;
        this.playheadNanos = nanoClock.getAsLong();
    }

    /** Milliseconds of audio that have been sent but would not have finished playing yet. */
    public synchronized int bufferedMs() {
        return (int) Math.max(0, (playheadNanos - clock.getAsLong()) / NANOS_PER_MS);
    }

    /**
     * Decodes a DFPWM chunk.
     *
     * @return the PCM to send to the clients, or an empty array if the queue is too long and nothing was consumed
     */
    public synchronized byte[] decodeDfpwm(final byte[] data, final int sampleRate) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("no audio data");
        }
        if (data.length > MAX_DFPWM_BYTES) {
            throw new IllegalArgumentException("at most " + MAX_DFPWM_BYTES + " bytes of DFPWM per call");
        }
        final int pcmLength = DfpwmDecoder.pcmLength(data.length, sampleRate);
        if (pcmNanos(pcmLength) > MAX_BUFFER_MS * NANOS_PER_MS) {
            throw new IllegalArgumentException("chunk is longer than " + MAX_BUFFER_MS + " ms of audio");
        }
        if (!tryQueue(pcmLength)) {
            return NOTHING;
        }
        return decoder.decode(data, sampleRate);
    }

    /** Accounts for raw PCM (44.1 kHz, 16-bit mono); returns whether the queue had room. */
    public synchronized boolean acceptPcm(final int length) {
        if (pcmNanos(length) > MAX_BUFFER_MS * NANOS_PER_MS) {
            throw new IllegalArgumentException("chunk is longer than " + MAX_BUFFER_MS + " ms of audio");
        }
        return tryQueue(length);
    }

    /** Counts audio that was admitted elsewhere (for example under a byte budget) without refusing it. */
    public synchronized void notePcm(final int length) {
        advance();
        playheadNanos += pcmNanos(length);
    }

    private boolean tryQueue(final int pcmLength) {
        advance();
        final long added = pcmNanos(pcmLength);
        final long now = clock.getAsLong();
        if (playheadNanos - now + added > MAX_BUFFER_MS * NANOS_PER_MS) {
            return false;
        }
        playheadNanos += added;
        return true;
    }

    private void advance() {
        final long now = clock.getAsLong();
        if (playheadNanos < now) {
            if (now - playheadNanos > IDLE_RESET_MS * NANOS_PER_MS) {
                decoder.reset();
            }
            playheadNanos = now;
        }
    }

    private static long pcmNanos(final int length) {
        return length * 1_000_000_000L / PCM_BYTES_PER_SECOND;
    }
}
