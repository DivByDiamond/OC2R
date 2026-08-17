package li.cil.oc2.common.integration.ic2;

import javax.annotation.Nullable;

public final class Ic2EuBridge {
    public static final int FE_PER_EU = 4;

    private static Ic2EuBridge INSTANCE = null;

    @Nullable
    private EuEnergyAdapter adapter = null;

    private Ic2EuBridge() {}

    public static synchronized Ic2EuBridge getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Ic2EuBridge();
        }
        return INSTANCE;
    }

    public void register(final EuEnergyAdapter adapter) {
        this.adapter = adapter;
    }

    public boolean isAvailable() {
        return adapter != null;
    }

    public int pushEu(final int maxEu, final boolean simulate) {
        return adapter != null ? adapter.pushEu(maxEu, simulate) : 0;
    }

    public int pullEu(final int maxEu, final boolean simulate) {
        return adapter != null ? adapter.pullEu(maxEu, simulate) : 0;
    }
}