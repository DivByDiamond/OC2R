/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.bus.element.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.FixedSizeItemStackHandler;
import li.cil.oc2.common.container.RobotInventoryContainer;
import li.cil.oc2.common.container.RobotTerminalContainer;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.robot.*;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.*;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TerminalUtils;
import li.cil.oc2.common.vm.*;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.singleton;
import static li.cil.oc2.common.Constants.*;

@EventBusSubscriber(modid = API.MOD_ID)
public final class Robot extends Entity implements li.cil.oc2.api.capabilities.Robot, TerminalUserProvider, ICaptureInputStateStorage {
    public static final EntityDataAccessor<BlockPos> TARGET_POSITION = SynchedEntityData.defineId(Robot.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<Direction> TARGET_DIRECTION = SynchedEntityData.defineId(Robot.class, EntityDataSerializers.DIRECTION);
    public static final EntityDataAccessor<Byte> SELECTED_SLOT = SynchedEntityData.defineId(Robot.class, EntityDataSerializers.BYTE);

    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String BUS_ELEMENT_TAG_NAME = "bus_element";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String COMMAND_PROCESSOR_TAG_NAME = "commands";
    private static final String INVENTORY_TAG_NAME = "inventory";
    private static final String SELECTED_SLOT_TAG_NAME = "selected_slot";

    public static final int INVENTORY_SIZE = 12;

    ///////////////////////////////////////////////////////////////////

    private RobotEventHandler eventHandler;
    private RobotBlockCollider blockCollider;

    private final RobotInventory robotInventory;
    private final RobotMovementController movementController;
    private final Terminal terminal = new Terminal();
    private final RobotVirtualMachine virtualMachine;
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.robotEnergyStorage);
    private final RobotAnimationState animationState;
    private final Set<Player> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());
    private long lastPistonMovement;

    public boolean captureInputState;

    ///////////////////////////////////////////////////////////////////

    public Robot(final EntityType<?> type, final Level world) {
        super(type, world);
        this.blocksBuilding = true;
        setNoGravity(true);

        if (world.isClientSide()) {
            terminal.setDisplayOnly(true);
        }

        robotInventory = new RobotInventory(this);
        movementController = new RobotMovementController(this);
        final CommonDeviceBusController busController = new CommonDeviceBusController(robotInventory.getBusElement(), Config.robotEnergyPerTick);
        virtualMachine = new RobotVirtualMachine(this, busController, terminal, movementController);
        virtualMachine.state.builtinDevices.rtcMinecraft.setLevel(world);
        animationState = new RobotAnimationState(virtualMachine, () -> movementController.hasQueuedActions());
        eventHandler = new RobotEventHandler(this, virtualMachine);
        blockCollider = new RobotBlockCollider(this);
        robotInventory.setOnDeviceChanged(() -> virtualMachine.busController.scheduleBusScan());
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    public RobotAnimationState getAnimationState() {
        return animationState;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public FixedEnergyStorage getEnergyStorage() {
        return energy;
    }

    public RobotMovementController getMovementController() {
        return movementController;
    }

    public RobotInventory getRobotInventory() {
        return robotInventory;
    }

    public VMItemStackHandlers getItemStackHandlers() {
        return robotInventory.getItemStackHandlers();
    }

    @Override
    public ItemStackHandler getInventory() {
        return robotInventory.getInventory();
    }

    @Override
    public int getSelectedSlot() {
        return getEntityData().get(SELECTED_SLOT);
    }

    @Override
    public void setSelectedSlot(final int value) {
        getEntityData().set(SELECTED_SLOT, (byte) Mth.clamp(value, 0, INVENTORY_SIZE - 1));
    }

    @Override
    public boolean getCaptureInputState() {
        return captureInputState;
    }

    @Override
    public void setCaptureInputState(boolean value) {
        this.captureInputState = value;
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            Entities.ROBOT.get(),
            (robot, ctx) -> {
                return robot.robotInventory.getInventory();
            }
        );
        if (Config.robotsUseEnergy()) {
            event.registerEntity(
                Capabilities.EnergyStorage.ENTITY,
                Entities.ROBOT.get(),
                (robot, ctx) -> {
                    return robot.energy;
                }
            );
        }
        event.registerEntity(
            Capabilities.Robot.ENTITY,
            Entities.ROBOT.get(),
            (robot, ctx) -> {
                return robot;
            }
        );
    }

    public long getLastPistonMovement() {
        return lastPistonMovement;
    }

    public void start() {
        if (!level().isClientSide()) {
            virtualMachine.start();
        }
    }

    public void stop() {
        if (!level().isClientSide()) {
            virtualMachine.stop();
        }
    }

    public void openTerminalScreen(final ServerPlayer player) {
        RobotTerminalContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void openInventoryScreen(final ServerPlayer player) {
        RobotInventoryContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void addTerminalUser(final Player player) {
        terminalUsers.add(player);
    }

    public void removeTerminalUser(final Player player) {
        terminalUsers.remove(player);
    }

    @Override
    public Iterable<Player> getTerminalUsers() {
        return terminalUsers;
    }

    public void dropSelf() {
        if (!isAlive()) {
            return;
        }

        final ItemStack stack = new ItemStack(Items.ROBOT.get());
        exportToItemStack(stack);
        spawnAtLocation(stack);

        discard();
        LevelUtils.playSound(level(), blockPosition(), SoundType.METAL, SoundType::getBreakSound);
    }

    @Override
    public void tick() {
        final boolean isClient = level().isClientSide();

        if (firstTick) {
            if (isClient) {
                requestInitialState();
            } else {
                eventHandler.register();
                RobotActions.initializeData(this);
                if (movementController.getCurrentAction() != null) {
                    movementController.getCurrentAction().initialize(this);
                }
            }
        }

        super.tick();

        if (isClient) {
            terminal.clientTick();
        }

        if (!isClient) {
            virtualMachine.tick();
        }

        movementController.tick();

        if (!isClient) {
            blockCollider.collideWithWorld();
        }
    }

    @Override
    public boolean skipAttackInteraction(final Entity entity) {
        if (entity instanceof Player player && player.isCreative()) {
            dropSelf();
        }
        return true;
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level().isClientSide()) {
            if (Wrenches.isWrench(stack)) {
                if (player.isShiftKeyDown()) {
                    dropSelf();
                } else if (player instanceof final ServerPlayer serverPlayer) {
                    openInventoryScreen(serverPlayer);
                }
            } else {
                if (player.isShiftKeyDown()) {
                    start();
                } else if (player instanceof final ServerPlayer serverPlayer) {
                    openTerminalScreen(serverPlayer);
                }
            }
        }

        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if (!level().isClientSide() && reason.shouldDestroy()) {
            // Full unload to release out-of-nbt persisted runtime-only data such as ram.
            virtualMachine.stop();
            virtualMachine.dispose();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canCollideWith(final Entity entity) {
        return entity != this;
    }

    @Override
    public void push(final Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return false;
    }

    public void exportToItemStack(final ItemStack stack) {
        var container = new RestrictedContainer();
        robotInventory.saveItems(container);
        stack.set(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER, container);

        CustomData.update(DataComponents.CUSTOM_DATA, stack, (nbt) -> {
            var tag = NBTUtils.getOrCreateChildTag(nbt, MOD_TAG_NAME);
            tag.put(ENERGY_TAG_NAME, energy.serializeNBT(registryAccess()));
        });
    }

    public void importFromItemStack(final ItemStack stack) {
        final var provider = registryAccess();
        final var container = stack.get(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER);
        final CompoundTag itemsTag = NBTUtils.getChildTag(stack, MOD_TAG_NAME, ITEMS_TAG_NAME);

        if (container != null) {
            robotInventory.loadItems(provider, container);
        } else {
            robotInventory.loadItems(provider, itemsTag);
            robotInventory.getInventory().deserializeNBT(provider, itemsTag.getCompound(INVENTORY_TAG_NAME));
        }

        energy.deserializeNBT(provider, NBTUtils.getChildTag(stack, MOD_TAG_NAME, ENERGY_TAG_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_POSITION, BlockPos.ZERO);
        builder.define(TARGET_DIRECTION, Direction.NORTH);
        builder.define(SELECTED_SLOT, (byte) 0);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        final var provider = registryAccess();
        if (virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(STATE_TAG_NAME, virtualMachine.serialize());
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }

        tag.put(COMMAND_PROCESSOR_TAG_NAME, movementController.serialize());
        tag.put(BUS_ELEMENT_TAG_NAME, robotInventory.serializeBusElement(provider));
        robotInventory.saveItems(provider, NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME));
        tag.put(DEVICES_TAG_NAME, robotInventory.saveDevices(provider));
        tag.put(ENERGY_TAG_NAME, energy.serializeNBT(provider));
        tag.put(INVENTORY_TAG_NAME, robotInventory.getInventory().serializeNBT(provider));
        tag.putByte(SELECTED_SLOT_TAG_NAME, getEntityData().get(SELECTED_SLOT));
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        final var provider = registryAccess();
        virtualMachine.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        movementController.deserialize(tag.getCompound(COMMAND_PROCESSOR_TAG_NAME));
        robotInventory.deserializeBusElement(tag.getCompound(BUS_ELEMENT_TAG_NAME));
        robotInventory.loadItems(provider, tag.getCompound(ITEMS_TAG_NAME));
        robotInventory.loadDevices(provider, tag.getCompound(DEVICES_TAG_NAME));
        energy.deserializeNBT(provider, tag.getCompound(ENERGY_TAG_NAME));
        robotInventory.getInventory().deserializeNBT(provider, tag.getCompound(INVENTORY_TAG_NAME));
        setSelectedSlot(tag.getByte(SELECTED_SLOT_TAG_NAME));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void checkInsideBlocks() {
    }


    @Override
    protected Vec3 limitPistonMovement(final Vec3 pos) {
        lastPistonMovement = level().getGameTime();
        return super.limitPistonMovement(pos);
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    private void requestInitialState() {
        Network.sendToServer(new RobotInitializationRequestMessage(this));
    }



    ///////////////////////////////////////////////////////////////////









}
