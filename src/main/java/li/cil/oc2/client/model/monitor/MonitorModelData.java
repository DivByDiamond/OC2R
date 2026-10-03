package li.cil.oc2.client.model.monitor;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** NeoForge {@link ModelData} carrying the multiblock position of a monitor block. */
public final class MonitorModelData {
    public static final ModelProperty<MonitorModelTypes.MonitorData> MONITOR_PROPERTY =
            new ModelProperty<>();

    private MonitorModelData() {}

    public static ModelData fromState(final BlockState state) {
        return ModelData.of(MONITOR_PROPERTY, MonitorModelTypes.dataFromState(state));
    }
}
