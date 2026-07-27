# The OpenComputers II (OC2R) API

Welcome to the API of OC2R, fellow developer! This document provides an overview of what integrations this API allows and how to best implement them. The primary purpose of the API is to let other mods implement their own devices for the computers in this mod.

> OC2R is a rewrite of the original OpenComputers 2 mod, updated to **NeoForge** and modern Minecraft versions.

## The `RPCDevice`

The core of the `RPCDevice` system is the [`RPCDevice`](bus/device/rpc/RPCDevice.java) interface itself. It defines a list of [`RPCMethods`](bus/device/rpc/RPCMethod.java), which represent the methods that can be called on the device. This is the suggested type of device to add, and allows easily exposing methods in your classes to virtual machines.

> If you have seen the APIs of OpenComputers or ComputerCraft before, this should feel fairly familiar.

### The `ObjectDevice`

It is perfectly fine to implement these interfaces manually. There is a more convenient way, however, when adding a device explicitly for this mod, in the form of the [`ObjectDevice`](bus/device/object/ObjectDevice.java). This class allows wrapping a Java object as an `RPCDevice`. Methods to be exposed in the device are defined by adding the [`Callback`](bus/device/object/Callback.java) annotation to methods of the object's class.

### Type Names

In addition to methods, `RPCDevices` provide a list of "type names". These names are meta-data that can be used by programs running in the virtual machines to identify devices. They should be clear and unique to avoid confusion between device types. For example, "machine" is probably too generic, whereas "redstone_furnace" is better. Note that for all `BlockEntities` providing devices, their registry name is automatically added to the list of type names. Equally, for all `Items` providing devices, their registry name is automatically added to the list of type names.

### Method Name Collisions

All `RPCDevices` found for a particular `BlockEntity` or `Item` are merged and presented as one singular `RPCDevice` to the virtual machine. This means that not only type names are merged, but `RPCMethodGroups` are merged into a single list as well. In most cases, it is fine to return each `RPCMethod` as its own `RPCMethodGroup`. For this reason, the `RPCMethod` interface extends the `RPCMethodGroup` interface.

The system supports method overloading to some degree. `RPCMethodGroups` with matching method name and parameter count are queried one by one, for a method matching a list of parameters. `RPCMethods` provide a default implementation for this, using the declared parameter types to determine if they match.

However, RPCs are passed from VM to Java as JSON messages, so some overloads that are clearly different on the Java side may lead to ambiguity. Specifically, in cases where one JSON serialization can be deserialized into different types. Most problematic in this area are `null` values, since they match any object type parameter.

> The system does a best-effort attempt: it will try deserializing parameters for ambiguous overloads one after the other, until deserialization for all parameter types succeeds.

To avoid ambiguity, it is recommended to pick clear and unique method names where reasonable. This is particularly true for generic `RPCDevices`, e.g. devices providing access to common capabilities, which may be provided by various `BlockEntities`. An example of this is the built-in devices for the `IEnergyStorage` capability.

For more control, `RPCMethodGroups` may implement custom override resolution via `findOverload(RPCInvocation)`.

### Device Lifecycle

Where needed, the optional interface methods `mount()`, `unmount()` and `suspend()` may be implemented to react to device lifecycle events. This can be useful when some state needs to be initialized or reset when the computer starts or stops, or the device is connected to or disconnected from a computer.

These methods are called in the following cases:

- `mount()` is called when a device is added to a running computer, or the computer it was added to starts running. It is also called when a computer resumes running after the chunk it sits in is loaded.
- `unmount()` is called when a device is removed from a running computer, or the computer it was added to stops running.
- `suspend()` is called when a device is connected to a running computer and the chunk the computer is in is unloaded. Either due to chunk unload or world unload.

This can be useful for various things. For example:

- Setting a flag in the block the device is associated with.
  - Set the flag in `mount()`.
  - Unset the flag in `unmount()`.
  - Ignore `suspend()`.
- Track out-of-minecraft resources, such as a file with extra data.
  - Create and open the file in `mount()`.
  - Close and delete the file in `unmount()`.
  - Close the file in `suspend()`.

### No Active Back-channel

