package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH4
        extends CSISequenceHandler { // Combined Handler 4 (XTWINOPS, XTSMTITLE, DECSWBV, and
    // DECRARA)
    public CH4(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.greaterThan) { // XTSMTITLE
            System.out.println("XTSMTITLE is not implemented");
        } else if (state.space) { // DECSWBV
            System.out.println("DECSWBV is not implemented yet");
        } else if (state.dollarSign) { // DECRARA
            System.out.println("DECRARA is not implemented");
        } else { // XTWINOPS
            switch (args[0]) {
                case 14 ->
                        terminal.io.putResponse(
                                "\033[4;"
                                        + (Terminal.HEIGHT * Terminal.CHAR_HEIGHT)
                                        + ";"
                                        + (Terminal.WIDTH * Terminal.CHAR_WIDTH)
                                        + "t");
                case 15 ->
                        terminal.io.putResponse(
                                "\033[5;"
                                        + (Terminal.HEIGHT * Terminal.CHAR_HEIGHT)
                                        + ";"
                                        + (Terminal.WIDTH * Terminal.CHAR_WIDTH)
                                        + "t");
                case 16 ->
                        terminal.io.putResponse(
                                "\033[6;" + Terminal.CHAR_HEIGHT + ";" + Terminal.CHAR_WIDTH + "t");
                case 18 ->
                        terminal.io.putResponse(
                                "\033[8;" + Terminal.HEIGHT + ";" + Terminal.WIDTH + "t");
                case 19 ->
                        terminal.io.putResponse(
                                "\033[9;" + Terminal.HEIGHT + ";" + Terminal.WIDTH + "t");
                default -> {}
            }
        }
    }
}
