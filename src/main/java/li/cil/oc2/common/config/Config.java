package li.cil.oc2.common.config;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.config.client.GUISpec;
//? if <26.1 {
import net.minecraft.world.item.Tiers;
//?}

/**
 * Runtime configuration of the mod.
 *
 * <p>All mutable fields are {@code volatile}: the config file is re-read at runtime (the
 * loader calls {@code loadValues()} from its config event), which writes these fields on the
 * config thread, while the server, client and VM threads read them. Without {@code volatile}
 * a {@code long} or {@code double} may be read torn (JLS 17.7), e.g. yielding a wrong memory
 * limit or a wrong energy cost.
 */
@SuppressWarnings("FieldMayBeFinal")
public final class Config {
    public static volatile long maxAllocatedMemory = 512 * Constants.MEGABYTE;
    public static volatile int diskSizeTier1 = 8 * Constants.MEGABYTE;
    public static volatile int diskSizeTier2 = 16 * Constants.MEGABYTE;
    public static volatile int diskSizeTier3 = 32 * Constants.MEGABYTE;
    public static volatile int diskSizeTier4 = 128 * Constants.MEGABYTE;
    public static volatile int flashMemorySizeTier1 = 4 * Constants.MEGABYTE;
    public static volatile int flashMemorySizeTier2 = 8 * Constants.MEGABYTE;
    public static volatile int flashMemorySizeTier3 = 16 * Constants.MEGABYTE;

    public static volatile int maxBusElements = 1024;
    public static volatile int networkConnectorPorts = 4;
    public static volatile double busCableEnergyPerTick = 0.1;
    public static volatile double busInterfaceEnergyPerTick = 0.5;
    public static volatile int cableEnergyCapacity = 3000;
    public static volatile int cableEnergyTransferPerTick = 1024;
    public static volatile int computerEnergyPerTick = 10;
    public static volatile int computerEnergyStorage = 8000;
    public static volatile int chargerEnergyPerTick = 2500;
    public static volatile int chargerEnergyStorage = 10000;
    public static volatile int projectorEnergyPerTick = 20;
    public static volatile int projectorEnergyStorage = 2000;
    public static volatile int monitorEnergyPerTick = 15;
    public static volatile int monitorEnergyStorage = 2000;
    public static volatile int monitorMaxWidth = 5;
    public static volatile int monitorFps = 20;
    public static volatile int monitorMaxHeight = 5;
    public static volatile int videoCodec = 0;
    public static volatile int cardCageEnergyPerTick = 20;
    public static volatile int cardCageEnergyStorage = 2000;
    public static volatile int gatewayEnergyPerPacket = 20;
    public static volatile int gatewayEnergyStorage = 2000;

    public static volatile int robotEnergyPerTick = 5;
    public static volatile int robotEnergyStorage = 750000;

    public static volatile double memoryEnergyPerMegabytePerTick = 0.5;
    public static volatile double hardDriveEnergyPerMegabytePerTick = 1;
    public static volatile double cpuEnergyPerMegahertzPerTick = 0.1;
    public static volatile int cpuFrequencyTier1 = 50_000_000;
    public static volatile int cpuFrequencyTier2 = 100_000_000;
    public static volatile int cpuFrequencyTier3 = 200_000_000;
    public static volatile int cpuFrequencyTier4 = 400_000_000;
    public static volatile int gpuEnergyPerTickTier1 = 2;
    public static volatile int gpuEnergyPerTickTier2 = 3;
    public static volatile int gpuEnergyPerTickTier3 = 5;
    public static volatile int gpuEnergyPerTickTier4 = 8;
    public static volatile int redstoneInterfaceCardEnergyPerTick = 1;
    public static volatile int networkInterfaceEnergyPerTick = 1;
    public static volatile int fileImportExportCardEnergyPerTick = 1;
    public static volatile int soundCardEnergyPerTick = 1;
    public static volatile int blockOperationsModuleEnergyPerTick = 2;
    public static volatile int inventoryOperationsModuleEnergyPerTick = 1;
    public static volatile int scannerModuleEnergyPerTick = 2;
    public static volatile int craftingModuleEnergyPerTick = 1;
    public static volatile int networkTunnelEnergyPerTick = 2;

    public static volatile String blockOperationsModuleToolTier = Tiers.DIAMOND.name();
    public static volatile long soundCardCoolDownSeconds = 2;
    public static volatile int soundCardPcmBytesPerSecond = 128 * 1024;
    public static volatile int vmTimeQuotaMs = 25;

    public static volatile UUID fakePlayerUUID = UUID.fromString("e39dd9a7-514f-4a2d-aa5e-b6030621416d");
    public static volatile int ethernetFrameTimeToLive = 12;
    public static volatile int hubEthernetFramesPerTick = 32;

    // Distinct defaults: bind all interfaces, remote endpoint loopback (see VXLANSpec).
    private static final String DEFAULT_VXLAN_BIND_HOST = "0.0.0.0";
    private static final String DEFAULT_VXLAN_REMOTE_HOST = "127.0.0.1";
    private static final String DEFAULT_NAME_SERVER = String.format("%d.%d.%d.%d", 1, 1, 1, 1);

    public static volatile boolean enable = false;
    public static volatile String remoteHost = DEFAULT_VXLAN_REMOTE_HOST;
    public static volatile int remotePort = 4789;
    public static volatile int vxlanPacketQueueCapacity = 32;
    public static volatile String bindHost = DEFAULT_VXLAN_BIND_HOST;
    public static volatile int bindPort = 4789;
    public static volatile boolean internetCardEnabled = false;
    public static volatile int defaultSessionLifetimeMs = 60 * 1000;
    public static volatile int defaultSessionsNumberPerCardLimit = 10;
    public static volatile int defaultSessionsNumberLimit = 100;
    public static volatile int defaultEchoRequestTimeoutMs = 1000;
    public static volatile List<String> deniedHosts =
            Arrays.asList(
                    "127.0.0.0/8",
                    "10.0.0.0/8",
                    "100.64.0.0/10",
                    "172.16.0.0/12",
                    "192.168.0.0/16",
                    "224.0.0.0/4");
    public static volatile List<String> allowedHosts = List.of();
    public static volatile String defaultNameServer = DEFAULT_NAME_SERVER;
    public static volatile boolean useSynchronisedNAT = false;
    public static volatile int streamBufferSize = 32 * 1024;
    public static volatile int tcpRetransmissionTimeoutMs = 2 * 1000;

    public static volatile GUISpec.CaptureInputMode captureInputMode = GUISpec.CaptureInputMode.PER_BLOCK;
    public static volatile boolean captureInputDefaultState = false;
    public static volatile float monitorBorder = 2f;

    public static boolean computersUseEnergy() {
        return computerEnergyPerTick > 0 && computerEnergyStorage > 0;
    }

    public static boolean projectorsUseEnergy() {
        return projectorEnergyStorage > 0 && projectorEnergyPerTick > 0;
    }

    public static boolean cardCagesUseEnergy() {
        return cardCageEnergyStorage > 0 && cardCageEnergyPerTick > 0;
    }

    public static boolean robotsUseEnergy() {
        return robotEnergyPerTick > 0 && robotEnergyStorage > 0;
    }

    public static boolean monitorsUseEnergy() {
        return computerEnergyPerTick > 0 && computerEnergyStorage > 0;
    }

    public static boolean gatewayUseEnergy() {
        return gatewayEnergyPerPacket > 0 && gatewayEnergyStorage > 0;
    }
}
