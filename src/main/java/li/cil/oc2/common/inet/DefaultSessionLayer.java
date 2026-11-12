package li.cil.oc2.common.inet;

import org.jetbrains.annotations.Nullable;

/**
 * Binding point of the native ICMP echo.
 *
 * <p>All eight bundled {@code liboc2rnet} binaries (Linux, macOS, Windows and Android) export the entry point
 * as {@code Java_li_cil_oc2_common_inet_DefaultSessionLayer_sendICMP}, so the {@code native} method has to live
 * in a class with exactly this name. It sat in the real session layer until that class moved to
 * {@code inet.layer.impl}; from then on the symbol was never found and every ping silently used the fallback.
 */
public final class DefaultSessionLayer {
    private DefaultSessionLayer() {}

    /** Sends one echo request over a raw socket; null if the host did not answer or raw sockets are not allowed. */
    public static native byte @Nullable [] sendICMP(byte[] ip, byte[] data, int size, int timeout);
}
