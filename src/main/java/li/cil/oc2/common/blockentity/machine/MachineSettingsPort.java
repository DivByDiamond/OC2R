package li.cil.oc2.common.blockentity.machine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.integration.create.CreateControlSetting;
import li.cil.oc2.common.integration.create.CreateControlsDevice;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only state inspection and explicitly supported, typed machine settings. */
public interface MachineSettingsPort {
    MachineInterfaceBlockEntity machine();

    @Callback
    default String getTargetBlock() {
        return BuiltInRegistries.BLOCK.getKey(machine().targetState().getBlock()).toString();
    }

    @Callback
    @SuppressWarnings("PMD.UseConcurrentHashMap") // Local snapshot, never shared.
    default Map<String, String> getTargetState() {
        final BlockState state = machine().targetState();
        final Map<String, String> values = new LinkedHashMap<>();
        state.getProperties().forEach(property -> values.put(property.getName(), state.getValue(property).toString()));
        return Map.copyOf(values);
    }

    @Callback
    default List<CreateControlSetting> getSettings() {
        return controls().getSettings();
    }

    @Callback
    default int setSetting(@Parameter("id") final int id, @Parameter("value") final int value) {
        return controls().setValue(id, value);
    }

    private CreateControlsDevice controls() {
        return CreateControlsDevice.find(machine().targetEntity())
                .orElseThrow(() -> new IllegalStateException("Target has no supported settings API"));
    }
}
