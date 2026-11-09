package li.cil.oc2.common.tablet.radio;

import java.util.List;
import java.util.Map;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.radio.service.RadioMessage;
import li.cil.oc2.common.radio.service.RadioServiceBroker;

/**
 * The guest-facing radio of a tablet on {@code /dev/hvc0}, with the same calls as the robot device
 * so one messenger works on both. Every call is checked by the broker for the session of the tablet,
 * which exists only while the tablet is held and attached.
 */
public final class TabletDevice {
    private final TabletRadio radio;

    public TabletDevice(final TabletRadio radio) {
        this.radio = radio;
    }

    @Callback
    public String getRadioEndpointId() {
        return radio.endpoint().id().toString();
    }

    @Callback
    public void joinRadioNetwork(@Parameter("invite") final String invite, @Parameter("secret") final String secret) {
        radio.join(invite, secret);
    }

    @Callback
    public String getRadioAddress() {
        return radio.endpoint().address();
    }

    @Callback
    public Map<String, Object> getRadioStatus() {
        return radio.status();
    }

    @Callback
    public void registerRadioService(@Parameter("service") final String service) {
        radio.register(service);
    }

    @Callback
    public void sendRadioMessage(
            @Parameter("address") final String address,
            @Parameter("service") final String service,
            @Parameter("channel") final String channel,
            @Parameter("requestId") final String request,
            @Parameter("payload") final String payload) {
        radio.endpoint().send(address, service, channel, request, payload);
    }

    @Callback
    public List<RadioMessage> pollRadioMessages() {
        return radio.endpoint().pollMessages();
    }

    @Callback
    public void publishRadioTelemetry(
            @Parameter("service") final String service,
            @Parameter("channel") final String channel,
            @Parameter("payload") final String payload) {
        radio.endpoint().publish(service, channel, payload);
    }

    @Callback
    public String subscribeRadioTelemetry(
            @Parameter("address") final String address,
            @Parameter("service") final String service,
            @Parameter("fields") final String[] fields,
            @Parameter("staleTicks") final int staleTicks) {
        return radio.endpoint().subscribe(address, service, fields, staleTicks);
    }

    @Callback
    public void unsubscribeRadioTelemetry(@Parameter("subscription") final String subscription) {
        radio.endpoint().unsubscribe(subscription);
    }

    @Callback
    public String sendRadioCommand(
            @Parameter("address") final String address,
            @Parameter("service") final String service,
            @Parameter("channel") final String channel,
            @Parameter("requestId") final String request,
            @Parameter("payload") final String payload,
            @Parameter("timeoutTicks") final int timeout) {
        return radio.endpoint().command(address, service, channel, request, payload, timeout);
    }

    @Callback
    public void updateRadioCommand(
            @Parameter("requestId") final String request,
            @Parameter("state") final String state,
            @Parameter("result") final String result) {
        radio.endpoint().commandStatus(request, state, result);
    }

    @Callback
    public List<String> discoverRadioServices(@Parameter("address") final String address) {
        return radio.endpoint().discover(address);
    }

    @Callback
    public List<RadioServiceBroker.Peer> listRadioDevices() {
        return radio.endpoint().peers();
    }

    @Callback
    public RadioServiceBroker.Estimate estimateRadioRoute(@Parameter("address") final String address) {
        return radio.endpoint().estimate(address);
    }

    @Callback
    public void pingRadio(@Parameter("address") final String address, @Parameter("requestId") final String request) {
        radio.endpoint().ping(address, request);
    }

    @Callback
    public List<RadioServiceBroker.Event> pollRadioEvents() {
        return radio.endpoint().poll();
    }
}
