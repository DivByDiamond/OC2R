/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static li.cil.oc2.gametest.TestSupport.COMPUTER_POS;
import static li.cil.oc2.gametest.TestSupport.fakePlayer;

public final class ComputerFixture {
    private final GameTestHelper helper;
    private final BlockPos pos;

    // --------------------------------------------------------------------- //

    public static ComputerFixture place(final GameTestHelper helper) {
        return place(helper, fakePlayer(helper), COMPUTER_POS);
    }

    public static ComputerFixture place(final GameTestHelper helper, final Player player) {
        return place(helper, player, COMPUTER_POS);
    }

    public static ComputerFixture place(final GameTestHelper helper, final Player player, final BlockPos pos) {
        TestSupport.place(helper, player, new ItemStack(Items.COMPUTER.get()), pos);
        return new ComputerFixture(helper, pos);
    }

    public static ComputerFixture at(final GameTestHelper helper, final BlockPos pos) {
        return new ComputerFixture(helper, pos);
    }

    public static ComputerFixture at(final GameTestHelper helper) {
        return new ComputerFixture(helper, COMPUTER_POS);
    }

    // --------------------------------------------------------------------- //

    public BlockPos pos() {
        return pos;
    }

    public ComputerBlockEntity blockEntity() {
        return helper.getBlockEntity(pos);
    }

    public VirtualMachine virtualMachine() {
        return blockEntity().getVirtualMachine();
    }

    public VMRunState runState() {
        return virtualMachine().getRunState();
    }

    public void start() {
        virtualMachine().start();
    }

    public void stop() {
        virtualMachine().stop();
    }

    public void assertRunState(final VMRunState expected, final String what) {
        final VirtualMachine vm = virtualMachine();
        if (vm.getRunState() != expected) {
            throw new GameTestAssertException(what + ": computer is " + vm.getRunState()
                + ", expected " + expected + ", bootError=" + vm.getBootError());
        }
    }

    public void assertNoBootError() {
        if (virtualMachine().getBootError() != null) {
            throw new GameTestAssertException("computer reports boot error " + virtualMachine().getBootError());
        }
    }

    public void assertNoError() {
        if (virtualMachine().getError() != null) {
            throw new GameTestAssertException("VM reported an error: " + virtualMachine().getError());
        }
    }

    // --------------------------------------------------------------------- //

    public IItemHandler handler(final DeviceType type) {
        return blockEntity().getItemStackHandlers().getItemHandler(type)
            .orElseThrow(() -> new GameTestAssertException("no item handler for " + type));
    }

    public ItemStack slot(final DeviceType type) {
        return handler(type).getStackInSlot(0);
    }

    public ComputerFixture install(final DeviceType type, final ItemStack stack) {
        final IItemHandler handler = handler(type);
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack, false).isEmpty()) {
                return this;
            }
        }
        throw new GameTestAssertException("could not install " + stack + " as " + type
            + "; all " + handler.getSlots() + " slot(s) rejected it");
    }

    public ItemStack uninstall(final DeviceType type) {
        final ItemStack removed = handler(type).extractItem(0, 1, false);
        if (removed.isEmpty()) {
            throw new GameTestAssertException("nothing to remove from the " + type + " slot");
        }
        return removed;
    }

    public Set<Device> devices() {
        return blockEntity().virtualMachine.busController.getDevices();
    }

    public int deviceCount() {
        return devices().size();
    }

    @Nullable
    public Object networkInterface(final Direction side) {
        return helper.getLevel().getCapability(
            Capabilities.NetworkInterface.BLOCK, helper.absolutePos(pos), side);
    }

    public long energy() {
        final var storage = helper.getLevel().getCapability(
            Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        if (storage == null) {
            throw new GameTestAssertException("computer exposes no energy storage capability");
        }
        return storage.getEnergyStored();
    }

    // --------------------------------------------------------------------- //

    public CompoundTag save() {
        return blockEntity().saveWithFullMetadata(registries());
    }

    public CompoundTag updateTag() {
        return blockEntity().getUpdateTag(registries());
    }

    public void load(final CompoundTag tag) {
        blockEntity().loadWithComponents(tag, registries());
    }

    // --------------------------------------------------------------------- //

    public String screen() {
        final Terminal terminal = blockEntity().terminalManager.getTerminal();
        final int width = terminal.width;
        final int height = Terminal.HEIGHT;
        final StringBuilder text = new StringBuilder();
        synchronized (terminal) {
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    final int index = row * width + col;
                    final int value = index < terminal.buffer.length ? terminal.buffer[index] : 0;
                    text.append(value == 0 ? ' ' : (value < 0x20 || value > 0x7E) ? '?' : (char) value);
                }
                text.append('\n');
            }
        }
        return text.toString();
    }

    public void type(final String text) {
        final Terminal terminal = blockEntity().terminalManager.getTerminal();
        for (final byte value : text.getBytes(StandardCharsets.US_ASCII)) {
            terminal.io.putInput(value);
        }
    }

    public void assertScreenContains(final String expected, final String what) {
        final String text = screen();
        if (!text.contains(expected)) {
            throw new GameTestAssertException(what + ": screen does not hold [" + expected + "]; "
                + describe() + "\n" + text);
        }
    }

    public String describe() {
        final VirtualMachine vm = virtualMachine();
        return "runState=" + vm.getRunState()
            + ", bootError=" + vm.getBootError()
            + ", error=" + vm.getError()
            + ", devices=" + deviceCount()
            + ", energy=" + energy()
            + ", deviceList=" + devices().stream().map(d -> d.getClass().getSimpleName()).sorted().toList();
    }

    public void assertNoGuestPanic() {
        final String text = screen();
        for (final String marker : new String[]{"Kernel panic", "Oops", "BUG:", "Call Trace"}) {
            if (text.contains(marker)) {
                throw new GameTestAssertException("guest reported '" + marker + "':\n" + text);
            }
        }
    }

    // --------------------------------------------------------------------- //

    private RegistryAccess registries() {
        return helper.getLevel().registryAccess();
    }

    // --------------------------------------------------------------------- //

    private ComputerFixture(final GameTestHelper helper, final BlockPos pos) {
        this.helper = helper;
        this.pos = pos;
    }
}
