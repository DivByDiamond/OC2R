package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
    private int attempts = 0;
    private int successes = 0;

    public ReportGameListener() {
    }

    @Override
    public void testStructureLoaded(GameTestInfo testInfo) {
        spawnBeacon(testInfo, Blocks.LIGHT_GRAY_STAINED_GLASS);
        this.attempts++;
    }

    private void handleRetry(GameTestInfo testInfo, GameTestRunner runner, boolean passed) {
        RetryOptions retryoptions = testInfo.retryOptions();
        String s = String.format("[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
        if (!retryoptions.unlimitedTries()) {
            s = s + String.format(", Left: %4d", retryoptions.numberOfTries() - this.attempts);
        }

        s = s + "]";
        String s1 = testInfo.getTestName() + " " + (passed ? "passed" : "failed") + "! " + testInfo.getRunTime() + "ms";
        String s2 = String.format("%-53s%s", s, s1);
        if (passed) {
            reportPassed(testInfo, s2);
        } else {
            say(testInfo.getLevel(), ChatFormatting.RED, s2);
        }

        if (retryoptions.hasTriesLeft(this.attempts, this.successes)) {
            runner.rerunTest(testInfo);
        }
    }

    @Override
    public void testPassed(GameTestInfo test, GameTestRunner runner) {
        this.successes++;
        if (test.retryOptions().hasRetries()) {
            this.handleRetry(test, runner, true);
        } else if (!test.isFlaky()) {
            reportPassed(test, test.getTestName() + " passed! (" + test.getRunTime() + "ms)");
        } else {
            if (this.successes >= test.requiredSuccesses()) {
                reportPassed(test, test + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(
                    test.getLevel(),
                    ChatFormatting.GREEN,
                    "Flaky test " + test + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
                );
                runner.rerunTest(test);
            }
        }
    }

    @Override
    public void testFailed(GameTestInfo test, GameTestRunner runner) {
        if (!test.isFlaky()) {
            reportFailure(test, test.getError());
            if (test.retryOptions().hasRetries()) {
                this.handleRetry(test, runner, false);
            }
        } else {
            TestFunction testfunction = test.getTestFunction();
            String s = "Flaky test " + test + " failed, attempt: " + this.attempts + "/" + testfunction.maxAttempts();
            if (testfunction.requiredSuccesses() > 1) {
                s = s + ", successes: " + this.successes + " (" + testfunction.requiredSuccesses() + " required)";
            }

            say(test.getLevel(), ChatFormatting.YELLOW, s);
            if (test.maxAttempts() - this.attempts + this.successes >= test.requiredSuccesses()) {
                runner.rerunTest(test);
            } else {
                reportFailure(test, new ExhaustedAttemptsException(this.attempts, this.successes, test));
            }
        }
    }

    @Override
    public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
        newTest.addListener(this);
    }

    public static void reportPassed(GameTestInfo testInfo, String message) {
        updateBeaconGlass(testInfo, Blocks.LIME_STAINED_GLASS);
        visualizePassedTest(testInfo, message);
    }

    private static void visualizePassedTest(GameTestInfo testInfo, String message) {
        say(testInfo.getLevel(), ChatFormatting.GREEN, message);
        GlobalTestReporter.onTestSuccess(testInfo);
    }

    protected static void reportFailure(GameTestInfo testInfo, Throwable error) {
        updateBeaconGlass(testInfo, testInfo.isRequired() ? Blocks.RED_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS);
        spawnLectern(testInfo, Util.describeError(error));
        visualizeFailedTest(testInfo, error);
    }

    protected static void visualizeFailedTest(GameTestInfo testInfo, Throwable error) {
        String s = error.getMessage() + (error.getCause() == null ? "" : " cause: " + Util.describeError(error.getCause()));
        String s1 = (testInfo.isRequired() ? "" : "(optional) ") + testInfo.getTestName() + " failed! " + s;
        say(testInfo.getLevel(), testInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, s1);
        Throwable throwable = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(error), error);
        if (throwable instanceof GameTestAssertPosException gametestassertposexception) {
            showRedBox(testInfo.getLevel(), gametestassertposexception.getAbsolutePos(), gametestassertposexception.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(testInfo);
    }

    protected static void spawnBeacon(GameTestInfo testInfo, Block block) {
        ServerLevel serverlevel = testInfo.getLevel();
        BlockPos blockpos = getBeaconPos(testInfo);
        serverlevel.setBlockAndUpdate(blockpos, Blocks.BEACON.defaultBlockState().rotate(testInfo.getRotation()));
        updateBeaconGlass(testInfo, block);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos blockpos1 = blockpos.offset(i, -1, j);
                serverlevel.setBlockAndUpdate(blockpos1, Blocks.IRON_BLOCK.defaultBlockState());
            }
        }
    }

    private static BlockPos getBeaconPos(GameTestInfo testInfo) {
        BlockPos blockpos = testInfo.getStructureBlockPos();
        BlockPos blockpos1 = new BlockPos(-1, -2, -1);
        return StructureTemplate.transform(blockpos.offset(blockpos1), Mirror.NONE, testInfo.getRotation(), blockpos);
    }

    private static void updateBeaconGlass(GameTestInfo testInfo, Block newBlock) {
        ServerLevel serverlevel = testInfo.getLevel();
        BlockPos blockpos = getBeaconPos(testInfo);
        if (serverlevel.getBlockState(blockpos).is(Blocks.BEACON)) {
            BlockPos blockpos1 = blockpos.offset(0, 1, 0);
            serverlevel.setBlockAndUpdate(blockpos1, newBlock.defaultBlockState());
        }
    }

    private static void spawnLectern(GameTestInfo testInfo, String message) {
        ServerLevel serverlevel = testInfo.getLevel();
        BlockPos blockpos = testInfo.getStructureBlockPos();
        BlockPos blockpos1 = new BlockPos(-1, 0, -1);
        BlockPos blockpos2 = StructureTemplate.transform(blockpos.offset(blockpos1), Mirror.NONE, testInfo.getRotation(), blockpos);
        serverlevel.setBlockAndUpdate(blockpos2, Blocks.LECTERN.defaultBlockState().rotate(testInfo.getRotation()));
        BlockState blockstate = serverlevel.getBlockState(blockpos2);
        ItemStack itemstack = createBook(testInfo.getTestName(), testInfo.isRequired(), message);
        LecternBlock.tryPlaceBook(null, serverlevel, blockpos2, blockstate, itemstack);
    }

    private static ItemStack createBook(String testName, boolean required, String message) {
        StringBuffer stringbuffer = new StringBuffer();
        Arrays.stream(testName.split("\\.")).forEach(p_177716_ -> stringbuffer.append(p_177716_).append('\n'));
        if (!required) {
            stringbuffer.append("(optional)\n");
        }

        stringbuffer.append("-------------------\n");
        ItemStack itemstack = new ItemStack(Items.WRITABLE_BOOK);
        itemstack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough(stringbuffer + message))));
        return itemstack;
    }

    protected static void say(ServerLevel serverLevel, ChatFormatting formatting, String message) {
        serverLevel.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(message).withStyle(formatting)));
    }

    private static void showRedBox(ServerLevel serverLevel, BlockPos pos, String displayMessage) {
        DebugPackets.sendGameTestAddMarker(serverLevel, pos, displayMessage, -2130771968, Integer.MAX_VALUE);
    }
}
