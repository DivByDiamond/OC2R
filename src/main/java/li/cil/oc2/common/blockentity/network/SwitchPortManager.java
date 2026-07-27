package li.cil.oc2.common.blockentity.network;

import java.util.List;
import java.util.Map;
import li.cil.oc2.common.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

final class SwitchPortManager {
    final PortSettings[] portSettings = new PortSettings[Constants.BLOCK_FACE_COUNT];

    SwitchPortManager() {
        for (int i = 0; i < portSettings.length; i++) {
            portSettings[i] = new PortSettings();
        }
    }

    PortSettings[] getPortSettings() {
        return portSettings.clone();
    }

    void setPortSettings(List<Map> settings) {
        int max = Math.min(portSettings.length, settings.size());
        for (int i = 0; i < max; i++) {
            portSettings[i].untagged = ((Double) settings.get(i).get("untagged")).shortValue();
        }
    }

    void save(List<Tag> ports) {
        for (PortSettings myPort : portSettings) {
            CompoundTag port = new CompoundTag();
            myPort.save(port);
            ports.add(port);
        }
    }

    void load(List<Tag> ports) {
        int i = 0;
        for (Tag port : ports) {
            portSettings[i++] = PortSettings.load((CompoundTag) port);
        }
    }
}