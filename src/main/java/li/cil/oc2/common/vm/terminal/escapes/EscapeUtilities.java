package li.cil.oc2.common.vm.terminal.escapes;

public class EscapeUtilities {
    public static int parseArgument(final char ch, int currentValue) {
        final int digit = ch - '0';
        if (currentValue < (Integer.MAX_VALUE - digit) / 10) {
            currentValue = currentValue * 10 + digit;
        } else {
            currentValue = Integer.MAX_VALUE;
        }
        return currentValue;
    }
}
