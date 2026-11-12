package li.cil.oc2.common.tablet;

import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.tablet.TabletFramebufferMessage;
import li.cil.oc2.common.radio.RadioClientLeases;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.oc2.common.tablet.display.TabletDisplayDevice;
import li.cil.oc2.common.tablet.radio.TabletBusElement;
import li.cil.oc2.common.tablet.radio.TabletHolder;
import li.cil.oc2.common.tablet.radio.TabletRadio;
import li.cil.oc2.common.util.nbt.NBTUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.video.DeltaFrameCodec;
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
    private static final String RADIO_SERVICES_TAG_NAME = "radio_services";

    private final UUID id;
    private final ServerLevel level;
    private final Terminal terminal = new Terminal();
    private final TabletInventory inventory;
    private final TabletVirtualMachine virtualMachine;
    private final TabletRadio radio;
    private final TabletDisplayDevice display = new TabletDisplayDevice();
    private final DeltaFrameCodec videoCodec = new DeltaFrameCodec();
    private final Supplier<Collection<ServerPlayer>> viewers;
    private final ByteBuffer videoFrame = ByteBuffer.allocate(TabletDisplayDevice.FRAME_BYTES);
    private boolean forceFrame = true;

    public TabletMachine(
            final UUID id,
            final ServerLevel level,
            final Supplier<Collection<ServerPlayer>> viewers) {
        this.id = id;
        this.level = level;
        this.viewers = viewers;
        inventory = new TabletInventory(level::registryAccess, false);
        radio = new TabletRadio(id, level);
        final CommonDeviceBusController bus =
                new CommonDeviceBusController(
                        new TabletBusElement(inventory.busElement, radio, id, display), 0, id::getMostSignificantBits);
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

    public TabletRadio radio() {
        return radio;
    }

    public TabletDisplayDevice display() { return display; }

    public void requestFrame() { forceFrame = true; }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Each bounded chunk needs its own immutable payload.
    private void sendFrame(final long now) {
        if (now % 4 != 0) return;
        final Collection<ServerPlayer> watching = viewers.get();
        if (watching.isEmpty() || !display.copyFrame(videoFrame, forceFrame)) return;
        if (forceFrame) videoCodec.requestKeyframe();
        forceFrame = false;
        final byte[] encoded = videoCodec.encode(
                videoFrame.array(), TabletDisplayDevice.WIDTH, TabletDisplayDevice.HEIGHT);
        final int size = TabletFramebufferMessage.MAX_CHUNK_SIZE;
        final int count = (encoded.length + size - 1) / size;
        for (int index = 0; index < count; index++) {
            final byte[] part = Arrays.copyOfRange(
                    encoded, index * size, Math.min(encoded.length, (index + 1) * size));
            final TabletFramebufferMessage message = new TabletFramebufferMessage(
                    id, encoded.length, index, count, part);
            for (final ServerPlayer player : watching) NetworkMessages.sendToClient(message, player);
        }
    }

    /** Called once per server tick while the tablet is in hand or its screen is open. */
    public void tick(@Nullable final TabletHolder holder, final long now) {
        radio.update(holder, now);
        if (holder == null) display.releaseInput();
        virtualMachine.tick();
        sendFrame(now);
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

    /** Writes memory, devices and terminal before the machine is released; held guest input is let go. */
    public CompoundTag save() {
        display.releaseInput();
        return checkpoint();
    }

    /**
     * Writes memory, devices and terminal of a machine that keeps running. The worker thread is joined
     * first and continues with the next tick; keys a player holds right now stay held.
     */
    public CompoundTag checkpoint() {
        final CompoundTag tag = new CompoundTag();
        tag.put(STATE_TAG_NAME, virtualMachine.serialize());
        if (virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }
        inventory.saveItems(level.registryAccess(), NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME));
        tag.put(DEVICES_TAG_NAME, inventory.saveDevices(level.registryAccess()));
        tag.put("display", display.serializeNBT(level.registryAccess()));
        tag.putString(RADIO_SERVICES_TAG_NAME, String.join("\n", radio.services()));
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
        display.deserializeNBT(level.registryAccess(), NBTUtils.getChildTag(tag, "display"));
        //? if >=26.1 {
        /*final String services = tag.getStringOr(RADIO_SERVICES_TAG_NAME, "");
        *///?} else {
        final String services = tag.getString(RADIO_SERVICES_TAG_NAME);
        //?}
        radio.restoreServices(services.isEmpty() ? java.util.List.of() : Arrays.asList(services.split("\n")));
    }

    /** Releases the machine after {@link #save()}; the state stays in the store. */
    public void suspend() {
        radio.release(RadioClientLeases.Reason.DROPPED);
        virtualMachine.suspend();
        virtualMachine.dispose();
    }
}
