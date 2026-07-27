package li.cil.oc2.common.vm.terminal.modes;

@SuppressWarnings("unused")
public final class PrivateMode {
    public static final int DECCKM = 1; // Cursor key
    public static final int DECANM = 2; // ANSI/VT52
    public static final int DECCOLM = 3; // Column
    public static final int DECSCLM = 4; // Scrolling
    public static final int DECSCNM = 5; // Screen
    public static final int DECOM = 6; // Origin
    public static final int DECAWM = 7; // Auto wrap
    public static final int DECARM = 8; // Auto-repeating
    public static final int X10MM = 9; // X10 Mouse Cursor
    public static final int TOOLBAR = 10; // Toolbar
    public static final int START_BLINKING_CURSOR = 12; // SBC
    public static final int START_BLINKING_CURSOR2 = 13; // SBC2
    public static final int XORBLINK = 14; // XOR of blinking cursor control sequence
    public static final int DECPFF = 18; // Print Form Feed
    public static final int DECPEX = 19; // Set print extent to full screen
    public static final int DECTCEM = 25; // Show Cursor
    public static final int SHOW_SCROLL = 30; // Show Scrollbar
    public static final int FONT_SHIFT = 35; // Enable font-shifting
    public static final int TEKTRONIX = 38; // Enable Tektronix
    public static final int ENABLE_80_132 = 40; // Allow 80 -> 132 mode
    public static final int MORE_FIX = 41; // More(1) fix
    public static final int DECNRCM = 42; // Enable National Replacement Character sets
    public static final int DECGEPM = 43; // Enable Graphic Print Color Mode
    public static final int MARG_BELL = 44; // Enable margin bell
    public static final int XTREVWRAP = 45; // Enable reverse wrap around
    public static final int XTLOGGING = 46; // Enable logging
    public static final int ALT_BUFFER = 47; // Use alternate screen buffer
    public static final int DECNKM = 66; // Application keypad mode
    public static final int DECBKM = 67; // Backarrow key sends backspace
    public static final int DECLRMM = 69; // Enable left and right margin mode
    public static final int DECSDM = 80; // Enable Sixel Display Mode
    public static final int DECNCSM = 96; // Do not clear on DECCOLM set/reset
    public static final int X11MM = 1000; // X11 Mouse Mode
    public static final int HILITE_MOUSE = 1001; // Hilite Mouse Mode
    public static final int CELL_MOTION_MOUSE = 1002; // Cell Motion Mouse Mode
    public static final int ALL_MOTION_MOUSE_TRACKING = 1003; // All Motion Mouse Tracking Mode
    public static final int FOCUS_IN_FOCUS_OUT = 1004; // Focus In/Out events
    public static final int UTF8_MOUSE = 1005; // UTF-8 Mouse Mode
    public static final int SGR_MOUSE = 1006; // SGR Mouse Mode
    public static final int ALTERNATE_SCROLL_MODE = 1007;
    public static final int SCROLL_BOTTOM_ON_OUTPUT = 1010; // Scroll to bottom on output
    public static final int SCROLL_BOTTOM_ON_KEY_PRESS = 1011; // Scroll to bottom when key pressed
    public static final int FAST_SCROLL = 1014; // Fast scroll
    public static final int URXVT_MOUSE = 1015;
    public static final int SGR_MOUSE_PIXEL = 1016;
    public static final int META_KEY =
            1034; // Interpret "meta" key (Zero Affect in our implementation as this would typically
    // mess with the 8th bit of input in a 7bit setup, but we always send 8)
    public static final int SPECIAL_MODIFIERS = 1035; // Enable special alt and numlock modifiers
    public static final int META_SENDS_ESCAPE = 1036; // Meta sends ESC when pressed
    public static final int DEL_EDIT_KEYPAD_DEL = 1037; // Send DEL from editing-keypad DEL key
    public static final int ALT_SENDS_ESC = 1039; // Alt sends ESC when pressed
    public static final int KEEP_SELECTION = 1040; // Keeps selection even if not highlighted
    public static final int USE_CLIP = 1041; // Use Clipboard selection
    public static final int ENABLE_URGENCY = 1042; // Bell will be urgent on CTRL-G
    public static final int RAISE_ON_CTRL_G = 1043; // NO FUNCTION
    public static final int KEEP_CLIP = 1044; // Keep most recent clipboard
    public static final int EXT_REV_WRAP = 1045; // Extended reverse wrap mode
    public static final int ALLOW_ALT_BUFFER = 1046; // Enable to and from alt buffer
    public static final int SWITCH_ALT_BUFFER = 1047; // Switch to alt buffer
    public static final int SAVE_CURSOR = 1048; // Save cursor as in DECSC
    public static final int SAVE_CLEAR_AND_SWITCH = 1049; // Combines 1047 and 1048
    public static final int SET_TERMINFO_FUNC_KEY_MODE =
            1050; // Set terminfo/termcap function-key mode
    public static final int SET_SUN_KEY_MODE = 1051; // Set Sun key mode
    public static final int SET_HP_K0EY_MODE = 1052; // Set HP function-key mode
    public static final int SET_SCO_KEY_MODE = 1053; // Set SCO key mode
    public static final int SET_LEGACY_KEYBOARD = 1060; // Emulate legacy keyboard
    public static final int SET_VT220_KEYBOARD = 1061; // Emulate VT220 keyboard
    public static final int ENABLE_READLINE_MOUSE_1 = 2001; // Readline mouse button-1
    public static final int ENABLE_READLINE_MOUSE_2 = 2002; // Readline mouse button-2
    public static final int ENABLE_READLINE_MOUSE_3 = 2003; // Readline mouse button-3
    public static final int SET_BRACKETED_PASTE = 2004; // Set bracketed paste mode
    public static final int ENABLE_READLINE_CHAR_QUOTE = 2005; // Enable readline character quoting
    public static final int ENABLE_READLINE_NEWLINE_PASTE = 2006; // Enable readline newline pasting
    public static final int APPLICATION_SYNC = 2026; // Wait to render until application is ready
    public static final int APPLICATION_ESC_MODE = 7727; // Send escape key as ^[0[
}
