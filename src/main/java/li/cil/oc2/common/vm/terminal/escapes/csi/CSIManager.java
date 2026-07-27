package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.escapes.EscapeUtilities;

import java.util.Arrays;
import java.util.HashMap;

public class CSIManager {
    private final int[] args = new int[10];
    private int argCount = 0;
    private boolean questionMark = false;
    private boolean greaterThan = false;
    private boolean dollarSign = false;
    private boolean hash = false;
    private boolean quote = false;
    private boolean singleQuote = false;
    private boolean space = false;
    private boolean exclamation = false;

    private final Terminal terminal;
    private final HashMap<Character, CSISequenceHandler> sequences = new HashMap<>();

    public CSIManager(Terminal terminal) {
        this.terminal = terminal;

        sequences.put('A', new CUU(terminal));
        sequences.put('B', new CUD(terminal));
        sequences.put('C', new CUF(terminal));
        sequences.put('D', new CUB(terminal));
        sequences.put('G', new CHA(terminal));
        sequences.put('H', new CUP(terminal));
        sequences.put('J', new ED(terminal));
        sequences.put('K', new EL(terminal));
        sequences.put('L', new IL(terminal));
        sequences.put('M', new DL(terminal));
        sequences.put('P', new CH10(terminal));
        sequences.put('S', new CH8(terminal));
        sequences.put('T', new CH9(terminal));
        sequences.put('X', new ECH(terminal));

        sequences.put('c', new DA(terminal));
        sequences.put('d', new VPA(terminal));
        sequences.put('f', new HVP(terminal));
        sequences.put('g', new TBC(terminal));
        sequences.put('h', new CH2(terminal));
        sequences.put('l', new CH3(terminal));
        sequences.put('m', new SGR(terminal));
        sequences.put('n', new DSR(terminal));
        sequences.put('p', new CH5(terminal));
        sequences.put('q', new CH7(terminal));
        sequences.put('r', new CH1(terminal));
        sequences.put('s', new CH6(terminal));
        sequences.put('t', new CH4(terminal));

        sequences.put('@', new CH11(terminal));
    }

    public void handle(final char ch) {
        if (ch >= '0' && ch <= '9') {
            if (argCount < args.length) {
                args[argCount] = EscapeUtilities.parseArgument(ch, args[argCount]);
            }
        } else {
            switch (ch) {
                case ' ' -> {
                    space = true;
                    return;
                }
                case '?' -> {
                    questionMark = true;
                    return;
                }
                case '>' -> {
                    greaterThan = true;
                    return;
                }
                case '$' -> {
                    dollarSign = true;
                    return;
                }
                case '#' -> {
                    hash = true;
                    return;
                }
                case '"' -> {
                    quote = true;
                    return;
                }
                case '\'' -> {
                    singleQuote = true;
                    return;
                }
                case '!' -> {
                    exclamation = true;
                    return;
                }
                case ';' -> {
                    argCount++;
                    return; // Keep going, we have another argument.
                }
                default -> argCount++;
            }

            terminal.state = Terminal.State.NORMAL;

            CSISequenceHandler handler = sequences.get(ch);
            CSIState state =
                    new CSIState(
                            questionMark,
                            greaterThan,
                            dollarSign,
                            hash,
                            quote,
                            singleQuote,
                            space,
                            exclamation);

            if (handler != null) {
                handler.execute(args, argCount, state);
            } else {
                System.out.println("Control sequence: " + ch);
            }
        }
    }

    public void reset() {
        questionMark = false;
        greaterThan = false;
        dollarSign = false;
        hash = false;
        quote = false;
        singleQuote = false;
        space = false;
        argCount = 0;
        Arrays.fill(args, 0);
    }
}
