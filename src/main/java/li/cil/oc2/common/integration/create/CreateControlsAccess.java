package li.cil.oc2.common.integration.create;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;

/** Public Create behaviour API, resolved only when the optional mod is installed. */
final class CreateControlsAccess {
    private static final String PREFIX = "com.simibubi.create.foundation.blockEntity.";
    private final Class<?> smartType;
    private final Class<?> scrollType;
    private final Class<?> optionType;
    private final Method allBehaviours;
    private final Method getValue;
    private final Method setValue;
    private final Method formatValue;
    private final Method isActive;
    private final Method getOption;
    private final Field labelField;

    CreateControlsAccess(final Class<?> smartType, final Class<?> scrollType, final Class<?> optionType)
            throws ReflectiveOperationException {
        this.smartType = smartType;
        this.scrollType = scrollType;
        this.optionType = optionType;
        allBehaviours = smartType.getMethod("getAllBehaviours");
        getValue = scrollType.getMethod("getValue");
        setValue = scrollType.getMethod("setValue", int.class);
        formatValue = scrollType.getMethod("formatValue");
        isActive = scrollType.getMethod("isActive");
        getOption = optionType.getMethod("get");
        labelField = scrollType.getField("label");
    }

    static Optional<CreateControlsAccess> load(final ClassLoader loader) {
        try {
            return Optional.of(new CreateControlsAccess(
                    Class.forName(PREFIX + "SmartBlockEntity", false, loader),
                    Class.forName(PREFIX + "behaviour.scrollValue.ScrollValueBehaviour", false, loader),
                    Class.forName(PREFIX + "behaviour.scrollValue.ScrollOptionBehaviour", false, loader)));
        } catch (final ReflectiveOperationException | LinkageError incompatible) {
            LogManager.getLogger().warn(
                    "Create controls integration disabled: incompatible behaviour API", incompatible);
            return Optional.empty();
        }
    }

    boolean supports(final Object blockEntity) {
        return smartType.isInstance(blockEntity);
    }

    List<?> settings(final Object blockEntity) {
        return ((Collection<?>) invoke(allBehaviours, blockEntity)).stream().filter(scrollType::isInstance).toList();
    }

    int value(final Object setting) {
        return ((Number) invoke(getValue, setting)).intValue();
    }

    int set(final Object setting, final int value) {
        if (!active(setting)) throw new IllegalStateException("Create setting is currently inactive");
        // Create owns clamping, callbacks, dirty marking and client synchronization.
        invoke(setValue, setting, value);
        return value(setting);
    }

    boolean active(final Object setting) {
        return (boolean) invoke(isActive, setting);
    }

    String formatted(final Object setting) {
        return (String) invoke(formatValue, setting);
    }

    String label(final Object setting) {
        try {
            return ((Component) labelField.get(setting)).getString();
        } catch (final IllegalAccessException error) {
            throw new IllegalStateException("Create setting label is inaccessible", error);
        }
    }

    List<String> options(final Object setting) {
        if (!optionType.isInstance(setting)) return List.of();
        final Enum<?> selected = (Enum<?>) invoke(getOption, setting);
        return Arrays.stream(selected.getDeclaringClass().getEnumConstants()).map(Enum::name).toList();
    }

    private static Object invoke(final Method method, final Object target, final Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (final InvocationTargetException failure) {
            throw new IllegalStateException(failure.getCause().getMessage(), failure);
        } catch (final ReflectiveOperationException failure) {
            throw new IllegalStateException("Create behaviour API is inaccessible", failure);
        }
    }
}
