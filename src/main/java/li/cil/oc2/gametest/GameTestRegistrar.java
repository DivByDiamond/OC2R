/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

//? if >=26.1 {
/*import com.mojang.serialization.MapCodec;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import li.cil.oc2.api.API;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

// Registers the in-game tests on Minecraft 26.x, where the vanilla GameTest annotation and NeoForge's
// GameTestHolder/PrefixGameTestTemplate are gone and tests are registered as GameTestInstances through
// RegisterGameTestsEvent. The annotations below keep the test sources identical across versions; each
// annotated method is registered under the id "<holder namespace>:<method name>", the same ids the 1.21
// annotations produce with @PrefixGameTestTemplate(false).
@EventBusSubscriber(modid = API.MOD_ID)
public final class GameTestRegistrar {
    // Every test class in this package. Add new classes here, or their tests are silently skipped.
    private static final List<Class<?>> TEST_CLASSES = List.of(
        DeviceBusTests.class,
        NetworkCableItemTests.class,
        NetworkConnectorTests.class,
        RecipeTests.class,
        RedstoneInterfaceTests.class,
        RegistrationTests.class,
        RobotCarryTests.class,
        RobotTests.class,
        ScannerModuleTests.class,
        TerminalTests.class);

    @SubscribeEvent
    public static void onRegisterGameTests(final RegisterGameTestsEvent event) {
        // The tests are reflective instances without a registered instance type, which the client
        // cannot receive when joining a world; so they only exist on the game test server.
        if (!net.neoforged.neoforge.server.loading.ServerModLoader.isGameTestServer()) {
            return;
        }

        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(API.MOD_ID, "default"),
            new TestEnvironmentDefinition.AllOf());

        for (final Class<?> type : TEST_CLASSES) {
            registerClass(event, environment, type);
        }
    }

    private static void registerClass(final RegisterGameTestsEvent event,
                                      final Holder<TestEnvironmentDefinition<?>> environment,
                                      final Class<?> type) {
        final GameTestHolder holder = type.getAnnotation(GameTestHolder.class);
        final PrefixGameTestTemplate prefix = type.getAnnotation(PrefixGameTestTemplate.class);
        if (holder == null || prefix == null || prefix.value()) {
            throw new IllegalStateException(type.getName()
                + " must be annotated with @GameTestHolder and @PrefixGameTestTemplate(false)");
        }

        for (final Method method : type.getDeclaredMethods()) {
            final GameTest test = method.getAnnotation(GameTest.class);
            if (test == null) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())
                || method.getParameterCount() != 1
                || method.getParameterTypes()[0] != GameTestHelper.class) {
                throw new IllegalStateException(method + " must be static and take a single GameTestHelper");
            }

            final TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.fromNamespaceAndPath(test.templateNamespace(), test.template()),
                test.timeoutTicks(),
                0,
                true);
            event.registerTest(
                Identifier.fromNamespaceAndPath(holder.value(), toIdentifierPath(method.getName())),
                new ReflectiveTest(data, method));
        }
    }

    // Runs one annotated static test method against the helper the framework provides.
    private static final class ReflectiveTest extends GameTestInstance {
        private final Method method;

        ReflectiveTest(final TestData<Holder<TestEnvironmentDefinition<?>>> data, final Method method) {
            super(data);
            this.method = method;
        }

        @Override
        public void run(final GameTestHelper helper) {
            try {
                method.invoke(null, helper);
            } catch (final InvocationTargetException e) {
                // Rethrow the test's own failure unchanged so the framework reports the assertion.
                final Throwable cause = e.getCause();
                if (cause instanceof final RuntimeException runtime) {
                    throw runtime;
                }
                if (cause instanceof final Error error) {
                    throw error;
                }
                throw new IllegalStateException(method + " threw a checked exception", cause);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(method + " is not accessible", e);
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            // Code-defined tests are never serialized; the codec only has to exist.
            return MapCodec.unit(this);
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal(method.getDeclaringClass().getSimpleName() + "." + method.getName());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface GameTest {
        String template();

        String templateNamespace();

        int timeoutTicks() default 100;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface GameTestHolder {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PrefixGameTestTemplate {
        boolean value();
    }

    // Identifier paths only allow [a-z0-9/._-], test methods are camelCase.
    private static String toIdentifierPath(final String methodName) {
        return methodName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    private GameTestRegistrar() {
    }
}
*///?} else {
//?}
