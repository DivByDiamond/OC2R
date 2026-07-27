package li.cil.oc2.common.inet.tcp;
import li.cil.oc2.common.inet.tcp.state.AcceptState;
import li.cil.oc2.common.inet.tcp.state.ConnectState;
import li.cil.oc2.common.inet.tcp.state.EstablishedState;
import li.cil.oc2.common.inet.tcp.state.ExpiredState;
import li.cil.oc2.common.inet.tcp.state.FinishState;
import li.cil.oc2.common.inet.tcp.state.RejectState;

public final class TcpStates {
    static final TcpState CONNECT = new ConnectState();
    static final TcpState ACCEPT = new AcceptState();
    static final TcpState REJECT = new RejectState();
    static final TcpState ESTABLISHED = new EstablishedState();
    static final TcpState FINISH = new FinishState();
    static final TcpState EXPIRED = new ExpiredState();

    private TcpStates() {}
}
