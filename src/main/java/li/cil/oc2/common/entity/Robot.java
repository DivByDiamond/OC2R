package li.cil.oc2.common.entity;

import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.container.RobotInventoryContainer;
import li.cil.oc2.common.container.RobotTerminalContainer;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.robot.*;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import li.cil.oc2.common.vm.*;
import li.cil.oc2.common.vm.terminal.Terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

public final class Robot extends Entity
        implements li.cil.oc2.api.capabilities.Robot,
                TerminalUserProvider,
                ICaptureInputStateStorage {
    public static final EntityDataAccessor<BlockPos> TARGET_POSITION =
            SynchedEntityData.defineId(Robot.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<Direction> TARGET_DIRECTION =
            SynchedEntityData.defineId(Robot.class, EntityDataSerializers.DIRECTION);
    public static final EntityDataAccessor<Byte> SELECTED_SLOT =
            SynchedEntityData.defineId(Robot.class, EntityDataSerializers.BYTE);

    public static final int INVENTORY_SIZE = 12;

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

    public Robot(final EntityType<?> type, final Level world) {
        super(type, world);
        this.blocksBuilding = true;
        setNoGravity(true);

        if (world.isClientSide()) {
            terminal.setDisplayOnly(true);
        }

        robotInventory = new RobotInventory(this);
        movementController = new RobotMovementController(this);
        final CommonDeviceBusController busController =
                new CommonDeviceBusController(
                        robotInventory.getBusElement(), Config.robotEnergyPerTick);
        virtualMachine = new RobotVirtualMachine(this, busController, terminal, movementController);
        virtualMachine.state.builtinDevices.rtcMinecraft.setLevel(world);
        animationState =
                new RobotAnimationState(
                        virtualMachine, () -> movementController.hasQueuedActions());
        eventHandler = new RobotEventHandler(this, virtualMachine);
        blockCollider = new RobotBlockCollider(this);
        robotInventory.setOnDeviceChanged(() -> virtualMachine.busController.scheduleBusScan());
    }

    @OnlyIn(Dist.CLIENT)
    public RobotAnimationState getAnimationState() {
        return animationState;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public AbstractVirtualMachine getVirtualMachine() {
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
    public void setCaptureInputState(final boolean value) {
        this.captureInputState = value;
    }

    public RobotEventHandler getEventHandler() {
        return eventHandler;
    }

    public RobotBlockCollider getBlockCollider() {
        return blockCollider;
    }

    public long getLastPistonMovement() {
        return lastPistonMovement;
    }

    public void start() {
        if (!level().isClientSide()) virtualMachine.start();
    }

    public void stop() {
        if (!level().isClientSide()) virtualMachine.stop();
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

    public void exportToItemStack(final ItemStack stack) {
        RobotSerializer.exportToItemStack(this, stack);
    }

    public void importFromItemStack(final ItemStack stack) {
        RobotSerializer.importFromItemStack(this, stack);
    }

    public void dropSelf() {
        RobotInteractionHandler.dropSelf(this);
    }

    @Override
    public void tick() {
        super.tick();
        RobotTickHandler.tick(this, firstTick);
    }

    @Override
    public boolean skipAttackInteraction(final Entity entity) {
        return RobotInteractionHandler.skipAttackInteraction(this, entity);
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        return RobotInteractionHandler.interact(this, player, hand);
    }

    @Override
    public void remove(final RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide() && reason.shouldDestroy()) {
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
    public void push(final Entity entity) {}

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return false;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(TARGET_POSITION, BlockPos.ZERO);
        builder.define(TARGET_DIRECTION, Direction.NORTH);
        builder.define(SELECTED_SLOT, (byte) 0);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        RobotSerializer.save(this, tag);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        RobotSerializer.load(this, tag);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void checkInsideBlocks() {}

    @Override
    protected Vec3 limitPistonMovement(final Vec3 pos) {
        lastPistonMovement = level().getGameTime();
        return super.limitPistonMovement(pos);
    }
}
