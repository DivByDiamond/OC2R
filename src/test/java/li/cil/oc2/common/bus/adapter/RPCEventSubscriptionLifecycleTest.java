package li.cil.oc2.common.bus.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.IEventSink;
import li.cil.oc2.api.bus.device.rpc.RPCEventSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RPCEventSubscriptionLifecycleTest {
    @Test
    void unsubscribingAnObjectDeviceStopsItsEvents() {
        final EventDevice source = new EventDevice();
        final ObjectDevice device = new ObjectDevice(source);
        final DeviceBusController controller = controller(device);
        final DeviceRegistry registry = new DeviceRegistry();
        final IEventSink sink = mock(IEventSink.class);
        registry.rebuild(controller, sink);
        final UUID id = registry.devicesById.keySet().iterator().next();
        registry.subscribe(sink, id, Assertions::fail);
        registry.subscribe(sink, id, Assertions::fail);
        assertEquals(1, source.listeners.size(), "repeated subscribe has one sink");
        registry.unsubscribe(sink, id, Assertions::fail);
        source.emit();
        verifyNoInteractions(sink);
        assertTrue(source.listeners.isEmpty(), "unsubscribe unwraps the ObjectDevice event source");
    }

    @Test
    void disconnectingOneControllerPreservesTheOtherControllersSubscription() {
        final EventDevice source = new EventDevice();
        final ObjectDevice device = new ObjectDevice(source);
        final DeviceBusController firstController = controller(device);
        final DeviceBusController secondController = controller(device);
        final DeviceRegistry first = new DeviceRegistry();
        final DeviceRegistry second = new DeviceRegistry();
        final IEventSink firstSink = mock(IEventSink.class);
        final IEventSink secondSink = mock(IEventSink.class);
        first.rebuild(firstController, firstSink);
        second.rebuild(secondController, secondSink);
        first.mountDevices();
        second.mountDevices();
        first.subscribe(firstSink, first.devicesById.keySet().iterator().next(), Assertions::fail);
        second.subscribe(secondSink, second.devicesById.keySet().iterator().next(), Assertions::fail);
        assertEquals(2, source.listeners.size());
        first.clearSubscriptions(firstSink);
        first.unmountDevices();
        assertEquals(1, source.listeners.size(), "disconnect removes only its own sink");
        source.emit();
        verifyNoInteractions(firstSink);
        verify(secondSink).postEvent(any(), any());
        when(secondController.getDevices()).thenReturn(Set.of());
        second.rebuild(secondController, secondSink);
        assertTrue(source.listeners.isEmpty(), "removing the device clears its remaining subscription");
        reset(secondSink);
        source.emit();
        verifyNoInteractions(secondSink);
    }

    private static DeviceBusController controller(final ObjectDevice device) {
        final DeviceBusController controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(Set.of(device));
        when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(UUID.randomUUID()));
        return controller;
    }

    public static final class EventDevice implements RPCEventSource {
        final Map<IEventSink, UUID> listeners = new ConcurrentHashMap<>();

        @Callback
        public int read() {
            return 1;
        }

        @Override
        public void subscribe(final IEventSink sink, final UUID sourceId) {
            listeners.put(sink, sourceId);
        }

        @Override
        public void unsubscribe(final IEventSink sink) {
            listeners.remove(sink);
        }

        void emit() {
            final JsonObject event = new JsonObject();
            event.addProperty("event", "sample");
            listeners.forEach((sink, id) -> sink.postEvent(id, event));
        }
    }
}