Unlike some other computer mods (e.g. OpenComputers and ComputerCraft), there is no *active* back-channel in the `RPCDevice` API. In other words, it is not possible for `RPCDevices` to raise events in the virtual machines. The only way to provide data to the virtual machines is as values returned from exposed methods. Programs running in the virtual machines must always poll for changed data.

## The `BlockDeviceProvider` and `ItemDeviceProvider`

So you have an `RPCDevice` (or a `VMDevice`) and want the computer to use it. The core functionality that makes devices available to the mod are the [`BlockDeviceProvider`](bus/device/provider/BlockDeviceProvider.java) and [`ItemDeviceProvider`](bus/device/provider/ItemDeviceProvider.java) interfaces.

There is a registry for each, with which all block and item providers must be registered. These registries are queried to collect devices for a given block in the world, or an item in a machine inventory.

### Block Devices

Block devices are queried for all blocks adjacent to a `Bus Interface` that is connected to some computer via some `Bus Cable` and another `Bus Interface`. Connected `Bus Cables` with attached `Bus Interfaces` define a [`DeviceBus`](bus/DeviceBus.java). Computers collect all devices attached to the `DeviceBus` and make them available to the virtual machine they run. Each registered `BlockDeviceProvider` is queried for a block in question, and the found `RPCDevices` are aggregated into one `RPCDevice` proxy.

> `BusInterfaces` look for `Devices` using `BlockDeviceProviders`.

The mod comes with a set of convenience `BlockDeviceProviders`, which enable offering devices in various ways. This means you do not necessarily have to implement your own provider. The following built-in providers exist:

- `BlockEntities` are queried for the `Device` capability. If there is one, the returned device is used.
  - This allows optional support for this mod, based on whether it is present or not.
- `Blocks` and `BlockEntities` are scanned for `Callbacks`. If there are any, they are wrapped in an `ObjectDevice`.
  - This implies a hard dependency on this mod, due to the use of the `Callback` annotation in your `Block`/`BlockEntity` code.

### Item Devices

Item devices are queried for items inserted into computers and robots. For each `ItemStack` in a device slot, each `ItemDeviceProvider` is queried for the item in question, and the found `RPCDevices` are aggregated into one `RPCDevice` proxy.

> Note that such items must be tagged with the slot type they fit into, or they cannot be placed into computers and robots.

## The `VMDevice`

`VMDevices` are low-level, memory-mapped devices emulating "real" hardware, and thus requiring driver support by the operating system running in the virtual machines.

> `VMDevices` are very low-level, and most people can ignore them.

The core of the `VMDevice` system is the [`VMDevice`](bus/device/vm/VMDevice.java) interface itself. It defines a proxy used to load and unload actual emulated hardware. `VMDevices` use the [`VMContext`](bus/device/vm/context/VMContext.java) to properly bind hardware to the virtual machine upon initialization. This typically includes reserving an address block in memory, possibly hooking up interrupts and reserving host memory from the memory tracker. In most cases, `VMDevices` add a `MemoryMappedDevice` to the `MemoryMap` - an interface used by [Sedna], the VM implementation used to run the computers in this mod.

On the off chance you wish to add a `VMDevice`, and the existing devices do not suffice for reference, open a discussion on Github. I will skip more details here, since most people probably do not care, and it might scare people off instead...

## Examples

These examples are roughly sorted in order of likely usefulness. Most mods will want to maintain an optional integration with this mod instead of a hard dependency, so these examples are shown first.

### Block Device for own `BlockEntity`

In this example, a device is made available for a custom `BlockEntity`.

Using NeoForge capabilities:

