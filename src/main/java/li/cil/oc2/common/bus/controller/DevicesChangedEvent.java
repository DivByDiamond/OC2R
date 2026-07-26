package li.cil.oc2.common.bus.controller;

import li.cil.oc2.api.bus.device.Device;

import java.util.Collection;

public record DevicesChangedEvent(Collection<Device> devices) { }
