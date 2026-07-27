package li.cil.oc2.common.inet.tcp;
import li.cil.oc2.common.inet.tcp.state.AcceptState;
import li.cil.oc2.common.inet.tcp.state.ConnectState;
import li.cil.oc2.common.inet.tcp.state.EstablishedState;
import li.cil.oc2.common.inet.tcp.state.ExpiredState;
import li.cil.oc2.common.inet.tcp.state.FinishState;
import li.cil.oc2.common.inet.tcp.state.RejectState;

public final class TcpStates {
    public static final TcpState CONNECT = new ConnectState();
    public static final TcpState ACCEPT = new AcceptState();
    public static final TcpState REJECT = new RejectState();
    public static final TcpState ESTABLISHED = new EstablishedState();
    public static final TcpState FINISH = new FinishState();
    public static final TcpState EXPIRED = new ExpiredState();

    private TcpStates() {}
}
