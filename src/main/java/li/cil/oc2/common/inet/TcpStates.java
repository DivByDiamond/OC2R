package li.cil.oc2.common.inet;

final class TcpStates {
    static final TcpState CONNECT = new ConnectState();
    static final TcpState ACCEPT = new AcceptState();
    static final TcpState REJECT = new RejectState();
    static final TcpState ESTABLISHED = new EstablishedState();
    static final TcpState FINISH = new FinishState();
    static final TcpState EXPIRED = new ExpiredState();

    private TcpStates() {
    }
}
