package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

public class TestFinder<T> implements StructureBlockPosFinder, TestFunctionFinder {
    static final TestFunctionFinder NO_FUNCTIONS = Stream::empty;
    static final StructureBlockPosFinder NO_STRUCTURES = Stream::empty;
    private final TestFunctionFinder testFunctionFinder;
    private final StructureBlockPosFinder structureBlockPosFinder;
    private final CommandSourceStack source;
    private final Function<TestFinder<T>, T> contextProvider;

    @Override
    public Stream<BlockPos> findStructureBlockPos() {
        return this.structureBlockPosFinder.findStructureBlockPos();
    }

    TestFinder(CommandSourceStack source, Function<TestFinder<T>, T> contextProvider, TestFunctionFinder testFunctionFinder, StructureBlockPosFinder structureBlockPosFinder) {
        this.source = source;
        this.contextProvider = contextProvider;
        this.testFunctionFinder = testFunctionFinder;
        this.structureBlockPosFinder = structureBlockPosFinder;
    }

    T get() {
        return this.contextProvider.apply(this);
    }

    public CommandSourceStack source() {
        return this.source;
    }

    @Override
    public Stream<TestFunction> findTestFunctions() {
        return this.testFunctionFinder.findTestFunctions();
    }

    public static class Builder<T> {
        private final Function<TestFinder<T>, T> contextProvider;
        private final UnaryOperator<Supplier<Stream<TestFunction>>> testFunctionFinderWrapper;
        private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

        public Builder(Function<TestFinder<T>, T> contextProvider) {
            this.contextProvider = contextProvider;
            this.testFunctionFinderWrapper = p_329857_ -> p_329857_;
            this.structureBlockPosFinderWrapper = p_329858_ -> p_329858_;
        }

        private Builder(
            Function<TestFinder<T>, T> contextProvider, UnaryOperator<Supplier<Stream<TestFunction>>> testFunctionFinderWrapper, UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper
        ) {
            this.contextProvider = contextProvider;
            this.testFunctionFinderWrapper = testFunctionFinderWrapper;
            this.structureBlockPosFinderWrapper = structureBlockPosFinderWrapper;
        }

        public TestFinder.Builder<T> createMultipleCopies(int count) {
            return new TestFinder.Builder<>(this.contextProvider, createCopies(count), createCopies(count));
        }

        private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int count) {
            return p_329860_ -> {
                List<Q> list = new LinkedList<>();
                List<Q> list1 = ((Stream)p_329860_.get()).toList();

                for (int i = 0; i < count; i++) {
                    list.addAll(list1);
                }

                return list::stream;
            };
        }

        private T build(CommandSourceStack source, TestFunctionFinder testFunctionFinder, StructureBlockPosFinder structureBlockPosFinder) {
            return new TestFinder<>(
                    source,
                    this.contextProvider,
                    this.testFunctionFinderWrapper.apply(testFunctionFinder::findTestFunctions)::get,
                    this.structureBlockPosFinderWrapper.apply(structureBlockPosFinder::findStructureBlockPos)::get
                )
                .get();
        }

        public T radius(CommandContext<CommandSourceStack> context, int radius) {
            CommandSourceStack commandsourcestack = context.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findStructureBlocks(blockpos, radius, commandsourcestack.getLevel())
            );
        }

        public T nearest(CommandContext<CommandSourceStack> context) {
            CommandSourceStack commandsourcestack = context.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.findNearestStructureBlock(blockpos, 15, commandsourcestack.getLevel()).stream()
            );
        }

        public T allNearby(CommandContext<CommandSourceStack> context) {
            CommandSourceStack commandsourcestack = context.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findStructureBlocks(blockpos, 200, commandsourcestack.getLevel())
            );
        }

        public T lookedAt(CommandContext<CommandSourceStack> context) {
            CommandSourceStack commandsourcestack = context.getSource();
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.lookedAtStructureBlockPos(
                        BlockPos.containing(commandsourcestack.getPosition()), commandsourcestack.getPlayer().getCamera(), commandsourcestack.getLevel()
                    )
            );
        }

        public T allTests(CommandContext<CommandSourceStack> context) {
            return this.build(
                context.getSource(),
                () -> GameTestRegistry.getAllTestFunctions().stream().filter(p_329855_ -> !p_329855_.manualOnly()),
                TestFinder.NO_STRUCTURES
            );
        }

        public T allTestsInClass(CommandContext<CommandSourceStack> context, String className) {
            return this.build(
                context.getSource(),
                () -> GameTestRegistry.getTestFunctionsForClassName(className).filter(p_329856_ -> !p_329856_.manualOnly()),
                TestFinder.NO_STRUCTURES
            );
        }

        public T failedTests(CommandContext<CommandSourceStack> context, boolean onlyRequired) {
            return this.build(
                context.getSource(),
                () -> GameTestRegistry.getLastFailedTests().filter(p_320430_ -> !onlyRequired || p_320430_.required()),
                TestFinder.NO_STRUCTURES
            );
        }

        public T byArgument(CommandContext<CommandSourceStack> context, String argumentName) {
            return this.build(context.getSource(), () -> Stream.of(TestFunctionArgument.getTestFunction(context, argumentName)), TestFinder.NO_STRUCTURES);
        }

        public T locateByName(CommandContext<CommandSourceStack> context, String name) {
            CommandSourceStack commandsourcestack = context.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.findStructureByTestFunction(blockpos, 1024, commandsourcestack.getLevel(), name)
            );
        }

        public T failedTests(CommandContext<CommandSourceStack> context) {
            return this.failedTests(context, false);
        }
    }
}
