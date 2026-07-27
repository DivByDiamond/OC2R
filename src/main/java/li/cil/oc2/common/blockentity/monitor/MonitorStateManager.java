package li.cil.oc2.common.blockentity.monitor;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.bus.device.DeviceGroup;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

final class MonitorStateManager {
    final DeviceGroup deviceGroup;
    final MonitorDevice monitorDevice;
    final KeyboardDevice<BlockEntity> keyboardDevice;
    final MonitorGUIRenderer monitor = new MonitorGUIRenderer();
    final FixedEnergyStorage energy;
    UUID deviceId = UUID.randomUUID();

    boolean hasEnergy;
    boolean isMounted;
    boolean isPowered;
    boolean captureInputState;

    MonitorStateManager(final BlockEntity blockEntity, final BooleanConsumer onMountedChanged) {
        this.energy = new FixedEnergyStorage(Config.monitorEnergyStorage);
        this.monitorDevice = new MonitorDevice(blockEntity, onMountedChanged);
        this.keyboardDevice = new KeyboardDevice<>(blockEntity);
        this.deviceGroup = new DeviceGroup(blockEntity);
        deviceGroup.addDevice(monitorDevice);
        deviceGroup.addDevice(keyboardDevice);
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
