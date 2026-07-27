package li.cil.oc2.common.bus.controller;

import java.util.Collection;
import li.cil.oc2.api.bus.device.Device;

public record DevicesChangedEvent(Collection<Device> devices) {}