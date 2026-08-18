package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.escapes.EscapeUtilities;
import li.cil.oc2.common.vm.terminal.escapes.index.IND;
import li.cil.oc2.common.vm.terminal.escapes.index.NEL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CSIManager {
    private static final Logger LOGGER = LogManager.getLogger();

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
    private boolean hasArg = false;

    private final Terminal terminal;
    private final Map<Character, CSISequenceHandler> sequences = new ConcurrentHashMap<>();

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

        sequences.put('x', new DECREQTPARM(terminal));

        sequences.put('@', new CH11(terminal));
    }

    public void handle(final char ch) {
        /* Control characters inside CSI sequences: execute them but don't terminate the sequence */
        if (ch < 0x20 || ch == 0x7F) {
            switch (ch) {
                case 0x08 -> { /* BS */
                    if (terminal.x > 0) terminal.x--;
                }
                case 0x0D -> { /* CR */
                    terminal.x = 0;
                }
                case 0x0A, 0x0B -> { /* LF / VT — respect LNM like normal path */
                    if (terminal.currentModeState.LNM) {
                        NEL.execute(terminal);
                    } else {
                        IND.execute(terminal);
                    }
                }
                case 0x09 -> { /* HT */
                    terminal.x = Math.min(terminal.x + 8 - (terminal.x % 8), Terminal.WIDTH - 1);
                }
                case 0x18, 0x1A -> { /* CAN / SUB — abort CSI sequence */
                    reset();
                    terminal.state = Terminal.State.NORMAL;
                    return;
                }
                case 0x1B -> { /* ESC — abort current CSI, start new escape */
                    reset();
                    terminal.state = Terminal.State.ESCAPE;
                    return;
                }
                default -> {} /* Other control chars: ignore, stay in CSI state */
            }
            return;
        }
        if (ch >= '0' && ch <= '9') {
            if (argCount < args.length) {
                hasArg = true;
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
                    argCount = Math.min(argCount + 1, args.length);
                    hasArg = true;
                    return; // Keep going, we have another argument.
                }
                default -> {}
            }

            if (hasArg) argCount = Math.min(argCount + 1, args.length);

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
                int[] defaults = handler.defaultParameters(state);
                if (defaults.length > 0) {
                    final int count = Math.min(defaults.length, args.length);
                    for (int i = 0; i < count; i++) {
                        if (args[i] == 0) {
                            args[i] = defaults[i];
                        }
                    }
                }
                handler.execute(args, argCount, state);
            } else {
                LOGGER.warn("Control sequence: {}", ch);
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
        exclamation = false;
        hasArg = false;
        argCount = 0;
        Arrays.fill(args, 0);
    }
}