```java
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

class ModBlockEntity extends BlockEntity {
    public int getMagicValue() {
        // ...
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> cap, @Nullable Direction side) {
        if (ModList.get().isLoaded("oc2r")) {
            T device = getDeviceCapability(cap);
            if (device != null) {
                return device;
            }
        }
        return super.getCapability(cap, side);
    }

    private <T> T getDeviceCapability(Capability<T> cap) {
        if (cap == Integration.DEVICE_CAPABILITY) {
            return cap.cast(Integration.createDevice(this));
        }
        return null;
    }
}

class Integration {
    public static final Capability<Device> DEVICE_CAPABILITY =
        Capabilities.get(/* register in your mod constructor via RegisterCapabilitiesEvent */);

    public static RPCDevice createDevice(ModBlockEntity blockEntity) {
        return new ObjectDevice(new ModBlockEntityDevice(blockEntity));
    }

    public record ModBlockEntityDevice(ModBlockEntity blockEntity) {
        @Callback
        public int getMagicValue() {
            return blockEntity.getMagicValue();
        }
    }
}
```

Using the `Callback` annotation in the `BlockEntity` (hard dependency):

```java
import li.cil.oc2.api.bus.device.object.Callback;
import net.minecraft.world.level.block.entity.BlockEntity;

class ModBlockEntity extends BlockEntity {
    @Callback
    public int getMagicValue() {
        // ...
    }
}
```

Using a custom `BlockDeviceProvider` is also possible - this is equivalent to the following example, on how to add devices to third-party `BlockEntities`.

### Block Device for a Third-Party `BlockEntity`

In this example, a simple device providing a single method, `squareRoot`, is made available for the `FurnaceBlockEntity`. As long as the registration of the `BlockDeviceProvider` is gated behind a check whether `oc2r` is present, this is a soft dependency.

Using `ObjectDevice`:

```java
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import net.minecraft.world.level.block.entity.BlockEntity;

class MyCalculatorDevice {
    @Callback(synchronize = false)
    public int squareRoot(int value) {
        if (value < 0) throw new IllegalArgumentException("Invalid input value!");
        return (int) Math.sqrt(value);
    }
}

class ModDeviceProvider implements BlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(BlockDeviceQuery query) {
        BlockEntity blockEntity = query.getLevel().getBlockEntity(query.getQueryPosition());
        if (blockEntity instanceof FurnaceBlockEntity) {
            return Invalidatable.of(new ObjectDevice(new MyCalculatorDevice(), "my_calculator_device"));
        } else {
            return Invalidatable.empty();
        }
    }
}
```

Using the `RPCDevice` and `RPCMethods` interfaces directly:

```java
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;
import li.cil.oc2.api.util.Invalidatable;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.util.Collections;
import java.util.List;

class ModDevice implements RPCDevice {
    @Override
    public List<String> getTypeNames() {
        return Collections.singletonList("my_calculator_device");
    }

    @Override
    public List<RPCMethodGroup> getMethodGroups() {
        return Collections.singletonList(new RPCMethod() {
            @Override
            public String getName() {
                return "squareRoot";
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }

            @Override
            public Class<?> getReturnType() {
                return int.class;
            }

            @Override
            public RPCParameter[] getParameters() {
                return new RPCParameter[]{() -> int.class};
            }

            @Override
            public Object invoke(RPCInvocation invocation) {
                int arg = invocation.getParameters().get(0).getAsInt();
                if (arg < 0) throw new IllegalArgumentException("Invalid input value!");
                return (int) Math.sqrt(arg);
            }
        });
    }
}

class ModDeviceProvider implements BlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(BlockDeviceQuery query) {
        BlockEntity blockEntity = query.getLevel().getBlockEntity(query.getQueryPosition());
        if (blockEntity instanceof FurnaceBlockEntity) {
            return Invalidatable.of(new ModDevice());
        } else {
            return Invalidatable.empty();
        }
    }
}
```

Shared device provider registration (NeoForge `DeferredRegister`):

```java
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import net.neoforged.neoforge.registries.DeferredRegister;

class Providers {
    static final DeferredRegister<BlockDeviceProvider> BLOCK_DEVICE_PROVIDERS =
            DeferredRegister.create(BlockDeviceProvider.class, "my_mod_id");

    static void initialize() {
        BLOCK_DEVICE_PROVIDERS.register("my_calculator_device", ModDeviceProvider::new);
    }
}
```

## Compatibility

OC2R is compatible with **Create Aeronautics**. The mod can be used alongside it without issues - computers, cables and devices interact normally with ships and moving structures.

[Sedna]: https://github.com/fnuecke/sedna
