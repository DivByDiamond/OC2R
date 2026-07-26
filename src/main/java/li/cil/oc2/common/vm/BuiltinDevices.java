
package li.cil.oc2.common.vm;
import java.io.IOException;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.Interrupt;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.rtc.SystemTimeRealTimeCounter;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.device.virtio.VirtIOFileSystemDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.buildroot.Buildroot;
import java.io.InputStream;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.function.Function;

public final class BuiltinDevices {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int RTC_HOST_INTERRUPT = 0x1;
    public static final int RTC_MINECRAFT_INTERRUPT = 0x2;
    public static final int RPC_INTERRUPT = 0x3;
    private static final int UART_INTERRUPT = 0x4;
    private static final int VFS_INTERRUPT = 0x5;
    private static final int BFS_INTERRUPT = 0x6;
    private static final int RFS_INTERRUPT = 0x7;


    public final MinecraftRealTimeCounter rtcMinecraft = new MinecraftRealTimeCounter();


    @Serialized public final VirtIOConsoleDevice rpcSerialDevice;
    @Serialized public final UART16550A uart;
    @Serialized public final VirtIOFileSystemDevice vfs;
    @Serialized public VirtIOBlockDevice bfs;
    @Serialized public VirtIOBlockDevice rfs;


    public BuiltinDevices(final GlobalVMContext context) {
        initialize(context, new GoldfishRTC(SystemTimeRealTimeCounter.get()), RTC_HOST_INTERRUPT, GoldfishRTC::getInterrupt);
        initialize(context, new GoldfishRTC(this.rtcMinecraft), RTC_MINECRAFT_INTERRUPT, GoldfishRTC::getInterrupt);
        rpcSerialDevice = initialize(context, new VirtIOConsoleDevice(context.getMemoryMap()), RPC_INTERRUPT, VirtIOConsoleDevice::getInterrupt);
        uart = initialize(context, new UART16550A(), UART_INTERRUPT, UART16550A::getInterrupt);
        vfs = initialize(context, new VirtIOFileSystemDevice(context.getMemoryMap(), "builtin", FileSystems.getLayeredFileSystem()), VFS_INTERRUPT, VirtIOFileSystemDevice::getInterrupt);
        final InputStream ris = Buildroot.getRootFilesystem();
        final InputStream bis = Buildroot.getBootFilesystem();
        if (ris == null) {
            LOGGER.error("Buildroot root filesystem (generated/rootfs.cramfs) is missing from sedna-buildroot.jar — VM will boot but have no rootfs.");
        }
        if (bis == null) {
            LOGGER.error("Buildroot boot filesystem (generated/bootfs.squashfs) is missing from sedna-buildroot.jar — VM will boot but have no bootfs.");
        }
        try {
            if (bis != null) {
                bfs = initialize(context, new VirtIOBlockDevice(context.getMemoryMap(), ByteBufferBlockDevice.createFromStream(bis, true)), BFS_INTERRUPT, VirtIOBlockDevice::getInterrupt);
            }
            if (ris != null) {
                rfs = initialize(context, new VirtIOBlockDevice(context.getMemoryMap(), ByteBufferBlockDevice.createFromStream(ris, true)), RFS_INTERRUPT, VirtIOBlockDevice::getInterrupt);
            }
        } catch (final IOException e) {
            // The original code did System.out.println here, which made this
            // failure mode invisible in production. Log it properly so users
            // can actually tell why their computer "appears on but does nothing".
            LOGGER.error("Failed to load one of the Buildroot block devices (bootfs/rootfs) into the VM", e);
        } catch (final Throwable t) {
            // NPE from createFromStream(null) used to escape here — and since
            // this constructor runs during AbstractVirtualMachine setup (i.e.
            // inside the ComputerBlockEntity constructor), it would crash the
            // block entity creation entirely. Capture it instead.
            LOGGER.error("Unexpected error while loading Buildroot block devices into the VM", t);
        }
    }


    private static <T extends MemoryMappedDevice> T initialize(final GlobalVMContext context, final T device, final int interrupt, final Function<T, Interrupt> interruptSupplier) {
        if (!context.getInterruptAllocator().claimInterrupt(interrupt)) throw new IllegalStateException();
        interruptSupplier.apply(device).set(interrupt, context.getInterruptController());
        context.getMemoryRangeAllocator().claimMemoryRange(device);
        return device;
    }
}
