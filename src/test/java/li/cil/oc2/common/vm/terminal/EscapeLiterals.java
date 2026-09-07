package li.cil.oc2.common.vm.terminal;

// Shared VT/ANSI escape-sequence literals for terminal tests.
//
// Composable pieces (CSI = ESC + "[") so test sequences read as assemblies of named parts and
// stay below PMD's AvoidDuplicateLiterals threshold. One source of truth so every terminal test
// (TerminalBufferTest, SGRTest, StringSequenceTest, ...) names the same bytes.
//
// Written via python rather than the Edit/Write tools: those decode the unicode-escape and
// backslash-escape literals in the payload into control characters instead of writing them as
// source text.
public final class EscapeLiterals {
    private EscapeLiterals() {
    }

    // Sequence introducers: ESC followed by the introducer byte.
    public static final String ESC = "\u001b";
    public static final String CSI = ESC + "[";
    public static final String OSC = ESC + "]";
    public static final String DCS = ESC + "P";
    public static final String APC = ESC + "_";

    // String Terminator (ST): ESC + backslash. Ends OSC/DCS/APC strings.
    public static final String ST = ESC + "\\";

    // String/data terminators and aborts.
    public static final String BEL = "\u0007"; // also an OSC terminator (xterm CASE_BELL, OSC path)
    public static final String CAN = "\u0018"; // cancel: aborts the current sequence (xterm CASE_CAN)
    public static final String SUB = "\u001a"; // substitute: aborts the current sequence (xterm CASE_SUB)

    // A lone backslash as content (not preceded by ESC) -- distinct from ST, which is ESC + backslash.
    public static final String BACKSLASH = "\\";
}
