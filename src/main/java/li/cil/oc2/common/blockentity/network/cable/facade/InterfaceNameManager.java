package li.cil.oc2.common.blockentity.network.cable.facade;

import java.util.List;
import java.util.Objects;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.network.BusInterfaceNameMessage;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class InterfaceNameManager {
    private final BusCableBlockEntity owner;
    private final String[] interfaceNames = new String[Constants.BLOCK_FACE_COUNT];

    public InterfaceNameManager(final BusCableBlockEntity owner) {
        this.owner = owner;
    }

    public String getInterfaceName(final Direction side) {
        final String interfaceName = interfaceNames[side.get3DDataValue()];
        return interfaceName == null ? "" : interfaceName;
    }

    public void setInterfaceName(final Direction side, final String name) {
        final var level = owner.getLevel();
        if (level == null) {
            return;
        }

        final String validatedName = validateName(name);
        if (Objects.equals(validatedName, interfaceNames[side.get3DDataValue()])) {
            return;
        }

        interfaceNames[side.get3DDataValue()] = validatedName;
        owner.setChanged();

        if (!level.isClientSide()) {
            final BusInterfaceNameMessage message =
                    BusInterfaceNameMessage.toClient(
                            owner, side, interfaceNames[side.get3DDataValue()]);
            NetworkMessages.sendToClientsTrackingBlockEntity(message, owner);
            owner.busElement.updateDevicesForNeighbor(side);
        }
    }

    public List<Tag> serialize() {
        final List<Tag> tag = new ListTag();
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            tag.add(StringTag.valueOf(getInterfaceName(Direction.from3DDataValue(i))));
        }
        return tag;
    }

    public void deserialize(final List<Tag> tag) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final String name = ((StringTag) tag.get(i)).getAsString().trim();
            interfaceNames[i] = name.substring(0, Math.min(32, name.length()));
        }
    }

    private static String validateName(final String name) {
        final String trimmed = name.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }
}