package li.cil.oc2.common.entity;

import java.util.*;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.container.robot.RobotInventoryContainer;
import li.cil.oc2.common.container.robot.RobotTerminalContainer;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.robot.AbstractRobotEntity;
import li.cil.oc2.common.entity.robot.RobotTickHandler;
import li.cil.oc2.common.entity.robot.RobotVirtualMachine;
import li.cil.oc2.common.entity.robot.misc.RobotBlockCollider;
import li.cil.oc2.common.entity.robot.misc.RobotEventHandler;
import li.cil.oc2.common.entity.robot.misc.RobotInteractionHandler;
import li.cil.oc2.common.entity.robot.misc.RobotInventory;
import li.cil.oc2.common.entity.robot.movement.RobotMovementController;
import li.cil.oc2.common.entity.robot.state.RobotAnimationState;
import li.cil.oc2.common.entity.robot.state.RobotSerializer;
import li.cil.oc2.common.vm.handler.VMItemStackHandlers;
import li.cil.oc2.common.vm.runner.AbstractVirtualMachine;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class Robot extends AbstractRobotEntity
        implements li.cil.oc2.api.capabilities.Robot,
                TerminalUserProvider {
    private RobotEventHandler eventHandler;
    private RobotBlockCollider blockCollider;

    private final RobotInventory robotInventory;
    private final RobotMovementController movementController;
    private final Terminal terminal = new Terminal();
    private final RobotVirtualMachine virtualMachine;
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.robotEnergyStorage);
    private final RobotAnimationState animationState;
    private final Set<Player> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());

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

    public RobotEventHandler getEventHandler() {
        return eventHandler;
    }

    public RobotBlockCollider getBlockCollider() {
        return blockCollider;
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
            getEventHandler().unregister();
            virtualMachine.stop();
            virtualMachine.dispose();
        }
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        RobotSerializer.save(this, tag);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        RobotSerializer.load(this, tag);
    }
}
