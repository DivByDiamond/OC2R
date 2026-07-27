package li.cil.oc2.common.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientSpec {
    public static final ModConfigSpec CLIENT_CONFIG_SPEC;
    private static final GUISpec guiSpec;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // GUI CONFIGURATION //
        builder.push("gui");
        guiSpec = new GUISpec(builder);
        builder.pop();

        CLIENT_CONFIG_SPEC = builder.build();
    }

    public static void loadValues() {
        // GUI CONFIGURATION //
        guiSpec.loadValues();
    }
}