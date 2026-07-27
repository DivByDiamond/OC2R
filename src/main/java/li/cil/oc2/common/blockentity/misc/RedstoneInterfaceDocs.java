package li.cil.oc2.common.blockentity.misc;

import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import net.neoforged.fml.ModList;

final class RedstoneInterfaceDocs {
    private static final String GET_REDSTONE_INPUT = "getRedstoneInput";
    private static final String GET_REDSTONE_OUTPUT = "getRedstoneOutput";
    private static final String SET_REDSTONE_OUTPUT = "setRedstoneOutput";
    private static final String GET_BUNDLED_INPUT = "getBundledInput";
    private static final String GET_BUNDLED_OUTPUT = "getBundledOutput";
    private static final String SET_BUNDLED_OUTPUT = "setBundledOutput";
    private static final String SET_BUNDLED_OUTPUTS = "setBundledOutputs";
    private static final String SIDE = "side";
    private static final String VALUE = "value";
    private static final String VALUES = "values";
    private static final String COLOUR = "colour";

    static void getDeviceDocumentation(final DocumentedDevice.DeviceVisitor visitor) {
        visitor.visitCallback(GET_REDSTONE_INPUT)
                .description(
                        "Get the current redstone level received on the specified side. Note that"
                            + " if the current output level on the specified side is not zero, this"
                            + " will affect the measured level.\n"
                            + "Sides may be specified by name or zero-based index. Please note that"
                            + " the side depends on the orientation of the device.")
                .returnValueDescription("the current received level on the specified side.")
                .parameterDescription(SIDE, "the side to read the input level from.");

        visitor.visitCallback(GET_REDSTONE_OUTPUT)
                .description(
                        "Get the current redstone level transmitted on the specified side. This"
                            + " will return the value last set via setRedstoneOutput().\n"
                            + "Sides may be specified by name or zero-based index. Please note that"
                            + " the side depends on the orientation of the device.")
                .returnValueDescription("the current transmitted level on the specified side.")
                .parameterDescription(SIDE, "the side to read the output level from.");

        visitor.visitCallback(SET_REDSTONE_OUTPUT)
                .description(
                        "Set the new redstone level transmitted on the specified side.\n"
                            + "Sides may be specified by name or zero-based index. Please note that"
                            + " the side depends on the orientation of the device.")
                .parameterDescription(SIDE, "the side to write the output level to.")
                .parameterDescription(
                        VALUE, "the output level to set, will be clamped to [0, 15].");

        if (ModList.get().isLoaded("projectred_transmission")) {
            visitor.visitCallback(GET_BUNDLED_INPUT)
                    .description("Get the current bundled level received on the specified side.")
                    .parameterDescription(SIDE, "the side to read the bundled input level from");

            visitor.visitCallback(GET_BUNDLED_OUTPUT)
                    .description("Get the current bundled level sent out on the specified side.")
                    .parameterDescription(SIDE, "the side to read the bundled output level from");

            visitor.visitCallback(SET_BUNDLED_OUTPUT)
                    .description(
                            "Set the new bundled level transmitted for a specific color on the"
                                + " specified side.\n"
                                + "Sides may be specified by name or zero-based index. Please note"
                                + " that the side depends on the orientation of the device.")
                    .parameterDescription(SIDE, "the side to write the output level to.")
                    .parameterDescription(
                            VALUE, "the output level to set, will be clamped to [0, 255].")
                    .parameterDescription(COLOUR, "the colour wire this sets, as int [0, 15]");

            visitor.visitCallback(SET_BUNDLED_OUTPUTS)
                    .description(
                            "Set the new bundled levels transmitted on the specified side.\n"
                                + "Sides may be specified by name or zero-based index. Please note"
                                + " that the side depends on the orientation of the device.")
                    .parameterDescription(SIDE, "the side to write the output level to.")
                    .parameterDescription(
                            VALUES,
                            "the output levels to set in array form, each value will be clamped to"
                                    + " [0, 255], 16 entries.");
        }
    }

    private RedstoneInterfaceDocs() {}
}