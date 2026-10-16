package li.cil.oc2.gametest;

import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.misc.flash.FlashMemoryFlasherBlockEntity;
import li.cil.oc2.common.firmware.FlashImageUpload;
import li.cil.oc2.common.firmware.FlashWriterService;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.message.firmware.FlashWriterActionMessage;
import li.cil.oc2.common.network.message.firmware.FlashWriterChunkMessage;
import li.cil.oc2.common.network.message.firmware.FlashWriterStatusMessage;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.item.ItemStackUtils;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FlashWriterTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 300)
    public static void imageUploadIsLeasedAuthorizedAndStoredInWorldBlob(final GameTestHelper helper) throws Exception {
        final BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, Blocks.FLASH_MEMORY_FLASHER.get());
        final FlashMemoryFlasherBlockEntity writer = TestSupport.blockEntity(helper, relative, FlashMemoryFlasherBlockEntity.class);
        final ServerPlayer player = mockPlayer(helper);
        final ServerPlayer intruder = mockPlayer(helper);
        final BlockPos pos = writer.getBlockPos();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        intruder.setPos(player.getX(), player.getY(), player.getZ());
        writer.insert(new ItemStack(Items.FLASH_MEMORY.get()), player);
        final ItemStack original = writer.getDiskItemStack();
        final int capacity = Items.FLASH_MEMORY.get().getCapacity(original);
        final UUID token = FlashWriterService.open(player, writer);
        TestSupport.assertNotNull(helper, token, "server-issued writer session");
        final Field channelField = Connection.class.getDeclaredField("channel");
        channelField.setAccessible(true);
        final EmbeddedChannel channel = (EmbeddedChannel) channelField.get(connection(player));
        final byte[] image = new byte[FlashImageUpload.CHUNK_SIZE + 31];
        for (int index = 0; index < image.length; index++) image[index] = (byte) (index * 7 + 19);
        final String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image));
        FlashWriterService.action(player, new FlashWriterActionMessage(pos, token, "begin", image.length, "test.bin", digest));
        TestSupport.assertTrue(helper, "upload must hold the writer lease", writer.isWriting());
        writer.eject(player);
        TestSupport.assertTrue(helper, "ejection must not remove a leased item", writer.getDiskItemStack() == original);
        TestSupport.assertTrue(helper, "a second writer cannot acquire the lease", !writer.beginWriting());
        final boolean[] complete = {false};
        helper.onEachTick(() -> {
            if (complete[0]) return;
            channel.runPendingTasks();
            channel.flush();
            Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                if (!(outbound instanceof ClientboundCustomPayloadPacket packet)
                        || !(packet.payload() instanceof FlashWriterStatusMessage status)
                        || !status.session().equals(token)) continue;
                if (status.state().equals("error")) throw TestSupport.failure(helper, "writer rejected transfer: " + status.detail());
                if (status.state().equals("ready")) {
                    // A different player and a guessed token cannot cancel or write this session.
                    FlashWriterService.action(intruder, new FlashWriterActionMessage(pos, token, "cancel", 0, "", ""));
                    FlashWriterService.action(player, new FlashWriterActionMessage(pos, UUID.randomUUID(), "cancel", 0, "", ""));
                    FlashWriterService.chunk(intruder, new FlashWriterChunkMessage(pos, token, 0, new byte[]{0}));
                    FlashWriterService.chunk(player, new FlashWriterChunkMessage(pos, token, 0,
                            Arrays.copyOf(image, FlashImageUpload.CHUNK_SIZE)));
                } else if (status.state().equals("chunk")) {
                    if (status.offset() < image.length) {
                        FlashWriterService.chunk(player, new FlashWriterChunkMessage(pos, token, status.offset(),
                                Arrays.copyOfRange(image, status.offset(), image.length)));
                    } else {
                        FlashWriterService.action(player, new FlashWriterActionMessage(pos, token, "finish", 0, "", ""));
                    }
                } else if (status.state().equals("done")) {
                    try {
                        TestSupport.assertTrue(helper, "completion releases the writer", !writer.isWriting());
                        final UUID blob = blobOf(writer.getDiskItemStack());
                        final var registries = helper.getLevel().registryAccess();
                        final CompoundTag saved = writer.getUpdateTag(registries);
                        final FlashMemoryFlasherBlockEntity restored = new FlashMemoryFlasherBlockEntity(pos, writer.getBlockState());
                        restored.loadAdditional(saved, registries);
                        restored.setLevel(helper.getLevel());
                        // A real restored server BE subsequently exports/syncs its slot again.
                        // Without the handler's onLoad import, this used to erase the UUID.
                        restored.getUpdateTag(registries);
                        TestSupport.assertTrue(helper, "blob handle survives block entity NBT round-trip",
                                blob.equals(blobOf(restored.getDiskItemStack())));
                        TestSupport.assertTrue(helper, "item stores only a small blob reference",
                                ItemStackUtils.getModDataTag(writer.getDiskItemStack()).toString().length() < 512);
                        BlobStorage.closeAsync(blob).join();
                        final FileChannel stored = BlobStorage.getOrOpenAsync(blob).join();
                        final ByteBuffer actual = ByteBuffer.allocate(image.length);
                        while (actual.hasRemaining()) {
                            if (stored.read(actual) < 0) throw new IllegalStateException("blob is truncated");
                        }
                        TestSupport.assertTrue(helper, "image survives closing and reopening its world blob", Arrays.equals(image, actual.array()));
                        TestSupport.assertTrue(helper, "storage is padded to capacity", stored.size() == capacity);
                        BlobStorage.closeAsync(blob).join();
                        complete[0] = true;
                        helper.succeed();
                    } catch (final Exception error) {
                        throw TestSupport.failure(helper, "cannot verify written blob: " + error);
                    }
                }
            }
        });
    }

    private static ServerPlayer mockPlayer(final GameTestHelper helper) throws Exception {
        @SuppressWarnings("removal") final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // NeoForge exposes this specifically for GameTests: negotiate every registered mod
        // channel on the real EmbeddedChannel rather than bypassing network sends.
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection(player));
        return player;
    }

    private static Connection connection(final ServerPlayer player) throws Exception {
        final Field field = net.minecraft.server.network.ServerCommonPacketListenerImpl.class
                .getDeclaredField("connection");
        field.setAccessible(true);
        return (Connection) field.get(player.connection);
    }

    private static UUID blobOf(final ItemStack stack) {
        //? if >=26.1 {
        /*return ItemStackUtils.getModDataTag(stack).getCompoundOrEmpty("data")
                .read("blob", net.minecraft.core.UUIDUtil.CODEC).orElseThrow();
        *///?} else {
        return ItemStackUtils.getModDataTag(stack).getCompound("data").getUUID("blob");
        //?}
    }

    private FlashWriterTests() {}
}
