package li.cil.oc2.common.bus.device.vm.item.network;

import javax.annotation.Nullable;
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

    /**
     * Connection leased from {@link InternetManagerImpl} while the card is mounted; released
     * (via {@code stop()}) on unmount so the manager can tear down the TCP/IP stack.
     */
    private InternetConnection internetConnection = null;

    public InternetCardDevice(final ItemStack identity) {
        super(identity);
    }

    private void openInternetAccess() {
        LOGGER.debug("Connect internet card");
        closeInternetAccess();
        final InternetAdapter internetAdapter = new InternetAdapterImpl(getNetworkInterface());
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