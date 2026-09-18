package li.cil.oc2.client.hooks;

import li.cil.oc2.client.model.monitor.MonitorModelTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public final class MonitorModelHooks {
    private MonitorModelHooks() {}

    public static ModelData getModelData(final BlockState state) {
        return MonitorModelTypes.fromState(state);
    }
}