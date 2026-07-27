package li.cil.oc2.common.vm.terminal.escapes;

public class EscapeUtilities {
    public static int parseArgument(final char ch, final int currentValue) {
        final int digit = ch - '0';
        int result;
        if (currentValue < (Integer.MAX_VALUE - digit) / 10) {
            result = currentValue * 10 + digit;
        } else {
            result = Integer.MAX_VALUE;
        }
        return result;
    }
}
