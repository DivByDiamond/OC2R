package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;

public class GameTestBatchFactory {
    private static final int MAX_TESTS_PER_BATCH = 50;

    public static Collection<GameTestBatch> fromTestFunction(Collection<TestFunction> testFunctions, ServerLevel level) {
        Map<String, List<TestFunction>> map = testFunctions.stream().collect(Collectors.groupingBy(TestFunction::batchName));
        return map.entrySet()
            .stream()
            .flatMap(
                p_325551_ -> {
                    String s = p_325551_.getKey();
                    List<TestFunction> list = p_325551_.getValue();
                    return Streams.mapWithIndex(
                        Lists.partition(list, 50).stream(),
                        (p_351706_, p_351707_) -> toGameTestBatch(
                                p_351706_.stream().map(p_320787_ -> toGameTestInfo(p_320787_, 0, level)).toList(), s, p_351707_
                            )
                    );
                }
            )
            .toList();
    }

    public static GameTestInfo toGameTestInfo(TestFunction testFunction, int rotationSteps, ServerLevel level) {
        return new GameTestInfo(testFunction, StructureUtils.getRotationForRotationSteps(rotationSteps), level, RetryOptions.noRetries());
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo() {
        return fromGameTestInfo(50);
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo(int maxTests) {
        return p_351703_ -> {
            Map<String, List<GameTestInfo>> map = p_351703_.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(p_320634_ -> p_320634_.getTestFunction().batchName()));
            return map.entrySet()
                .stream()
                .flatMap(
                    p_351712_ -> {
                        String s = p_351712_.getKey();
                        List<GameTestInfo> list = p_351712_.getValue();
                        return Streams.mapWithIndex(
                            Lists.partition(list, maxTests).stream(), (p_351709_, p_351710_) -> toGameTestBatch(List.copyOf(p_351709_), s, p_351710_)
                        );
                    }
                )
                .toList();
        };
    }

    public static GameTestBatch toGameTestBatch(Collection<GameTestInfo> gameTestInfos, String functionName, long index) {
        Consumer<ServerLevel> consumer = GameTestRegistry.getBeforeBatchFunction(functionName);
        Consumer<ServerLevel> consumer1 = GameTestRegistry.getAfterBatchFunction(functionName);
        return new GameTestBatch(functionName + ":" + index, gameTestInfos, consumer, consumer1);
    }
}
