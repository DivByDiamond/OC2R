package li.cil.oc2.common.block;

import net.minecraft.util.StringRepresentable;

public enum ConnectionType implements StringRepresentable {
    NONE,
    CABLE,
    INTERFACE;

    @Override
    public String getSerializedName() {
        return switch (this) {
            case NONE -> "none";
            case CABLE -> "cable";
            case INTERFACE -> "interface";
        };
    }
}
