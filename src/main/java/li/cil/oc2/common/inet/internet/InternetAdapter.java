package li.cil.oc2.common.inet.internet;

import javax.annotation.Nullable;

public interface InternetAdapter {
    @Nullable
    byte[] receiveEthernetFrame();

    void sendEthernetFrame(byte[] frame);
}