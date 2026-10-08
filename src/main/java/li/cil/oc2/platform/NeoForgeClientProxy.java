package li.cil.oc2.platform;

import li.cil.oc2.client.hooks.BusCableModelHooks;
import li.cil.oc2.client.hooks.ClientProxyImpl;
import li.cil.oc2.client.hooks.MonitorModelHooks;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//? if >=26.1 {
/*import net.neoforged.neoforge.model.data.ModelData;
*///?} else {
import net.neoforged.neoforge.client.model.data.ModelData;
//?}

/** The NeoForge client proxy: {@link ClientProxyImpl} plus the NeoForge {@link ModelData} hooks. */
@OnlyIn(Dist.CLIENT)
public final class NeoForgeClientProxy extends ClientProxyImpl {
    @Override
    public Object emptyModelData() {
        return ModelData.EMPTY;
    }

    @Override
    public Object computeBusCableModelData(final BusCableBlockEntity owner, final Object current) {
        return BusCableModelHooks.computeModelData(owner, (ModelData) current);
    }

    @Override
    public Object computeMonitorModelData(final BlockState state) {
        return MonitorModelHooks.getModelData(state);
    }
}
