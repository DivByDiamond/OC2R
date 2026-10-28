package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.keyboard.link.KeyboardPairing;
import li.cil.oc2.common.blockentity.keyboard.link.KeyboardTargets;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KeyboardTests {
    private static final BlockPos KEYBOARD = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = new BlockPos(4, 2, 2);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void keyboardAssociationSurvivesDetachedLoadAndRejectsReplacement(final GameTestHelper helper) {
        helper.setBlock(KEYBOARD, Blocks.KEYBOARD.get().defaultBlockState());
        helper.setBlock(TARGET, Blocks.COMPUTER.get().defaultBlockState());
        final KeyboardBlockEntity keyboard = blockEntity(helper, KEYBOARD, KeyboardBlockEntity.class);
        final ComputerBlockEntity computer = blockEntity(helper, TARGET, ComputerBlockEntity.class);
        assertTrue(helper, "legacy computer must not acquire an unsolicited keyboard",
                !computer.busElement.getLocalDevices().contains(computer.keyboardDevice));
        final var player = fakePlayer(helper);
        player.setPos(Vec3.atCenterOf(keyboard.getBlockPos()));
        KeyboardPairing.select(player, keyboard);
        assertTrue(helper, "endpoint interaction must consume selection", KeyboardPairing.tryLink(player, computer.getBlockPos()));
        assertTrue(helper, "explicit link must mount native input",
                computer.busElement.getLocalDevices().contains(computer.keyboardDevice));
        final CompoundTag saved = keyboard.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var restored = (KeyboardBlockEntity) BlockEntity.loadStatic(keyboard.getBlockPos(),
                keyboard.getBlockState(), saved, helper.getLevel().registryAccess());
        assertTrue(helper, "identity and association must survive detached loading",
                restored != null && restored.getLevel() == null
                        && restored.getInputId().equals(keyboard.getInputId())
                        && restored.getTargetPos().equals(computer.getBlockPos()));
        helper.setBlock(TARGET, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(TARGET, Blocks.COMPUTER.get().defaultBlockState());
        assertTrue(helper, "replacement must never receive an old association",
                keyboard.inputTarget(null, computer.getBlockPos()) == null);
        keyboard.serverTick();
        assertTrue(helper, "broken endpoint must return keyboard to legacy mode",
                keyboard.getTargetPos().equals(keyboard.getBlockPos()));
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void projectorAssociationHasNativeInputAndPersistsEnablement(final GameTestHelper helper) {
        helper.setBlock(KEYBOARD, Blocks.KEYBOARD.get().defaultBlockState());
        helper.setBlock(TARGET, Blocks.PROJECTOR.get().defaultBlockState());
        final KeyboardBlockEntity keyboard = blockEntity(helper, KEYBOARD, KeyboardBlockEntity.class);
        final ProjectorBlockEntity projector = blockEntity(helper, TARGET, ProjectorBlockEntity.class);
        assertTrue(helper, "legacy projector must preserve framebuffer-only topology", !projector.isNativeInputEnabled());
        keyboard.link(projector);
        assertTrue(helper, "projector pairing must route native input",
                keyboard.inputTarget(null, projector.getBlockPos()) == projector.keyboardDevice
                        && projector.inputDevices.getDevices().contains(projector.keyboardDevice));
        final CompoundTag saved = projector.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var restored = (ProjectorBlockEntity) BlockEntity.loadStatic(projector.getBlockPos(),
                projector.getBlockState(), saved, helper.getLevel().registryAccess());
        assertTrue(helper, "projector input enablement must survive save/load", restored != null && restored.isNativeInputEnabled());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void monitorFragmentResolvesToSharedInputOrigin(final GameTestHelper helper) {
        final var originState = Blocks.MONITOR.get().defaultBlockState()
                .setValue(MonitorBlock.FACING, Direction.NORTH).setValue(MonitorBlock.WIDTH, 2);
        helper.setBlock(TARGET, originState);
        helper.setBlock(TARGET.east(), originState.setValue(MonitorBlock.ORIGIN_OFFSET_X, 1));
        final MonitorBlockEntity monitor = blockEntity(helper, TARGET, MonitorBlockEntity.class);
        final BlockEntity resolved = KeyboardTargets.resolve(helper.getLevel(), helper.absolutePos(TARGET.east()));
        assertTrue(helper, "fragment focus must resolve to origin", resolved == monitor);
        helper.setBlock(KEYBOARD, Blocks.KEYBOARD.get().defaultBlockState());
        final KeyboardBlockEntity keyboard = blockEntity(helper, KEYBOARD, KeyboardBlockEntity.class);
        keyboard.link(resolved);
        assertTrue(helper, "monitor origin must own the routed native input",
                keyboard.inputTarget(null, monitor.getBlockPos()) == monitor.getKeyboardDevice());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void keyReleaseSurvivesExhaustedBudgetAndUnloadEndsCapture(final GameTestHelper helper) {
        helper.setBlock(KEYBOARD, Blocks.KEYBOARD.get().defaultBlockState());
        final KeyboardBlockEntity keyboard = blockEntity(helper, KEYBOARD, KeyboardBlockEntity.class);
        final var player = serverPlayer(helper);
        player.setPos(Vec3.atCenterOf(keyboard.getBlockPos()));
        keyboard.capture.begin(player);
        keyboard.capture.focus(keyboard.inputTarget(player, keyboard.getBlockPos()));
        keyboard.capture.key(29, true);
        keyboard.capture.key(0x111, true);
        assertTrue(helper, "test must hold modifier and right button", keyboard.capture.heldInputCount() == 2);
        for (int i = 0; i < 300; i++) li.cil.oc2.common.network.util.PlayerRateLimits.allowEvents(player, 256);
        final li.cil.oc2.platform.MessageContext context = new li.cil.oc2.platform.MessageContext() {
            @Override public net.minecraft.world.entity.player.Player player() { return player; }
            @Override public net.minecraft.network.Connection connection() { return serverConnection(player); }
            @Override public void enqueueWork(final Runnable work) { work.run(); }
        };
        new li.cil.oc2.common.network.message.monitor.input.KeyboardInputMessage(keyboard, 29, false)
                .handleMessage(context);
        new li.cil.oc2.common.network.message.monitor.input.KeyboardInputMessage(keyboard, 0x111, false)
                .handleMessage(context);
        assertTrue(helper, "rate budget must never drop held-state releases", keyboard.capture.heldInputCount() == 0);
        keyboard.capture.key(42, true);
        keyboard.onChunkUnloaded();
        assertTrue(helper, "chunk unload must release held state and capture ownership",
                keyboard.capture.heldInputCount() == 0 && !keyboard.capture.owns(player));
        helper.succeed();
    }

}
