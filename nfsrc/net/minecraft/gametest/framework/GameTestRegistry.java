package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

public class GameTestRegistry {
    private static final Collection<TestFunction> TEST_FUNCTIONS = Lists.newArrayList();
    private static final Set<String> TEST_CLASS_NAMES = Sets.newHashSet();
    private static final Map<String, Consumer<ServerLevel>> BEFORE_BATCH_FUNCTIONS = Maps.newHashMap();
    private static final Map<String, Consumer<ServerLevel>> AFTER_BATCH_FUNCTIONS = Maps.newHashMap();
    private static final Set<TestFunction> LAST_FAILED_TESTS = Sets.newHashSet();

    /**
 * @deprecated Forge: Use {@link
 *             net.neoforged.neoforge.event.RegisterGameTestsEvent
 *             RegisterGameTestsEvent} to register game tests
 */
    @Deprecated
    public static void register(Class<?> testClass) {
        Arrays.stream(testClass.getDeclaredMethods()).sorted(Comparator.comparing(Method::getName)).forEach(GameTestRegistry::register);
    }

    /**
 * @deprecated Forge: Use {@link
 *             net.neoforged.neoforge.event.RegisterGameTestsEvent
 *             RegisterGameTestsEvent} to register game tests
 */
    @Deprecated
    public static void register(Method testMethod) {
         register(testMethod, java.util.Set.of());
    }
    /**
 * @deprecated Forge: Use {@link
 *             net.neoforged.neoforge.event.RegisterGameTestsEvent
 *             RegisterGameTestsEvent} to register game tests
 */
    @Deprecated
    public static void register(Method testMethod, java.util.Set<String> allowedNamespaces) {
        String s = testMethod.getDeclaringClass().getSimpleName();
        GameTest gametest = testMethod.getAnnotation(GameTest.class);
        if (gametest != null && (allowedNamespaces.isEmpty() || allowedNamespaces.contains(net.neoforged.neoforge.gametest.GameTestHooks.getTemplateNamespace(testMethod)))) {
            TEST_FUNCTIONS.add(turnMethodIntoTestFunction(testMethod));
            TEST_CLASS_NAMES.add(s);
        }

        GameTestGenerator gametestgenerator = testMethod.getAnnotation(GameTestGenerator.class);
        if (gametestgenerator != null) {
            Collection<TestFunction> testFunctions = new java.util.ArrayList<>(useTestGeneratorMethod(testMethod));
            if (!allowedNamespaces.isEmpty())
                 testFunctions.removeIf(t -> !allowedNamespaces.contains(net.minecraft.resources.ResourceLocation.parse(t.structureName()).getNamespace()));
            TEST_FUNCTIONS.addAll(testFunctions);
            TEST_CLASS_NAMES.add(s);
        }

        registerBatchFunction(testMethod, BeforeBatch.class, BeforeBatch::batch, BEFORE_BATCH_FUNCTIONS);
        registerBatchFunction(testMethod, AfterBatch.class, AfterBatch::batch, AFTER_BATCH_FUNCTIONS);
    }

    private static <T extends Annotation> void registerBatchFunction(
        Method testMethod, Class<T> annotationType, Function<T, String> valueGetter, Map<String, Consumer<ServerLevel>> positioning
    ) {
        T t = testMethod.getAnnotation(annotationType);
        if (t != null) {
            String s = valueGetter.apply(t);
            Consumer<ServerLevel> consumer = positioning.putIfAbsent(s, (Consumer<ServerLevel>)turnMethodIntoConsumer(testMethod));
            if (consumer != null) {
                throw new RuntimeException("Hey, there should only be one " + annotationType + " method per batch. Batch '" + s + "' has more than one!");
            }
        }
    }

    public static Stream<TestFunction> getTestFunctionsForClassName(String className) {
        return TEST_FUNCTIONS.stream().filter(p_127674_ -> isTestFunctionPartOfClass(p_127674_, className));
    }

    public static Collection<TestFunction> getAllTestFunctions() {
        return TEST_FUNCTIONS;
    }

