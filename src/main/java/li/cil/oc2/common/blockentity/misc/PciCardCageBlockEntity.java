package li.cil.oc2.common.blockentity.misc;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.misc.PciCardCageBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.misc.PciCardCageDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class PciCardCageBlockEntity extends ModBlockEntity implements TickableBlockEntity {

    private static final String ENERGY_TAG_NAME = "energy";
    private static final String HAS_ENERGY_TAG_NAME = "has_energy";

    private final PciCardCageDevice cardCageDevice =
            new PciCardCageDevice(this, this::handleMountedChanged);
    private boolean isMounted;
    private boolean energyPresent;
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.cardCageEnergyStorage);

    public PciCardCageBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.PCI_CARD_CAGE.get(), pos, state);
    }

    private void handleMountedChanged(final boolean value) {
        isMounted = value;
    }

    public boolean hasEnergy() {
        return energyPresent;
    }

    @Override
    public void serverTick() {
        if (!isMounted) {
            return;
        }

        if (Config.cardCagesUseEnergy()) {
            final boolean hasEnergy =
                    energy.extractEnergy(Config.cardCageEnergyPerTick, true)
                            >= Config.cardCageEnergyPerTick;
            if (hasEnergy) {
                energy.extractEnergy(Config.cardCageEnergyPerTick, false);
            }
            if (energyPresent != hasEnergy) {
                energyPresent = hasEnergy;
                setChanged();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);

        tag.putBoolean(HAS_ENERGY_TAG_NAME, energyPresent);

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);

        energyPresent = tag.getBoolean(HAS_ENERGY_TAG_NAME);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(ENERGY_TAG_NAME, energy.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG_NAME));
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (Config.cardCagesUseEnergy()) {
            event.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, be, side) -> {
                        if (be instanceof final PciCardCageBlockEntity self) {
                            return self.energy;
                        }
                        return null;
                    },
                    Blocks.PCI_CARD_CAGE.get());
        }

        event.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final PciCardCageBlockEntity self
                            && side
                                    == self.getBlockState()
                                            .getValue(PciCardCageBlock.FACING)
                                            .getOpposite()) {
                        return self.cardCageDevice;
                    }
                    return null;
                },
                Blocks.PCI_CARD_CAGE.get());
    }
}