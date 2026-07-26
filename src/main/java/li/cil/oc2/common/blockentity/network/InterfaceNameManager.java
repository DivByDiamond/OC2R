package li.cil.oc2.common.blockentity.network;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.Objects;

final class InterfaceNameManager {
    private final BusCableBlockEntity owner;
    private final String[] interfaceNames = new String[Constants.BLOCK_FACE_COUNT];

    InterfaceNameManager(final BusCableBlockEntity owner) {
        this.owner = owner;
    }

    String getInterfaceName(final Direction side) {
        final String interfaceName = interfaceNames[side.get3DDataValue()];
        return interfaceName == null ? "" : interfaceName;
    }

    void setInterfaceName(final Direction side, final String name) {
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
            final BusInterfaceNameMessage message = BusInterfaceNameMessage.ToClient(owner, side, interfaceNames[side.get3DDataValue()]);
            Network.sendToClientsTrackingBlockEntity(message, owner);
            owner.busElement.updateDevicesForNeighbor(side);
        }
    }

    ListTag serialize() {
        final ListTag tag = new ListTag();
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            tag.add(StringTag.valueOf(getInterfaceName(Direction.from3DDataValue(i))));
        }
        return tag;
    }

    void deserialize(final ListTag tag) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final String name = tag.getString(i).trim();
            interfaceNames[i] = name.substring(0, Math.min(32, name.length()));
        }
    }

    private static String validateName(final String name) {
        final String trimmed = name.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }
}
