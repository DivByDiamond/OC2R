package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public abstract class CSISequenceHandler {
    protected Terminal terminal;

    public CSISequenceHandler(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Per-parameter defaults applied by {@link CSIManager} before {@link #execute}: an omitted or
     * zero parameter is replaced with the value of the matching slot. The default depends on the
     * control function and on the CSI modifiers (DECSTBM vs XTRESTORE both live on 'r'), so there
     * is no single rule in CSIManager — each handler declares its own.
     *
     * <p>Return an empty array when parameters carry no defaults and must be read as-is, e.g. mode
     * numbers in DECSET/DECRST ({@link CH2}/{@link CH3}) where {@code 0} is an empty slot, or
     * DECSCUSR ({@link CH7}) where {@code 0} is a valid cursor style.
     *
     * @param state the CSI modifiers (?, &gt;, #, etc.) of the current sequence.
     * @return per-slot default values, or an empty array if no normalization applies.
     */
    public int[] defaultParameters(CSIState state) {
        return new int[0];
    }

    public abstract void execute(int[] args, int argsCount, CSIState state);
}