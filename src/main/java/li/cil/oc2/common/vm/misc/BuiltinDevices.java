package li.cil.oc2.common.vm.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.Interrupt;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.rtc.SystemTimeRealTimeCounter;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;
import li.cil.sedna.riscv.R5Board;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BuiltinDevices {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int RTC_HOST_INTERRUPT = 0x1;
    public static final int RTC_MINECRAFT_INTERRUPT = 0x2;
    public static final int RPC_INTERRUPT = 0x3;
    private static final int UART_INTERRUPT = 0x4;
    private static final int VFS_INTERRUPT = 0x5;
    private static final int RFS_INTERRUPT = 0x7;
    // Free slot between the VFS and the RFS; claimed up front so dynamically allocated devices
    // never take it, and only for VMs that do not already have a device on it.
    private static final int SYSCON_INTERRUPT = 0x6;

    public final MinecraftRealTimeCounter rtcMinecraft = new MinecraftRealTimeCounter();

    @Serialized public VirtIOConsoleDevice rpcSerialDevice;
    @Serialized public UART16550A uart;
    @Serialized public VirtIOFileSystemDevice vfs;
    // Legacy bootfs device (sedna-buildroot < 0.2.x served bootfs.squashfs
    // next to rootfs.cramfs). The 0.2.x image line ships a single
    // rootfs.ext2; the field stays so pre-5.2 worlds still deserialize, but
    // new VMs never attach it.
    @Deprecated @Serialized public VirtIOBlockDevice bfs;
    @Serialized public VirtIOBlockDevice rfs;

    public BuiltinDevices(final GlobalVMContext context) {
        initialize(
                context,
                new GoldfishRTC(SystemTimeRealTimeCounter.get()),
                RTC_HOST_INTERRUPT,
                GoldfishRTC::getInterrupt);
        initialize(
                context,
                new GoldfishRTC(this.rtcMinecraft),
                RTC_MINECRAFT_INTERRUPT,
                GoldfishRTC::getInterrupt);
        rpcSerialDevice =
                initialize(
                        context,
                        new VirtIOConsoleDevice(context.getMemoryMap()),
                        RPC_INTERRUPT,
                        VirtIOConsoleDevice::getInterrupt);
        uart = initialize(context, new UART16550A(), UART_INTERRUPT, UART16550A::getInterrupt);
        vfs =
                initialize(
                        context,
                        new VirtIOFileSystemDevice(
                                context.getMemoryMap(),
                                "builtin",
                                FileSystems.getLayeredFileSystem()),
                        VFS_INTERRUPT,
                        VirtIOFileSystemDevice::getInterrupt);
        try (InputStream ris = Buildroot.getRootFilesystem()) {
            if (ris == null) {
                LOGGER.error(
                        "Buildroot root filesystem (generated/rootfs.ext2) is missing from"
                                + " sedna-buildroot.jar — VM will boot but have no rootfs.");
            }
            if (ris != null) {
                rfs =
                        initialize(
                                context,
                                new VirtIOBlockDevice(
                                        context.getMemoryMap(),
                                        ByteBufferBlockDevice.createFromStream(ris, true)),
                                RFS_INTERRUPT,
                                VirtIOBlockDevice::getInterrupt);
            }
        } catch (final IOException e) {
            // The original code did System.out.println here, which made this
            // failure mode invisible in production. Log it properly so users
            // can actually tell why their computer "appears on but does nothing".
            LOGGER.error(
                    "Failed to load the Buildroot rootfs block device into the VM", e);
        } catch (final Exception t) {
            LOGGER.error("Unexpected error while loading Buildroot block devices into the VM", t);
        }
    }

    public static void connectSystemController(final GlobalVMContext context, final R5Board board) {
        if (context.getInterruptAllocator().claimInterrupt(SYSCON_INTERRUPT)) {
            board.getSystemController()
                    .getInterrupt()
                    .set(SYSCON_INTERRUPT, context.getInterruptController());
        }
    }

    private static <T extends MemoryMappedDevice> T initialize(
            final GlobalVMContext context,
            final T device,
            final int interrupt,
            final Function<T, Interrupt> interruptSupplier) {
        if (!context.getInterruptAllocator().claimInterrupt(interrupt))
            throw new IllegalStateException();
        interruptSupplier.apply(device).set(interrupt, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(device);
        return device;
    }
}