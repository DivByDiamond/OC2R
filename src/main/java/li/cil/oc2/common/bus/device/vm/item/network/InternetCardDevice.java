package li.cil.oc2.common.bus.device.vm.item.network;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.vm.item.AbstractNetworkInterfaceDevice;
import li.cil.oc2.common.inet.internet.InternetAdapter;
import li.cil.oc2.common.inet.internet.InternetConnection;
import li.cil.oc2.common.inet.internet.InternetManagerImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class InternetCardDevice extends AbstractNetworkInterfaceDevice {

    private static final Logger LOGGER = LogManager.getLogger();

    // Stable per-card identity: the MAC address is derived from it (see
    // DefaultLinkLocalLayer), so editing the card's NBT cannot pick an arbitrary
    // address and impersonate another card in the world's virtual LAN.
    public static final String DEVICE_ID_TAG_NAME = "DeviceId";

    private static final Random DEVICE_ID_RANDOM = new Random();

    /**
     * Connection leased from {@link InternetManagerImpl} while the card is mounted; released
     * (via {@code stop()}) on unmount so the manager can tear down the TCP/IP stack.
     */
    private InternetConnection internetConnection = null;
    @Nullable private UUID deviceId;

    public InternetCardDevice(final ItemStack identity) {
        super(identity);
    }

    /** Returns this card's stable identity, creating one on first use. */
    private UUID getOrCreateDeviceId() {
        if (deviceId == null) {
            deviceId = new UUID(DEVICE_ID_RANDOM.nextLong(), DEVICE_ID_RANDOM.nextLong());
        }
        return deviceId;
    }

    /**
     * Copies the device id into the adapter state so the link layer can derive its
     * hardware address from a trusted identity instead of the player-writable NBT.
     */
    private Tag withDeviceId(@Nullable final Tag adapterState) {
        final CompoundTag tag =
                adapterState instanceof final CompoundTag compound ? compound : new CompoundTag();
        tag.putUUID(DEVICE_ID_TAG_NAME, getOrCreateDeviceId());
        return tag;
    }

    private void openInternetAccess() {
        LOGGER.debug("Connect internet card");
        closeInternetAccess();
        final InternetAdapter internetAdapter = new InternetAdapterImpl(getNetworkInterface());
        internetAdapterState = withDeviceId(internetAdapterState);
        InternetManagerImpl.getInstance()
                .ifPresent(
                        internetManager ->
                                internetConnection =
                                        internetManager.connect(
                                                internetAdapter, internetAdapterState));
    }

    /** Stops and releases the connection; idempotent, safe to call before any connect. */
    private void closeInternetAccess() {
        if (internetConnection != null) {
            LOGGER.debug("Disconnect internet card");
            internetConnection.stop();
            internetConnection = null;
        }
    }

    /**
     * Adapter state captured at deserialization time and refreshed on each save, so that a
     * remount (which calls {@link #openInternetAccess()} again) restores the latest stack state
     * even if the device was never re-deserialized from NBT.
     */
    private Tag internetAdapterState = null;

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, final CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        internetAdapterState = tag.get(Constants.INTERNET_ADAPTER_TAG_NAME);
        if (internetAdapterState instanceof final CompoundTag compound
                && compound.hasUUID(DEVICE_ID_TAG_NAME)) {
            deviceId = compound.getUUID(DEVICE_ID_TAG_NAME);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        final CompoundTag tag = super.serializeNBT(provider);
        final InternetConnection internetConnection = this.internetConnection;
        if (internetConnection != null) {
            internetConnection
                    .saveAdapterState()
                    .ifPresent(
                            adapterState -> {
                                adapterState = withDeviceId(adapterState);
                                tag.put(Constants.INTERNET_ADAPTER_TAG_NAME, adapterState);
                                internetAdapterState = adapterState;
                            });
        }
        return tag;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        final VMDeviceLoadResult result = super.mount(context);
        openInternetAccess();
        return result;
    }

    @Override
    public void unmount() {
        super.unmount();
        closeInternetAccess();
    }

    private record InternetAdapterImpl(NetworkInterface networkInterface)
            implements InternetAdapter {

        @Nullable
        @Override
        public byte[] receiveEthernetFrame() {
            return networkInterface.readEthernetFrame();
        }

        @Override
        public void sendEthernetFrame(final byte[] frame) {
            networkInterface.writeEthernetFrame(networkInterface, frame, 64);
        }
    }
}