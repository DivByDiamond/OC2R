package li.cil.oc2.common.vm.terminal.escapes.csi;

public class CSIState {
    public boolean questionMark;
    public boolean greaterThan;
    public boolean dollarSign;
    public boolean hash;
    public boolean quote;
    public boolean singleQuote;
    public boolean space;
    public boolean exclamation;
    public boolean asterisk;

    public CSIState(
            boolean questionMark,
            boolean greaterThan,
            boolean dollarSign,
            boolean hash,
            boolean quote,
            boolean singleQuote,
            boolean space,
            boolean exclamation,
            boolean asterisk) {
        this.questionMark = questionMark;
        this.greaterThan = greaterThan;
        this.dollarSign = dollarSign;
        this.hash = hash;
        this.quote = quote;
        this.singleQuote = singleQuote;
        this.space = space;
        this.exclamation = exclamation;
        this.asterisk = asterisk;
    }
}
