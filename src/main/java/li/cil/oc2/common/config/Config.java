package li.cil.oc2.common.config;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.config.client.GUISpec;
import net.minecraft.world.item.Tiers;

@SuppressWarnings("FieldMayBeFinal")
public final class Config {
    public static long maxAllocatedMemory = 512 * Constants.MEGABYTE;
    public static int diskSizeTier1 = 8 * Constants.MEGABYTE;
    public static int diskSizeTier2 = 16 * Constants.MEGABYTE;
    public static int diskSizeTier3 = 32 * Constants.MEGABYTE;
    public static int diskSizeTier4 = 128 * Constants.MEGABYTE;
    public static int flashMemorySizeTier1 = 4 * Constants.MEGABYTE;
    public static int flashMemorySizeTier2 = 8 * Constants.MEGABYTE;
    public static int flashMemorySizeTier3 = 16 * Constants.MEGABYTE;

    public static double busCableEnergyPerTick = 0.1;
    public static double busInterfaceEnergyPerTick = 0.5;
    public static int cableEnergyCapacity = 3000;
    public static int cableEnergyTransferPerTick = 1024;
    public static int computerEnergyPerTick = 10;
    public static int computerEnergyStorage = 8000;
    public static int chargerEnergyPerTick = 2500;
    public static int chargerEnergyStorage = 10000;
    public static int projectorEnergyPerTick = 20;
    public static int projectorEnergyStorage = 2000;
    public static int monitorEnergyPerTick = 15;
    public static int monitorEnergyStorage = 2000;
    public static int monitorMaxWidth = 5;
    public static int monitorFps = 20;
    public static int monitorMaxHeight = 5;
    public static int videoCodec = 0;
    public static int cardCageEnergyPerTick = 20;
    public static int cardCageEnergyStorage = 2000;
    public static int gatewayEnergyPerPacket = 20;
    public static int gatewayEnergyStorage = 2000;

    public static int robotEnergyPerTick = 5;
    public static int robotEnergyStorage = 750000;

    public static double memoryEnergyPerMegabytePerTick = 0.5;
    public static double hardDriveEnergyPerMegabytePerTick = 1;
    public static double cpuEnergyPerMegahertzPerTick = 0.1;
    public static int cpuFrequencyTier1 = 50_000_000;
    public static int cpuFrequencyTier2 = 100_000_000;
    public static int cpuFrequencyTier3 = 200_000_000;
    public static int cpuFrequencyTier4 = 400_000_000;
    public static int gpuEnergyPerTickTier1 = 2;
    public static int gpuEnergyPerTickTier2 = 3;
    public static int gpuEnergyPerTickTier3 = 5;
    public static int gpuEnergyPerTickTier4 = 8;
    public static int redstoneInterfaceCardEnergyPerTick = 1;
    public static int networkInterfaceEnergyPerTick = 1;
    public static int fileImportExportCardEnergyPerTick = 1;
    public static int soundCardEnergyPerTick = 1;
    public static int blockOperationsModuleEnergyPerTick = 2;
    public static int inventoryOperationsModuleEnergyPerTick = 1;
    public static int networkTunnelEnergyPerTick = 2;

    public static String blockOperationsModuleToolTier = Tiers.DIAMOND.name();
    public static long soundCardCoolDownSeconds = 2;
    public static int vmTimeQuotaMs = 25;

    public static UUID fakePlayerUUID = UUID.fromString("e39dd9a7-514f-4a2d-aa5e-b6030621416d");
    public static int ethernetFrameTimeToLive = 12;
    public static int hubEthernetFramesPerTick = 32;

    private static final String DEFAULT_VXLAN_HOST = String.format("::%d", 1);
    private static final String DEFAULT_NAME_SERVER = String.format("%d.%d.%d.%d", 1, 1, 1, 1);

    public static boolean enable = false;
    public static String remoteHost = DEFAULT_VXLAN_HOST;
    public static int remotePort = 4789;
    public static String bindHost = DEFAULT_VXLAN_HOST;
    public static int bindPort = 4789;
    public static boolean internetCardEnabled = false;
    public static int defaultSessionLifetimeMs = 60 * 1000;
    public static int defaultSessionsNumberPerCardLimit = 10;
    public static int defaultSessionsNumberLimit = 100;
    public static int defaultEchoRequestTimeoutMs = 1000;
    public static List<String> deniedHosts =
            Arrays.asList(
                    "127.0.0.0/8",
                    "10.0.0.0/8",
                    "100.64.0.0/10",
                    "172.16.0.0/12",
                    "192.168.0.0/16",
                    "224.0.0.0/4");
    public static List<String> allowedHosts = List.of();
    public static String defaultNameServer = DEFAULT_NAME_SERVER;
    public static boolean useSynchronisedNAT = false;
    public static int streamBufferSize = 2000;
    public static int tcpRetransmissionTimeoutMs = 2 * 1000;

    public static GUISpec.CaptureInputMode captureInputMode = GUISpec.CaptureInputMode.PER_BLOCK;
    public static boolean captureInputDefaultState = false;
    public static float monitorBorder = 2f;

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