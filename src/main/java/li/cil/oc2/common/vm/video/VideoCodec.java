package li.cil.oc2.common.vm.video;

public enum VideoCodec {
    RAW(0),
    H264(1),
    DELTA(2);

    public final int id;

    VideoCodec(final int id) {
        this.id = id;
    }

    public static VideoCodec fromId(final int id) {
        return switch (id) {
            case 0 -> RAW;
            case 1 -> H264;
            case 2 -> DELTA;
            default -> RAW;
        };
    }
}
