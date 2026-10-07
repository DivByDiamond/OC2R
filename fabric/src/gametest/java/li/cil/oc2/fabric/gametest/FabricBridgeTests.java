package li.cil.oc2.fabric.gametest;

import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.platform.BlockCapability;
import li.cil.oc2.platform.CapabilityInvalidationListener;
import li.cil.oc2.platform.EnergyStorage;
import li.cil.oc2.platform.FabricCapabilityRegistrar;
import li.cil.oc2.platform.Platform;
import li.cil.oc2.platform.event.CommonEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Exercises the Fabric implementations of the platform bridges on a real server. */
public final class FabricBridgeTests implements FabricGameTest {
    private static final BlockCapability<String> TEST_CAPABILITY =
            BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath("oc2r_gametest", "marker"), String.class);
    private static final BlockCapability<EnergyStorage> ENERGY = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath("neoforge", "energy"), EnergyStorage.class);

    private static final FabricCapabilityRegistrar REGISTRAR = new FabricCapabilityRegistrar();
    private static final TestEnergy STORAGE = new TestEnergy();

    static {
        REGISTRAR.registerBlock(TEST_CAPABILITY, (level, pos, state, entity, side) -> "marker", Blocks.GOLD_BLOCK);
        REGISTRAR.registerBlock(ENERGY, (level, pos, state, entity, side) -> STORAGE, Blocks.DIAMOND_BLOCK);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void platformEnvironment(final GameTestHelper helper) {
        if (Platform.environment().isClient()) {
            helper.fail("the game test server must not be the client");
        }
        if (!Platform.environment().isModLoaded("oc2r")) {
            helper.fail("oc2r is not reported as loaded");
        }
        if (!Platform.environment().configDir().toFile().isDirectory()) {
            helper.fail("config dir does not exist");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void commonEventsFire(final GameTestHelper helper) {
        final AtomicInteger serverTicks = new AtomicInteger();
        final AtomicInteger levelTicks = new AtomicInteger();
        CommonEvents.SERVER_TICK_START.register(server -> serverTicks.incrementAndGet());
        CommonEvents.LEVEL_TICK_START.register(level -> levelTicks.incrementAndGet());
        helper.succeedWhen(() -> {
            if (serverTicks.get() < 2 || levelTicks.get() < 2) {
                helper.fail("events did not fire: server=" + serverTicks + " level=" + levelTicks);
            }
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void capabilityLookup(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.GOLD_BLOCK);
        final String value = Platform.capabilities()
                .getBlockCapability(TEST_CAPABILITY, helper.getLevel(), helper.absolutePos(pos), null);
        if (!"marker".equals(value)) {
            helper.fail("provider not found, got " + value);
        }
        helper.setBlock(pos, Blocks.STONE);
        if (Platform.capabilities()
                        .getBlockCapability(TEST_CAPABILITY, helper.getLevel(), helper.absolutePos(pos), null)
                != null) {
            helper.fail("capability still found on a block without a provider");
        }
        helper.succeed();
    }

    /** Checks that LevelChunkMixin reports block changes to invalidation listeners. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void invalidationOnBlockChange(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 2, 1);
        final AtomicInteger invalidations = new AtomicInteger();
        final CapabilityInvalidationListener listener = new CapabilityInvalidationListener(() -> {
            invalidations.incrementAndGet();
            return true;
        });
        Platform.capabilities().registerBlockCapabilityListener(helper.getLevel(), helper.absolutePos(pos), listener);
        helper.setBlock(pos, Blocks.IRON_BLOCK);
        if (invalidations.get() == 0) {
            helper.fail("listener was not notified of the block change");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void energyInteropWithTeamReborn(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.DIAMOND_BLOCK);
        final BlockPos absolute = helper.absolutePos(pos);

        final team.reborn.energy.api.EnergyStorage exposed =
                team.reborn.energy.api.EnergyStorage.SIDED.find(helper.getLevel(), absolute, null);
        if (exposed == null) {
            helper.fail("our energy storage is not visible to Team Reborn Energy");
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            exposed.insert(40, transaction);
            transaction.commit();
        }
        if (STORAGE.stored != 40) {
            helper.fail("expected 40 stored after a committed insert, got " + STORAGE.stored);
        }
        try (Transaction transaction = Transaction.openOuter()) {
            exposed.insert(10, transaction);
            // not committed: must not change anything
        }
        if (STORAGE.stored != 40) {
            helper.fail("an aborted insert changed the storage to " + STORAGE.stored);
        }

        final EnergyStorage viaBridge = Platform.energy().getBlockEnergy(helper.getLevel(), absolute, null);
        if (viaBridge == null || viaBridge.getEnergyStored() != 40) {
            helper.fail("EnergyBridge did not return our storage");
        }
        helper.succeed();
    }

    /** Operations in one transaction must see each other's effect, then commit together. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void energyTransactionAccountsForPendingOperations(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.DIAMOND_BLOCK);
        final team.reborn.energy.api.EnergyStorage exposed =
                team.reborn.energy.api.EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), null);
        if (exposed == null) {
            helper.fail("storage not exposed");
            return;
        }
        STORAGE.stored = 950;
        long first;
        long second;
        try (Transaction transaction = Transaction.openOuter()) {
            first = exposed.insert(30, transaction);
            // Only 20 of the 50 free units are left after the first insert.
            second = exposed.insert(30, transaction);
            transaction.commit();
        }
        if (first != 30 || second != 20) {
            helper.fail("expected 30 then 20 accepted, got " + first + " and " + second);
        }
        if (STORAGE.stored != 1000) {
            helper.fail("expected a full storage after the commit, got " + STORAGE.stored);
        }
        try (Transaction transaction = Transaction.openOuter()) {
            final long removed = exposed.extract(600, transaction);
            final long more = exposed.extract(600, transaction);
            if (removed != 600 || more != 400) {
                helper.fail("expected 600 then 400 extracted, got " + removed + " and " + more);
            }
            // aborted: nothing may change
        }
        if (STORAGE.stored != 1000) {
            helper.fail("an aborted extraction changed the storage to " + STORAGE.stored);
        }
        STORAGE.stored = 0;
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void menuTypeCanBeCreated(final GameTestHelper helper) {
        if (Platform.menus().createMenuType((id, inventory, data) -> null) == null) {
            helper.fail("no menu type created");
        }
        helper.succeed();
    }

    private static final class TestEnergy implements EnergyStorage {
        int stored;

        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            final int accepted = Math.min(maxReceive, 1000 - stored);
            if (!simulate) {
                stored += accepted;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            final int removed = Math.min(maxExtract, stored);
            if (!simulate) {
                stored -= removed;
            }
            return removed;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return 1000;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
