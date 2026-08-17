package li.cil.oc2.common.integration.ic2;

public interface EuEnergyAdapter {
    int pushEu(int maxEu, boolean simulate);

    int pullEu(int maxEu, boolean simulate);
}