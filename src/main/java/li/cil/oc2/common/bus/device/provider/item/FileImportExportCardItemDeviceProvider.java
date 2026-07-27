package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.Items;

import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class FileImportExportCardItemDeviceProvider extends AbstractItemDeviceProvider {
    public FileImportExportCardItemDeviceProvider() {
        super(Items.FILE_IMPORT_EXPORT_CARD);
    }

    @Override
    protected boolean matches(final ItemDeviceQuery query) {
        return super.matches(query) && getTerminalUserProvider(query).isPresent();
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return getTerminalUserProvider(query)
                .map(
                        provider ->
                                new FileImportExportCardItemDevice(query.getItemStack(), provider));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Config.fileImportExportCardEnergyPerTick;
    }

    private Optional<TerminalUserProvider> getTerminalUserProvider(final ItemDeviceQuery query) {
        if (query.getContainerBlockEntity().isPresent()) {
            var be = query.getContainerBlockEntity().get();
            if (be instanceof TerminalUserProvider terminal) {
                return Optional.of(terminal);
            }
        }

        if (query.getContainerEntity().isPresent()) {
            final Entity entity = query.getContainerEntity().get();
            if (entity instanceof TerminalUserProvider terminal) {
                return Optional.of(terminal);
            }
        }

        return Optional.empty();
    }
}