    public static Collection<String> getAllTestClassNames() {
        return TEST_CLASS_NAMES;
    }

    public static boolean isTestClass(String className) {
        return TEST_CLASS_NAMES.contains(className);
    }

    public static Consumer<ServerLevel> getBeforeBatchFunction(String functionName) {
        return BEFORE_BATCH_FUNCTIONS.getOrDefault(functionName, p_319462_ -> {
        });
    }

    public static Consumer<ServerLevel> getAfterBatchFunction(String functionName) {
        return AFTER_BATCH_FUNCTIONS.getOrDefault(functionName, p_319461_ -> {
        });
    }

    public static Optional<TestFunction> findTestFunction(String testName) {
        return getAllTestFunctions().stream().filter(p_319460_ -> p_319460_.testName().equalsIgnoreCase(testName)).findFirst();
    }

    public static TestFunction getTestFunction(String testName) {
        Optional<TestFunction> optional = findTestFunction(testName);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Can't find the test function for " + testName);
        } else {
            return optional.get();
        }
    }

    private static Collection<TestFunction> useTestGeneratorMethod(Method testMethod) {
        try {
            Object object = null;
            if (!java.lang.reflect.Modifier.isStatic(testMethod.getModifiers()))
                 object = testMethod.getDeclaringClass().newInstance();
            return (Collection<TestFunction>)testMethod.invoke(object);
        } catch (ReflectiveOperationException reflectiveoperationexception) {
            throw new RuntimeException(reflectiveoperationexception);
        }
    }

    private static TestFunction turnMethodIntoTestFunction(Method testMethod) {
        GameTest gametest = testMethod.getAnnotation(GameTest.class);
        String s = testMethod.getDeclaringClass().getSimpleName();
        String s1 = s.toLowerCase();
        boolean prefixGameTestTemplate = net.neoforged.neoforge.gametest.GameTestHooks.prefixGameTestTemplate(testMethod);
        String s2 = (prefixGameTestTemplate ? s1 + "." : "") + testMethod.getName().toLowerCase();
        String s3 = net.neoforged.neoforge.gametest.GameTestHooks.getTemplateNamespace(testMethod) + ":" + (gametest.template().isEmpty() ? s2 : (prefixGameTestTemplate ? s1 + "." : "") + gametest.template());
        String s4 = gametest.batch();
        Rotation rotation = StructureUtils.getRotationForRotationSteps(gametest.rotationSteps());
        return new TestFunction(
            s4,
            s2,
            s3,
            rotation,
            gametest.timeoutTicks(),
            gametest.setupTicks(),
            gametest.required(),
            gametest.manualOnly(),
            gametest.requiredSuccesses(),
            gametest.attempts(),
            gametest.skyAccess(),
            (Consumer<GameTestHelper>)turnMethodIntoConsumer(testMethod)
        );
    }

    private static Consumer<?> turnMethodIntoConsumer(Method testMethod) {
        return p_177512_ -> {
            try {
                Object object = null;
                if (!java.lang.reflect.Modifier.isStatic(testMethod.getModifiers()))
                     object = testMethod.getDeclaringClass().newInstance();
                testMethod.invoke(object, p_177512_);
            } catch (InvocationTargetException invocationtargetexception) {
                if (invocationtargetexception.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)invocationtargetexception.getCause();
                } else {
                    throw new RuntimeException(invocationtargetexception.getCause());
                }
            } catch (ReflectiveOperationException reflectiveoperationexception) {
                throw new RuntimeException(reflectiveoperationexception);
            }
        };
    }

    private static boolean isTestFunctionPartOfClass(TestFunction testFunction, String className) {
        return testFunction.testName().toLowerCase().startsWith(className.toLowerCase() + ".");
    }

    public static Stream<TestFunction> getLastFailedTests() {
        return LAST_FAILED_TESTS.stream();
    }

    public static void rememberFailedTest(TestFunction testFunction) {
        LAST_FAILED_TESTS.add(testFunction);
    }

    public static void forgetFailedTests() {
        LAST_FAILED_TESTS.clear();
    }
}
