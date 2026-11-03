package li.cil.oc2.common.entity.robot.state;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.action.RobotActionResult;
import li.cil.oc2.common.entity.robot.movement.MovementDirection;
import li.cil.oc2.common.entity.robot.rotation.RotationDirection;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class RobotDevice {
    private final Robot robot;

    public RobotDevice(final Robot robot) {
        this.robot = robot;
    }

    @Callback
    public String getRadioEndpointId() {
        return robot.getRadioEndpoint().id().toString();
    }

    @Callback
    public void joinRadioNetwork(
            @Parameter("invite") String invite, @Parameter("secret") String secret) {
        robot.getRadioEndpoint().joinRadioNetwork(invite, secret);
        robot.getRadioEndpoint().register("robot");
    }

    @Callback
    public String getRadioAddress() {
        return robot.getRadioEndpoint().address();
    }

    @Callback
    public java.util.Map<String, Object> getRadioStatus() {
        return robot.getRadioEndpoint().status();
    }

    @Callback
    public void registerRadioService(@Parameter("service") String service) {
        robot.getRadioEndpoint().register(service);
    }

    @Callback
    public void sendRadioMessage(
            @Parameter("address") String address,
            @Parameter("service") String service,
            @Parameter("channel") String channel,
            @Parameter("requestId") String request,
            @Parameter("payload") String payload) {
        robot.getRadioEndpoint().send(address, service, channel, request, payload);
    }

    @Callback
    public java.util.List<li.cil.oc2.common.radio.service.RadioMessage> pollRadioMessages() {
        return robot.getRadioEndpoint().pollMessages();
    }

    @Callback
    public void publishRadioTelemetry(
            @Parameter("service") String service,
            @Parameter("channel") String channel,
            @Parameter("payload") String payload) {
        robot.getRadioEndpoint().publish(service, channel, payload);
    }

    @Callback
    public String subscribeRadioTelemetry(
            @Parameter("address") String address,
            @Parameter("service") String service,
            @Parameter("fields") String[] fields,
            @Parameter("staleTicks") int staleTicks) {
        return robot.getRadioEndpoint().subscribe(address, service, fields, staleTicks);
    }

    @Callback
    public void unsubscribeRadioTelemetry(@Parameter("subscription") String subscription) {
        robot.getRadioEndpoint().unsubscribe(subscription);
    }

    @Callback
    public String sendRadioCommand(
            @Parameter("address") String address,
            @Parameter("service") String service,
            @Parameter("channel") String channel,
            @Parameter("requestId") String request,
            @Parameter("payload") String payload,
            @Parameter("timeoutTicks") int timeout) {
        return robot.getRadioEndpoint()
                .command(address, service, channel, request, payload, timeout);
    }

    @Callback
    public void updateRadioCommand(
            @Parameter("requestId") String request,
            @Parameter("state") String state,
            @Parameter("result") String result) {
        robot.getRadioEndpoint().commandStatus(request, state, result);
    }

    @Callback
    public java.util.List<String> discoverRadioServices(@Parameter("address") String address) {
        return robot.getRadioEndpoint().discover(address);
    }

    @Callback
    public java.util.List<li.cil.oc2.common.radio.service.RadioServiceBroker.Peer> listRadioDevices() {
        return robot.getRadioEndpoint().peers();
    }

    @Callback
    public li.cil.oc2.common.radio.service.RadioServiceBroker.Estimate estimateRadioRoute(
            @Parameter("address") String address) {
        return robot.getRadioEndpoint().estimate(address);
    }

    @Callback
    public void pingRadio(
            @Parameter("address") String address, @Parameter("requestId") String request) {
        robot.getRadioEndpoint().ping(address, request);
    }

    @Callback
    public java.util.List<li.cil.oc2.common.radio.service.RadioServiceBroker.Event>
            pollRadioEvents() {
        return robot.getRadioEndpoint().poll();
    }

    @Callback(synchronize = false)
    public int getEnergyStored() {
        return robot.getEnergyStorage().getEnergyStored();
    }

    @Callback(synchronize = false)
    public int getEnergyCapacity() {
        return robot.getEnergyStorage().getMaxEnergyStored();
    }

    @Callback(synchronize = false)
    public int getSelectedSlot() {
        return robot.getSelectedSlot();
    }

    @Callback(synchronize = false)
    public void setSelectedSlot(@Parameter("slot") final int slot) {
        robot.setSelectedSlot(slot);
    }

    @Callback
    public ItemStack getStackInSlot(@Parameter("slot") final int slot) {
        return robot.getInventory().getStackInSlot(slot);
    }

    @Callback(synchronize = false)
    public boolean move(@Parameter("direction") @Nullable final MovementDirection direction) {
        if (direction == null) throw new IllegalArgumentException();
        return robot.getMovementController().move(direction);
    }

    @Callback(synchronize = false)
    public boolean turn(@Parameter("direction") @Nullable final RotationDirection direction) {
        if (direction == null) throw new IllegalArgumentException();
        return robot.getMovementController().rotate(direction);
    }

    @Callback(synchronize = false)
    public int getLastActionId() {
        return robot.getMovementController().getLastActionId();
    }

    @Callback(synchronize = false)
    public int getQueuedActionCount() {
        return robot.getMovementController().getQueuedActionCount();
    }

    @Nullable
    @Callback(synchronize = false)
    public RobotActionResult getActionResult(@Parameter("actionId") final int actionId) {
        return robot.getMovementController().findActionResult(actionId);
    }

    @Callback(synchronize = false)
    public String getFacing() {
        return robot.getFacing();
    }

    @Callback(synchronize = false)
    public Robot.Position getPosition() {
        return robot.getPosition();
    }

    @Callback(synchronize = false)
    public void calibratePosition() {
        robot.calibratePosition();
    }
}
