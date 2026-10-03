package li.cil.oc2.fabric.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.PlatformBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.config.common.CommonSpec;
import li.cil.oc2.platform.Platform;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Block entity load/unload hooks, update tag handling and config registration of the Fabric entrypoint.
 */
public final class FabricHookTests implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    /** A block entity that counts the platform hooks. */
    private static final class Probe extends PlatformBlockEntity {
        final AtomicInteger loads = new AtomicInteger();
        final AtomicInteger unloads = new AtomicInteger();

        Probe(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
            super(type, pos, state);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            loads.incrementAndGet();
        }

        @Override
        public void onChunkUnloaded() {
            super.onChunkUnloaded();
            unloads.incrementAndGet();
        }
    }

    // A new block entity type cannot be created after the registries froze; borrow the chest's.
    private static BlockEntityType<?> probeType() {
        return BlockEntityType.CHEST;
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void onLoadIsDeferredToTheNextTick(final GameTestHelper helper) {
        final BlockPos absolute = helper.absolutePos(POS);
        helper.setBlock(POS, net.minecraft.world.level.block.Blocks.CHEST);
        final BlockState state = helper.getBlockState(POS);
        final Probe probe = new Probe(probeType(), absolute, state);
        helper.getLevel().setBlockEntity(probe);

        // Same as NeoForge: not called while the block entity is being added ...
        if (probe.loads.get() != 0) {
            helper.fail("onLoad ran synchronously while adding the block entity");
        }
        // ... but at the start of the next level tick, exactly once.
        helper.runAfterDelay(3, () -> {
            if (probe.loads.get() != 1) {
                helper.fail("onLoad count after ticks: " + probe.loads.get());
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void onChunkUnloadedIsCalledWhenChunkClearsBlockEntities(final GameTestHelper helper) {
        // A private chunk, so no other test's block entities are affected.
        final LevelChunk chunk = new LevelChunk(
                helper.getLevel(), new net.minecraft.world.level.ChunkPos(900000, 900000));
        final BlockPos pos = chunk.getPos().getWorldPosition();
        chunk.setBlockState(pos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), false);
        final Probe probe = new Probe(probeType(), pos, chunk.getBlockState(pos));
        probe.setLevel(helper.getLevel());
        chunk.setBlockEntity(probe);

        if (probe.unloads.get() != 0) {
            helper.fail("unloaded too early");
        }
        chunk.clearAllBlockEntities();
        if (probe.unloads.get() != 1) {
            helper.fail("onChunkUnloaded count: " + probe.unloads.get());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void updateTagRoundTrip(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.BUS_CABLE.get().defaultBlockState());
        final BusCableBlockEntity source = (BusCableBlockEntity) helper.getBlockEntity(POS);
        source.setFacade(new ItemStack(net.minecraft.world.item.Items.STONE));

        final CompoundTag tag = source.getUpdateTag(helper.getLevel().registryAccess());
        if (tag.isEmpty()) {
            helper.fail("empty update tag");
        }

        // A second cable that has not seen the facade, as a freshly tracking client would have.
        final BlockPos otherPos = POS.east(2);
        helper.setBlock(otherPos, Blocks.BUS_CABLE.get().defaultBlockState());
        final BusCableBlockEntity target = (BusCableBlockEntity) helper.getBlockEntity(otherPos);
        if (!target.getFacade().isEmpty()) {
            helper.fail("target should start without facade");
        }
        target.handleUpdateTag(tag, helper.getLevel().registryAccess());
        if (!target.getFacade().is(net.minecraft.world.item.Items.STONE)) {
            helper.fail("facade was not restored from the update tag: " + target.getFacade());
        }
        helper.succeed();
    }

    /**
     * Vanilla only sends block entity data with a block update when the block entity supplies a packet;
     * without it a facade set on a loaded cable never reached the clients that were already tracking it.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void facadeIsSentWithTheBlockUpdate(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.BUS_CABLE.get().defaultBlockState());
        final BusCableBlockEntity source = (BusCableBlockEntity) helper.getBlockEntity(POS);
        source.setFacade(new ItemStack(net.minecraft.world.item.Items.STONE));

        final ClientboundBlockEntityDataPacket packet = source.getUpdatePacket();
        if (packet == null) {
            helper.fail("the cable supplies no block entity data packet");
            return;
        }

        final BlockPos otherPos = POS.east(2);
        helper.setBlock(otherPos, Blocks.BUS_CABLE.get().defaultBlockState());
        final BusCableBlockEntity target = (BusCableBlockEntity) helper.getBlockEntity(otherPos);
        target.handleUpdateTag(packet.getTag(), helper.getLevel().registryAccess());
        if (!target.getFacade().is(net.minecraft.world.item.Items.STONE)) {
            helper.fail("the packet did not carry the facade: " + target.getFacade());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void configRegistrationReachedTheSpecs(final GameTestHelper helper) throws IOException {
        if (!CommonSpec.CONFIG_SPEC.isLoaded()) {
            helper.fail("the common config was registered but never loaded");
        }
        final Path configDir = Platform.environment().configDir();
        try (Stream<Path> files = Files.list(configDir)) {
            if (files.noneMatch(path -> path.getFileName().toString().startsWith(API.MOD_ID)
                    && path.getFileName().toString().endsWith("-common.toml"))) {
                helper.fail("no " + API.MOD_ID + " common config file in " + configDir);
            }
        }
        helper.succeed();
    }
}
