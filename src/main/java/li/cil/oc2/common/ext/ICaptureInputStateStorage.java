package li.cil.oc2.common.ext;

public interface ICaptureInputStateStorage {
    boolean getCaptureInputState(); // NOPMD getter API implemented across many BEs/containers

    void setCaptureInputState(boolean value);
}
