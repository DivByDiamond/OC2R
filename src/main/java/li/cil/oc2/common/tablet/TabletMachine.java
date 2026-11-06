package li.cil.oc2.common.tablet;

import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.oc2.common.util.nbt.NBTUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** The server side of one tablet: its virtual machine, device slots and terminal. */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // Fields back their same-named accessors.
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "The machine hands out its live parts by design.")
public final class TabletMachine {
    private static final String STATE_TAG_NAME = "state";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String DEVICES_TAG_NAME = "devices";

    private final UUID id;
    private final ServerLevel level;
    private final Terminal terminal = new Terminal();
    private final TabletInventory inventory;
    private final TabletVirtualMachine virtualMachine;

    public TabletMachine(
            final UUID id,
            final ServerLevel level,
            final Supplier<Collection<ServerPlayer>> viewers) {
        this.id = id;
        this.level = level;
        inventory = new TabletInventory(level::registryAccess, false);
        final CommonDeviceBusController bus =
                new CommonDeviceBusController(inventory.busElement, 0, id::getMostSignificantBits);
        virtualMachine = new TabletVirtualMachine(id, level, bus, terminal, inventory, viewers);
        virtualMachine.state.builtinDevices.rtcMinecraft.setLevel(level);
        inventory.setOnDeviceChanged(bus::scheduleBusScan);
    }

    public UUID id() {
        return id;
    }

    public TabletInventory inventory() {
        return inventory;
    }

    public TabletVirtualMachine virtualMachine() {
        return virtualMachine;
    }

    public Terminal terminal() {
        return terminal;
    }

    /** Called once per server tick while the tablet is in hand or its screen is open. */
    public void tick() {
        virtualMachine.tick();
    }

    /** Guest keyboard bytes coming from the open screen. */
    public void putInput(final byte[] data) {
        terminal.io.putInput(ByteBuffer.wrap(data));
    }

    /** Prepares a tablet that has never been saved: basic memory and the OnyxOS flash. */
    public void initializeNew() {
        inventory.ensureFixedMemory();
        inventory.installDefaultFlash();
    }

    /** Writes memory, devices and terminal; the worker thread is joined first. */
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.put(STATE_TAG_NAME, virtualMachine.serialize());
        if (virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }
        inventory.saveItems(level.registryAccess(), NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME));
        tag.put(DEVICES_TAG_NAME, inventory.saveDevices(level.registryAccess()));
        return tag;
    }

    /** Restores what {@link #save()} wrote. */
    public void load(final CompoundTag tag) {
        virtualMachine.deserialize(NBTUtils.getChildTag(tag, STATE_TAG_NAME));
        NBTSerialization.deserialize(NBTUtils.getChildTag(tag, TERMINAL_TAG_NAME), terminal);
        inventory.resetDevices();
        inventory.loadItems(level.registryAccess(), NBTUtils.getChildTag(tag, ITEMS_TAG_NAME));
        inventory.loadDevices(level.registryAccess(), NBTUtils.getChildTag(tag, DEVICES_TAG_NAME));
        inventory.ensureFixedMemory();
    }

    /** Releases the machine after {@link #save()}; the state stays in the store. */
    public void suspend() {
        virtualMachine.suspend();
        virtualMachine.dispose();
    }
}
