package li.cil.oc2.common.blockentity.monitor;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.UUID;
import java.util.function.Supplier;
import li.cil.oc2.common.bus.device.DeviceGroup;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

public final class MonitorStateManager {
    public final DeviceGroup deviceGroup;
    final MonitorDevice monitorDevice;
    final KeyboardDevice<BlockEntity> keyboardDevice;
    public final FixedEnergyStorage energy;
    UUID deviceId = UUID.randomUUID();

    private final Object monitorCache;

    private static Supplier<Object> createMonitorSupplier() {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            return li.cil.oc2.client.hooks.MonitorRendererHooks::createMonitorGUIRenderer;
        }
        return () -> null;
    }

    boolean hasEnergy;
    boolean isMounted;
    boolean isPowered;
    boolean captureInputState;

    MonitorStateManager(final BlockEntity blockEntity, final BooleanConsumer onMountedChanged) {
        this.energy = new FixedEnergyStorage(Config.monitorEnergyStorage);
        this.monitorDevice = new MonitorDevice(blockEntity, onMountedChanged);
        this.keyboardDevice = new KeyboardDevice<>(blockEntity);
        this.deviceGroup = new DeviceGroup(blockEntity);
        // Created once here rather than lazily in getMonitor(): a lazy null-check-then-set
        // is not thread-safe (render thread vs. main thread could both pass the null check
        // and each construct their own renderer, leaking one). This fixes construction only;
        // thread-safety of using the renderer is up to the renderer itself.
        this.monitorCache = createMonitorSupplier().get();
        deviceGroup.addDevice(monitorDevice);
        deviceGroup.addDevice(keyboardDevice);
    }

    public Object getMonitor() {
        return monitorCache;
    }

    CompoundTag createUpdateTag(final CompoundTag tag) {
        tag.putBoolean("projecting", isMounted);
        tag.putBoolean("has_energy", hasEnergy);
        tag.putBoolean("state", isPowered);
        tag.putUUID("device_id", deviceId);
        return tag;
    }

    void readUpdateTag(final CompoundTag tag) {
        isMounted = tag.getBoolean("projecting");
        hasEnergy = tag.getBoolean("has_energy");
        isPowered = tag.getBoolean("state");
        if (tag.hasUUID("device_id")) {
            deviceId = tag.getUUID("device_id");
        }
    }

    void savePersistent(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put("energy", energy.serializeNBT(registries));
        tag.putBoolean("projecting", isPowered);
        tag.putBoolean("has_energy", hasEnergy);
        tag.putUUID("device_id", deviceId);
    }

    void loadPersistent(final CompoundTag tag, final HolderLookup.Provider registries) {
        energy.deserializeNBT(registries, tag.getCompound("energy"));
        hasEnergy = tag.getBoolean("has_energy");
        isPowered = tag.getBoolean("projecting");
        if (tag.hasUUID("device_id")) {
            deviceId = tag.getUUID("device_id");
        }
    }
}