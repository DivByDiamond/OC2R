package li.cil.oc2.common.config.client;

import li.cil.oc2.common.config.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class GUISpec {
    public final ModConfigSpec.EnumValue<CaptureInputMode> captureInputMode;
    public final ModConfigSpec.BooleanValue captureInputDefaultState;
    public final ModConfigSpec.DoubleValue monitorBorder;

    GUISpec(ModConfigSpec.Builder builder) {
        captureInputMode =
                builder.comment(
                                "The option below changes the behavior of the capture input"
                                        + " feature:",
                                "PER_BLOCK - The capture input value is saved between UI opens on a"
                                        + " per computer/monitor/robot basis",
                                "SHARED_BETWEEN_TYPE - The capture input value is saved between UI"
                                        + " opens and is shared between all",
                                "blocks of the same type, e.g. enabling the setting on one monitor"
                                        + " will enable it for all monitors but not for a computer",
                                "GLOBAL_CAPTURE - The capture input value is saved between UI opens"
                                        + " and is shared between all devices that have the option")
                        .defineEnum("captureInputMode", CaptureInputMode.PER_BLOCK);

        captureInputDefaultState =
                builder.comment(
                                "Defines whether input capture should be enabled by default in a"
                                        + " session")
                        .define("captureInputDefaultState", false);

        monitorBorder =
                builder.comment(
                                "Width of the monitor bezel/border in block pixels (1 px = 1 unit"
                                        + " in the renderer's normalized screen space). The visible"
                                        + " screen area is (16 - 2*border) x (16 - 2*border), which"
                                        + " the terminal framebuffer is fitted into.",
                                "Higher values make the terminal text smaller/more inset; lower"
                                        + " values make it fill more of the screen.")
                        .defineInRange("monitorBorder", 2.0, 0.0, 7.0);
    }

    public enum CaptureInputMode {
        PER_BLOCK,
        SHARED_BETWEEN_TYPE,
        GLOBAL_CAPTURE
    }

    public void loadValues() {
        Config.captureInputMode = captureInputMode.get();
        Config.captureInputDefaultState = captureInputDefaultState.get();
        Config.monitorBorder = monitorBorder.get().floatValue();
    }
}