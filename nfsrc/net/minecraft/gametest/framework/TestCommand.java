package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class TestCommand {
    public static final int STRUCTURE_BLOCK_NEARBY_SEARCH_RADIUS = 15;
    public static final int STRUCTURE_BLOCK_FULL_SEARCH_RADIUS = 200;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_CLEAR_RADIUS = 200;
    private static final int MAX_CLEAR_RADIUS = 1024;
    private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
    private static final int SHOW_POS_DURATION_MS = 10000;
    private static final int DEFAULT_X_SIZE = 5;
    private static final int DEFAULT_Y_SIZE = 5;
    private static final int DEFAULT_Z_SIZE = 5;
    private static final String STRUCTURE_BLOCK_ENTITY_COULD_NOT_BE_FOUND = "Structure block entity could not be found";
    private static final TestFinder.Builder<TestCommand.Runner> testFinder = new TestFinder.Builder<>(TestCommand.Runner::new);

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder,
        Function<CommandContext<CommandSourceStack>, TestCommand.Runner> runnerGetter,
        Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> modifier
    ) {
        return argumentBuilder.executes(p_319508_ -> runnerGetter.apply(p_319508_).run())
            .then(
                Commands.argument("numberOfTimes", IntegerArgumentType.integer(0))
                    .executes(p_319503_ -> runnerGetter.apply(p_319503_).run(new RetryOptions(IntegerArgumentType.getInteger(p_319503_, "numberOfTimes"), false)))
                    .then(
                        modifier.apply(
                            Commands.argument("untilFailed", BoolArgumentType.bool())
                                .executes(
                                    p_319489_ -> runnerGetter.apply(p_319489_)
                                            .run(
                                                new RetryOptions(
                                                    IntegerArgumentType.getInteger(p_319489_, "numberOfTimes"),
                                                    BoolArgumentType.getBool(p_319489_, "untilFailed")
                                                )
                                            )
                                )
                        )
                    )
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder, Function<CommandContext<CommandSourceStack>, TestCommand.Runner> runnerGetter
    ) {
        return runWithRetryOptions(argumentBuilder, runnerGetter, p_319485_ -> p_319485_);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptionsAndBuildInfo(
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder, Function<CommandContext<CommandSourceStack>, TestCommand.Runner> runnerGetter
    ) {
        return runWithRetryOptions(
            argumentBuilder,
            runnerGetter,
            p_319482_ -> p_319482_.then(
                    Commands.argument("rotationSteps", IntegerArgumentType.integer())
                        .executes(
                            p_319487_ -> runnerGetter.apply(p_319487_)
                                    .run(
                                        new RetryOptions(
                                            IntegerArgumentType.getInteger(p_319487_, "numberOfTimes"), BoolArgumentType.getBool(p_319487_, "untilFailed")
                                        ),
                                        IntegerArgumentType.getInteger(p_319487_, "rotationSteps")
                                    )
                        )
                        .then(
                            Commands.argument("testsPerRow", IntegerArgumentType.integer())
                                .executes(
                                    p_319484_ -> runnerGetter.apply(p_319484_)
                                            .run(
                                                new RetryOptions(
                                                    IntegerArgumentType.getInteger(p_319484_, "numberOfTimes"),
                                                    BoolArgumentType.getBool(p_319484_, "untilFailed")
                                                ),
                                                IntegerArgumentType.getInteger(p_319484_, "rotationSteps"),
                                                IntegerArgumentType.getInteger(p_319484_, "testsPerRow")
                                            )
                                )
                        )
                )
        );
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ArgumentBuilder<CommandSourceStack, ?> argumentbuilder = runWithRetryOptionsAndBuildInfo(
            Commands.argument("onlyRequiredTests", BoolArgumentType.bool()),
            p_319498_ -> testFinder.failedTests(p_319498_, BoolArgumentType.getBool(p_319498_, "onlyRequiredTests"))
        );
        ArgumentBuilder<CommandSourceStack, ?> argumentbuilder1 = runWithRetryOptionsAndBuildInfo(
            Commands.argument("testClassName", TestClassNameArgument.testClassName()),
            p_319490_ -> testFinder.allTestsInClass(p_319490_, TestClassNameArgument.getTestClassName(p_319490_, "testClassName"))
        );
        dispatcher.register(
            Commands.literal("test")
                .then(
                    Commands.literal("run")
                        .then(
                            runWithRetryOptionsAndBuildInfo(
                                Commands.argument("testName", TestFunctionArgument.testFunctionArgument()),
                                p_319494_ -> testFinder.byArgument(p_319494_, "testName")
                            )
                        )
                )
                .then(
                    Commands.literal("runmultiple")
                        .then(
                            Commands.argument("testName", TestFunctionArgument.testFunctionArgument())
                                .executes(p_329852_ -> testFinder.byArgument(p_329852_, "testName").run())
                                .then(
                                    Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(
                                            p_329853_ -> testFinder.createMultipleCopies(IntegerArgumentType.getInteger(p_329853_, "amount"))
                                                    .byArgument(p_329853_, "testName")
                                                    .run()
                                        )
                                )
                        )
                )
                .then(runWithRetryOptionsAndBuildInfo(Commands.literal("runall").then(argumentbuilder1), testFinder::allTests))
                .then(runWithRetryOptions(Commands.literal("runthese"), testFinder::allNearby))
                .then(runWithRetryOptions(Commands.literal("runclosest"), testFinder::nearest))
                .then(runWithRetryOptions(Commands.literal("runthat"), testFinder::lookedAt))
                .then(runWithRetryOptionsAndBuildInfo(Commands.literal("runfailed").then(argumentbuilder), testFinder::failedTests))
                .then(
                    Commands.literal("verify")
                        .then(
                            Commands.argument("testName", TestFunctionArgument.testFunctionArgument())
                                .executes(p_351716_ -> testFinder.byArgument(p_351716_, "testName").verify())
                        )
                )
                .then(
                    Commands.literal("verifyclass")
                        .then(
                            Commands.argument("testClassName", TestClassNameArgument.testClassName())
                                .executes(
                                    p_351715_ -> testFinder.allTestsInClass(p_351715_, TestClassNameArgument.getTestClassName(p_351715_, "testClassName"))
                                            .verify()
                                )
                        )
                )
                .then(
                    Commands.literal("locate")
                        .then(
                            Commands.argument("testName", TestFunctionArgument.testFunctionArgument())
                                .executes(
                                    p_340631_ -> testFinder.locateByName(
                                                p_340631_, "minecraft:" + TestFunctionArgument.getTestFunction(p_340631_, "testName").structureName()
                                            )
                                            .locate()
                                )
                        )
                )
                .then(Commands.literal("resetclosest").executes(p_319479_ -> testFinder.nearest(p_319479_).reset()))
                .then(Commands.literal("resetthese").executes(p_319492_ -> testFinder.allNearby(p_319492_).reset()))
                .then(Commands.literal("resetthat").executes(p_319478_ -> testFinder.lookedAt(p_319478_).reset()))
                .then(
                    Commands.literal("export")
                        .then(
                            Commands.argument("testName", StringArgumentType.word())
                                .executes(
                                    p_319491_ -> exportTestStructure(p_319491_.getSource(), "minecraft:" + StringArgumentType.getString(p_319491_, "testName"))
                                )
                        )
                )
                .then(Commands.literal("exportclosest").executes(p_319480_ -> testFinder.nearest(p_319480_).export()))
                .then(Commands.literal("exportthese").executes(p_319505_ -> testFinder.allNearby(p_319505_).export()))
                .then(Commands.literal("exportthat").executes(p_319514_ -> testFinder.lookedAt(p_319514_).export()))
                .then(Commands.literal("clearthat").executes(p_319506_ -> testFinder.lookedAt(p_319506_).clear()))
                .then(Commands.literal("clearthese").executes(p_319504_ -> testFinder.allNearby(p_319504_).clear()))
                .then(
                    Commands.literal("clearall")
                        .executes(p_319509_ -> testFinder.radius(p_319509_, 200).clear())
                        .then(
                            Commands.argument("radius", IntegerArgumentType.integer())
                                .executes(
                                    p_319493_ -> testFinder.radius(p_319493_, Mth.clamp(IntegerArgumentType.getInteger(p_319493_, "radius"), 0, 1024)).clear()
                                )
                        )
                )
                .then(
                    Commands.literal("import")
                        .then(
                            Commands.argument("testName", StringArgumentType.word())
                                .executes(p_128025_ -> importTestStructure(p_128025_.getSource(), StringArgumentType.getString(p_128025_, "testName")))
                        )
                )
                .then(Commands.literal("stop").executes(p_319497_ -> stopTests()))
                .then(
                    Commands.literal("pos")
                        .executes(p_128023_ -> showPos(p_128023_.getSource(), "pos"))
                        .then(
                            Commands.argument("var", StringArgumentType.word())
                                .executes(p_128021_ -> showPos(p_128021_.getSource(), StringArgumentType.getString(p_128021_, "var")))
                        )
                )
                .then(
                    Commands.literal("create")
                        .then(
                            Commands.argument("testName", StringArgumentType.word())
                                .suggests(TestFunctionArgument::suggestTestFunction)
                                .executes(p_128019_ -> createNewStructure(p_128019_.getSource(), StringArgumentType.getString(p_128019_, "testName"), 5, 5, 5))
                                .then(
                                    Commands.argument("width", IntegerArgumentType.integer())
                                        .executes(
                                            p_128014_ -> createNewStructure(
                                                    p_128014_.getSource(),
                                                    StringArgumentType.getString(p_128014_, "testName"),
                                                    IntegerArgumentType.getInteger(p_128014_, "width"),
                                                    IntegerArgumentType.getInteger(p_128014_, "width"),
                                                    IntegerArgumentType.getInteger(p_128014_, "width")
                                                )
                                        )
                                        .then(
                                            Commands.argument("height", IntegerArgumentType.integer())
                                                .then(
                                                    Commands.argument("depth", IntegerArgumentType.integer())
                                                        .executes(
                                                            p_128007_ -> createNewStructure(
                                                                    p_128007_.getSource(),
                                                                    StringArgumentType.getString(p_128007_, "testName"),
                                                                    IntegerArgumentType.getInteger(p_128007_, "width"),
                                                                    IntegerArgumentType.getInteger(p_128007_, "height"),
                                                                    IntegerArgumentType.getInteger(p_128007_, "depth")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int resetGameTestInfo(GameTestInfo gameTestInfo) {
        gameTestInfo.getLevel().getEntities(null, gameTestInfo.getStructureBounds()).stream().forEach(p_326748_ -> p_326748_.remove(Entity.RemovalReason.DISCARDED));
        gameTestInfo.getStructureBlockEntity().placeStructure(gameTestInfo.getLevel());
        StructureUtils.removeBarriers(gameTestInfo.getStructureBounds(), gameTestInfo.getLevel());
        say(gameTestInfo.getLevel(), "Reset succeded for: " + gameTestInfo.getTestName(), ChatFormatting.GREEN);
        return 1;
    }

    static Stream<GameTestInfo> toGameTestInfos(CommandSourceStack source, RetryOptions retryOptions, StructureBlockPosFinder structureBlockPosFinder) {
        return structureBlockPosFinder.findStructureBlockPos().map(p_319501_ -> createGameTestInfo(p_319501_, source.getLevel(), retryOptions)).flatMap(Optional::stream);
    }

    static Stream<GameTestInfo> toGameTestInfo(CommandSourceStack source, RetryOptions retryOptions, TestFunctionFinder testFunctionFinder, int rotationSteps) {
        return testFunctionFinder.findTestFunctions()
            .filter(p_319496_ -> verifyStructureExists(source.getLevel(), p_319496_.structureName()))
            .map(p_319513_ -> new GameTestInfo(p_319513_, StructureUtils.getRotationForRotationSteps(rotationSteps), source.getLevel(), retryOptions));
    }

    private static Optional<GameTestInfo> createGameTestInfo(BlockPos pos, ServerLevel level, RetryOptions retryOptions) {
        StructureBlockEntity structureblockentity = (StructureBlockEntity)level.getBlockEntity(pos);
        if (structureblockentity == null) {
            say(level, "Structure block entity could not be found", ChatFormatting.RED);
            return Optional.empty();
        } else {
            String s = structureblockentity.getMetaData();
            Optional<TestFunction> optional = GameTestRegistry.findTestFunction(s);
            if (optional.isEmpty()) {
                say(level, "Test function for test " + s + " could not be found", ChatFormatting.RED);
                return Optional.empty();
            } else {
                TestFunction testfunction = optional.get();
                GameTestInfo gametestinfo = new GameTestInfo(testfunction, structureblockentity.getRotation(), level, retryOptions);
                gametestinfo.setStructureBlockPos(pos);
                return !verifyStructureExists(level, gametestinfo.getStructureName()) ? Optional.empty() : Optional.of(gametestinfo);
            }
        }
    }

    private static int createNewStructure(CommandSourceStack source, String structureName, int x, int y, int z) {
        if (x <= 48 && y <= 48 && z <= 48) {
            ServerLevel serverlevel = source.getLevel();
            BlockPos blockpos = createTestPositionAround(source).below();
            StructureUtils.createNewEmptyStructureBlock(
                structureName.toLowerCase(), blockpos, new Vec3i(x, y, z), Rotation.NONE, serverlevel
            );
            BlockPos blockpos1 = blockpos.above();
            BlockPos blockpos2 = blockpos1.offset(x - 1, 0, z - 1);
            BlockPos.betweenClosedStream(blockpos1, blockpos2)
                .forEach(p_326747_ -> serverlevel.setBlockAndUpdate(p_326747_, Blocks.BEDROCK.defaultBlockState()));
            StructureUtils.addCommandBlockAndButtonToStartTest(blockpos, new BlockPos(1, 0, -1), Rotation.NONE, serverlevel);
            return 0;
        } else {
            throw new IllegalArgumentException("The structure must be less than 48 blocks big in each axis");
        }
    }

    private static int showPos(CommandSourceStack source, String variableName) throws CommandSyntaxException {
        BlockHitResult blockhitresult = (BlockHitResult)source.getPlayerOrException().pick(10.0, 1.0F, false);
        BlockPos blockpos = blockhitresult.getBlockPos();
        ServerLevel serverlevel = source.getLevel();
        Optional<BlockPos> optional = StructureUtils.findStructureBlockContainingPos(blockpos, 15, serverlevel);
        if (optional.isEmpty()) {
            optional = StructureUtils.findStructureBlockContainingPos(blockpos, 200, serverlevel);
        }

        if (optional.isEmpty()) {
            source.sendFailure(Component.literal("Can't find a structure block that contains the targeted pos " + blockpos));
            return 0;
        } else {
            StructureBlockEntity structureblockentity = (StructureBlockEntity)serverlevel.getBlockEntity(optional.get());
            if (structureblockentity == null) {
                say(serverlevel, "Structure block entity could not be found", ChatFormatting.RED);
                return 0;
            } else {
                BlockPos blockpos1 = blockpos.subtract(optional.get());
                String s = blockpos1.getX() + ", " + blockpos1.getY() + ", " + blockpos1.getZ();
                String s1 = structureblockentity.getMetaData().isBlank() ? structureblockentity.getStructureName() : structureblockentity.getMetaData(); // Neo: use the metadata for the structure name
                Component component = Component.literal(s)
                    .setStyle(
                        Style.EMPTY
                            .withBold(true)
                            .withColor(ChatFormatting.GREEN)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy to clipboard")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "final BlockPos " + variableName + " = new BlockPos(" + s + ");"))
                    );
                source.sendSuccess(() -> Component.literal("Position relative to " + s1 + ": ").append(component), false);
                DebugPackets.sendGameTestAddMarker(serverlevel, new BlockPos(blockpos), s, -2147418368, 10000);
                return 1;
            }
        }
    }

    static int stopTests() {
        GameTestTicker.SINGLETON.clear();
        return 1;
    }

    static int trackAndStartRunner(CommandSourceStack source, ServerLevel level, GameTestRunner runner) {
        runner.addListener(new TestCommand.TestBatchSummaryDisplayer(source));
        MultipleTestTracker multipletesttracker = new MultipleTestTracker(runner.getTestInfos());
        multipletesttracker.addListener(new TestCommand.TestSummaryDisplayer(level, multipletesttracker));
        multipletesttracker.addFailureListener(p_127992_ -> GameTestRegistry.rememberFailedTest(p_127992_.getTestFunction()));
        runner.start();
        return 1;
    }

    static int saveAndExportTestStructure(CommandSourceStack source, StructureBlockEntity structureBlockEntity) {
        String s = structureBlockEntity.getStructureName();
        if (!structureBlockEntity.saveStructure(true)) {
            say(source, "Failed to save structure " + s);
        }

        return exportTestStructure(source, s);
    }

    private static int exportTestStructure(CommandSourceStack source, String structurePath) {
        Path path = Paths.get(StructureUtils.testStructuresDir);
        ResourceLocation resourcelocation = ResourceLocation.parse(structurePath);
        Path path1 = source.getLevel().getStructureManager().createAndValidatePathToGeneratedStructure(resourcelocation, ".nbt");
        Path path2 = NbtToSnbt.convertStructure(CachedOutput.NO_CACHE, path1, resourcelocation.getPath(), path);
        if (path2 == null) {
            say(source, "Failed to export " + path1);
            return 1;
        } else {
            try {
                FileUtil.createDirectoriesSafe(path2.getParent());
            } catch (IOException ioexception) {
                say(source, "Could not create folder " + path2.getParent());
                LOGGER.error("Could not create export folder", (Throwable)ioexception);
                return 1;
            }

            say(source, "Exported " + structurePath + " to " + path2.toAbsolutePath());
            return 0;
        }
    }

    private static boolean verifyStructureExists(ServerLevel level, String structure) {
        if (level.getStructureManager().get(ResourceLocation.parse(structure)).isEmpty()) {
            say(level, "Test structure " + structure + " could not be found", ChatFormatting.RED);
            return false;
        } else {
            return true;
        }
    }

    static BlockPos createTestPositionAround(CommandSourceStack source) {
        BlockPos blockpos = BlockPos.containing(source.getPosition());
        int i = source.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockpos).getY();
        return new BlockPos(blockpos.getX(), i + 1, blockpos.getZ() + 3);
    }

    static void say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static int importTestStructure(CommandSourceStack source, String structurePath) {
        Path path = Paths.get(StructureUtils.testStructuresDir, structurePath + ".snbt");
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace(structurePath);
        Path path1 = source.getLevel().getStructureManager().createAndValidatePathToGeneratedStructure(resourcelocation, ".nbt");

        try {
            BufferedReader bufferedreader = Files.newBufferedReader(path);
            String s = IOUtils.toString(bufferedreader);
            Files.createDirectories(path1.getParent());

            try (OutputStream outputstream = Files.newOutputStream(path1)) {
                NbtIo.writeCompressed(NbtUtils.snbtToStructure(s), outputstream);
            }

            source.getLevel().getStructureManager().remove(resourcelocation);
            say(source, "Imported to " + path1.toAbsolutePath());
            return 0;
        } catch (CommandSyntaxException | IOException ioexception) {
            LOGGER.error("Failed to load structure {}", structurePath, ioexception);
            return 1;
        }
    }

    static void say(ServerLevel serverLevel, String message, ChatFormatting formatting) {
        serverLevel.getPlayers(p_127945_ -> true).forEach(p_313469_ -> p_313469_.sendSystemMessage(Component.literal(message).withStyle(formatting)));
    }

    public static class Runner {
        private final TestFinder<TestCommand.Runner> finder;

        public Runner(TestFinder<TestCommand.Runner> finder) {
            this.finder = finder;
        }

        public int reset() {
            TestCommand.stopTests();
            return TestCommand.toGameTestInfos(this.finder.source(), RetryOptions.noRetries(), this.finder)
                    .map(TestCommand::resetGameTestInfo)
                    .toList()
                    .isEmpty()
                ? 0
                : 1;
        }

        private <T> void logAndRun(Stream<T> structureBlockPos, ToIntFunction<T> testCounter, Runnable onFail, Consumer<Integer> onSuccess) {
            int i = structureBlockPos.mapToInt(testCounter).sum();
            if (i == 0) {
                onFail.run();
            } else {
                onSuccess.accept(i);
            }
        }

        public int clear() {
            TestCommand.stopTests();
            CommandSourceStack commandsourcestack = this.finder.source();
            ServerLevel serverlevel = commandsourcestack.getLevel();
            GameTestRunner.clearMarkers(serverlevel);
            this.logAndRun(
                this.finder.findStructureBlockPos(),
                p_320518_ -> {
                    StructureBlockEntity structureblockentity = (StructureBlockEntity)serverlevel.getBlockEntity(p_320518_);
                    if (structureblockentity == null) {
                        return 0;
                    } else {
                        BoundingBox boundingbox = StructureUtils.getStructureBoundingBox(structureblockentity);
                        StructureUtils.clearSpaceForStructure(boundingbox, serverlevel);
                        return 1;
                    }
                },
                () -> TestCommand.say(serverlevel, "Could not find any structures to clear", ChatFormatting.RED),
                p_320503_ -> TestCommand.say(commandsourcestack, "Cleared " + p_320503_ + " structures")
            );
            return 1;
        }

        public int export() {
            MutableBoolean mutableboolean = new MutableBoolean(true);
            CommandSourceStack commandsourcestack = this.finder.source();
            ServerLevel serverlevel = commandsourcestack.getLevel();
            this.logAndRun(
                this.finder.findStructureBlockPos(),
                p_320242_ -> {
                    StructureBlockEntity structureblockentity = (StructureBlockEntity)serverlevel.getBlockEntity(p_320242_);
                    if (structureblockentity == null) {
                        TestCommand.say(serverlevel, "Structure block entity could not be found", ChatFormatting.RED);
                        mutableboolean.setFalse();
                        return 0;
                    } else {
                        if (TestCommand.saveAndExportTestStructure(commandsourcestack, structureblockentity) != 0) {
                            mutableboolean.setFalse();
                        }

                        return 1;
                    }
                },
                () -> TestCommand.say(serverlevel, "Could not find any structures to export", ChatFormatting.RED),
                p_320666_ -> TestCommand.say(commandsourcestack, "Exported " + p_320666_ + " structures")
            );
            return mutableboolean.getValue() ? 0 : 1;
        }

        int verify() {
            TestCommand.stopTests();
            CommandSourceStack commandsourcestack = this.finder.source();
            ServerLevel serverlevel = commandsourcestack.getLevel();
            BlockPos blockpos = TestCommand.createTestPositionAround(commandsourcestack);
            Collection<GameTestInfo> collection = Stream.concat(
                    TestCommand.toGameTestInfos(commandsourcestack, RetryOptions.noRetries(), this.finder),
                    TestCommand.toGameTestInfo(commandsourcestack, RetryOptions.noRetries(), this.finder, 0)
                )
                .toList();
            int i = 10;
            GameTestRunner.clearMarkers(serverlevel);
            GameTestRegistry.forgetFailedTests();
            Collection<GameTestBatch> collection1 = new ArrayList<>();

            for (GameTestInfo gametestinfo : collection) {
                for (Rotation rotation : Rotation.values()) {
                    Collection<GameTestInfo> collection2 = new ArrayList<>();

                    for (int j = 0; j < 100; j++) {
                        GameTestInfo gametestinfo1 = new GameTestInfo(gametestinfo.getTestFunction(), rotation, serverlevel, new RetryOptions(1, true));
                        collection2.add(gametestinfo1);
                    }

                    GameTestBatch gametestbatch = GameTestBatchFactory.toGameTestBatch(
                        collection2, gametestinfo.getTestFunction().batchName(), (long)rotation.ordinal()
                    );
                    collection1.add(gametestbatch);
                }
            }

            StructureGridSpawner structuregridspawner = new StructureGridSpawner(blockpos, 10, true);
            GameTestRunner gametestrunner = GameTestRunner.Builder.fromBatches(collection1, serverlevel)
                .batcher(GameTestBatchFactory.fromGameTestInfo(100))
                .newStructureSpawner(structuregridspawner)
                .existingStructureSpawner(structuregridspawner)
                .haltOnError(true)
                .build();
            return TestCommand.trackAndStartRunner(commandsourcestack, serverlevel, gametestrunner);
        }

        public int run(RetryOptions retryOptions, int rotationSteps, int testsPerRow) {
            TestCommand.stopTests();
            CommandSourceStack commandsourcestack = this.finder.source();
            ServerLevel serverlevel = commandsourcestack.getLevel();
            BlockPos blockpos = TestCommand.createTestPositionAround(commandsourcestack);
            Collection<GameTestInfo> collection = Stream.concat(
                    TestCommand.toGameTestInfos(commandsourcestack, retryOptions, this.finder),
                    TestCommand.toGameTestInfo(commandsourcestack, retryOptions, this.finder, rotationSteps)
                )
                .toList();
            if (collection.isEmpty()) {
                TestCommand.say(commandsourcestack, "No tests found");
                return 0;
            } else {
                GameTestRunner.clearMarkers(serverlevel);
                GameTestRegistry.forgetFailedTests();
                TestCommand.say(commandsourcestack, "Running " + collection.size() + " tests...");
                GameTestRunner gametestrunner = GameTestRunner.Builder.fromInfo(collection, serverlevel)
                    .newStructureSpawner(new StructureGridSpawner(blockpos, testsPerRow, false))
                    .build();
                return TestCommand.trackAndStartRunner(commandsourcestack, serverlevel, gametestrunner);
            }
        }

        public int run(int rotationSteps, int testsPerRow) {
            return this.run(RetryOptions.noRetries(), rotationSteps, testsPerRow);
        }

        public int run(int rotationSteps) {
            return this.run(RetryOptions.noRetries(), rotationSteps, 8);
        }

        public int run(RetryOptions retryOptions, int rotationSteps) {
            return this.run(retryOptions, rotationSteps, 8);
        }

        public int run(RetryOptions retryOptions) {
            return this.run(retryOptions, 0, 8);
        }

        public int run() {
            return this.run(RetryOptions.noRetries());
        }

        public int locate() {
            TestCommand.say(this.finder.source(), "Started locating test structures, this might take a while..");
            MutableInt mutableint = new MutableInt(0);
            BlockPos blockpos = BlockPos.containing(this.finder.source().getPosition());
            this.finder
                .findStructureBlockPos()
                .forEach(
                    p_340637_ -> {
                        StructureBlockEntity structureblockentity = (StructureBlockEntity)this.finder.source().getLevel().getBlockEntity(p_340637_);
                        if (structureblockentity != null) {
                            Direction direction = structureblockentity.getRotation().rotate(Direction.NORTH);
                            BlockPos blockpos1 = structureblockentity.getBlockPos().relative(direction, 2);
                            int j = (int)direction.getOpposite().toYRot();
                            String s = String.format("/tp @s %d %d %d %d 0", blockpos1.getX(), blockpos1.getY(), blockpos1.getZ(), j);
                            int k = blockpos.getX() - p_340637_.getX();
                            int l = blockpos.getZ() - p_340637_.getZ();
                            int i1 = Mth.floor(Mth.sqrt((float)(k * k + l * l)));
                            Component component = ComponentUtils.wrapInSquareBrackets(
                                    Component.translatable("chat.coordinates", p_340637_.getX(), p_340637_.getY(), p_340637_.getZ())
                                )
                                .withStyle(
                                    p_340633_ -> p_340633_.withColor(ChatFormatting.GREEN)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, s))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                                );
                            Component component1 = Component.literal("Found structure at: ").append(component).append(" (distance: " + i1 + ")");
                            this.finder.source().sendSuccess(() -> component1, false);
                            mutableint.increment();
                        }
                    }
                );
            int i = mutableint.intValue();
            if (i == 0) {
                TestCommand.say(this.finder.source().getLevel(), "No such test structure found", ChatFormatting.RED);
                return 0;
            } else {
                TestCommand.say(this.finder.source().getLevel(), "Finished locating, found " + i + " structure(s)", ChatFormatting.GREEN);
                return 1;
            }
        }
    }

    static record TestBatchSummaryDisplayer(CommandSourceStack source) implements GameTestBatchListener {
        @Override
        public void testBatchStarting(GameTestBatch p_319827_) {
            TestCommand.say(this.source, "Starting batch: " + p_319827_.name());
        }

        @Override
        public void testBatchFinished(GameTestBatch p_320779_) {
        }
    }

    public static record TestSummaryDisplayer(ServerLevel level, MultipleTestTracker tracker) implements GameTestListener {
        @Override
        public void testStructureLoaded(GameTestInfo testInfo) {
        }

        @Override
        public void testPassed(GameTestInfo test, GameTestRunner runner) {
            showTestSummaryIfAllDone(this.level, this.tracker);
        }

        @Override
        public void testFailed(GameTestInfo test, GameTestRunner runner) {
            showTestSummaryIfAllDone(this.level, this.tracker);
        }

        @Override
        public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
            this.tracker.addTestToTrack(newTest);
        }

        private static void showTestSummaryIfAllDone(ServerLevel level, MultipleTestTracker tracker) {
            if (tracker.isDone()) {
                TestCommand.say(level, "GameTest done! " + tracker.getTotalCount() + " tests were run", ChatFormatting.WHITE);
                if (tracker.hasFailedRequired()) {
                    TestCommand.say(level, tracker.getFailedRequiredCount() + " required tests failed :(", ChatFormatting.RED);
                } else {
                    TestCommand.say(level, "All required tests passed :)", ChatFormatting.GREEN);
                }

                if (tracker.hasFailedOptional()) {
                    TestCommand.say(level, tracker.getFailedOptionalCount() + " optional tests failed", ChatFormatting.GRAY);
                }
            }
        }
    }
}
