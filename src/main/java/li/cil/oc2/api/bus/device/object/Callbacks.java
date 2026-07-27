package li.cil.oc2.api.bus.device.object;

import static java.util.Objects.requireNonNull;

import li.cil.oc2.api.bus.device.rpc.AbstractRPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public final class Callbacks {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Class<?>, List<Method>> METHOD_BY_TYPE =
            Collections.synchronizedMap(new HashMap<>());
    private static final Map<Method, RPCParameter[]> PARAMETERS_BY_METHOD =
            Collections.synchronizedMap(new HashMap<>());

    private Callbacks() {}

    /** Collects Callback-annotated methods and generates RPCMethods. */
    public static List<RPCMethodGroup> collectMethods(final Object methodContainer) {
        final List<Method> reflectedMethods = getMethods(methodContainer.getClass());
        final ArrayList<RPCMethodGroup> methods = new ArrayList<>();
        for (final Method method : reflectedMethods) {
            try {
                methods.add(new ObjectRpcMethod(methodContainer, method));
            } catch (final IllegalAccessException e) {
                LOGGER.error("Failed accessing method [{}].", method);
            }
        }
        return methods;
    }

    /** Checks if Callback-annotated methods exist on the object. */
    public static boolean hasMethods(final Object object) {
        return object instanceof final Class<?> clazz
                ? !getMethods(clazz).isEmpty()
                : !getMethods(object.getClass()).isEmpty();
    }

    private static List<Method> getMethods(final Class<?> type) {
        synchronized (METHOD_BY_TYPE) {
            return METHOD_BY_TYPE.computeIfAbsent(
                    type,
                    c ->
                            Arrays.stream(c.getMethods())
                                    .filter(m -> m.isAnnotationPresent(Callback.class))
                                    .collect(Collectors.toList()));
        }
    }

    private record ConstructorData(
            String methodName,
            boolean synchronize,
            Class<?> returnType,
            RPCParameter[] parameters,
            MethodHandle handle,
            String description,
            String returnValueDescription) {
        static ConstructorData create(final Object target, final Method method)
                throws IllegalAccessException {
            final Callback annotation =
                    requireNonNull(
                            method.getAnnotation(Callback.class),
                            "Method without Callback annotation.");
            final String methodName =
                    Strings.isNotBlank(annotation.name()) ? annotation.name() : method.getName();
            String desc =
                    Strings.isNotBlank(annotation.description()) ? annotation.description() : null;
            String retDesc =
                    Strings.isNotBlank(annotation.returnValueDescription())
                            ? annotation.returnValueDescription()
                            : null;
            final HashMap<String, String> paramDescs = new HashMap<>();
            if (target instanceof final DocumentedDevice dd) {
                final VisitorImpl dv = new VisitorImpl();
                dd.getDeviceDocumentation(dv);
                final VisitorImpl cv = dv.callbacks.get(methodName);
                if (cv != null) {
                    if (Strings.isNotBlank(cv.description)) {
                        desc = cv.description;
                    }
                    if (Strings.isNotBlank(cv.returnValueDescription)) {
                        retDesc = cv.returnValueDescription;
                    }
                    paramDescs.putAll(cv.parameterDescriptions);
                }
            }
            final RPCParameter[] parameters =
                    PARAMETERS_BY_METHOD.computeIfAbsent(
                            method,
                            m ->
                                    Arrays.stream(m.getParameters())
                                            .map(
                                                    p -> {
                                                        final Parameter a =
                                                                p.getAnnotation(Parameter.class);
                                                        final String pn =
                                                                a != null
                                                                                && Strings
                                                                                        .isNotBlank(
                                                                                                a
                                                                                                        .value())
                                                                        ? a.value()
                                                                        : (p.isNamePresent()
                                                                                ? p.getName()
                                                                                : null);
                                                        final Class<?> pt = p.getType();
                                                        final String pd =
                                                                paramDescs.containsKey(pn)
                                                                        ? paramDescs.get(pn)
                                                                        : a != null
                                                                                        && Strings
                                                                                                .isNotBlank(
                                                                                                        a
                                                                                                                .description())
                                                                                ? a.description()
                                                                                : null;
                                                        return new RPCParameter() {
                                                            @Override
                                                            public Class<?> getType() {
                                                                return pt;
                                                            }

                                                            @Override
                                                            public Optional<String> getName() {
                                                                return Optional.ofNullable(pn);
                                                            }

                                                            @Override
                                                            public Optional<String>
                                                                    getDescription() {
                                                                return Optional.ofNullable(pd);
                                                            }
                                                        };
                                                    })
                                            .toArray(RPCParameter[]::new));
            return new ConstructorData(
                    methodName,
                    annotation.synchronize(),
                    method.getReturnType(),
                    parameters,
                    MethodHandles.lookup().unreflect(method).bindTo(target),
                    desc,
                    retDesc);
        }
    }

    private static final class ObjectRpcMethod extends AbstractRPCMethod {
        private final MethodHandle handle;
        private final String description;
        private final String returnValueDescription;

        ObjectRpcMethod(final Object target, final Method method) throws IllegalAccessException {
            this(ConstructorData.create(target, method));
        }

        private ObjectRpcMethod(final ConstructorData data) throws IllegalAccessException {
            super(data.methodName(), data.synchronize(), data.returnType(), data.parameters());
            this.handle = data.handle();
            this.description = data.description();
            this.returnValueDescription = data.returnValueDescription();
        }

        @Nullable
        @Override
        protected Object invoke(final Object... parameters) throws Throwable {
            return handle.invokeWithArguments(parameters);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getReturnValueDescription() {
            return Optional.ofNullable(returnValueDescription);
        }
    }

    private static final class VisitorImpl
            implements DocumentedDevice.DeviceVisitor, DocumentedDevice.CallbackVisitor {
        public final HashMap<String, VisitorImpl> callbacks = new HashMap<>();
        public String description;
        public String returnValueDescription;
        public final HashMap<String, String> parameterDescriptions = new HashMap<>();

        @Override
        public DocumentedDevice.CallbackVisitor visitCallback(final String n) {
            return callbacks.computeIfAbsent(n, u -> new VisitorImpl());
        }

        @Override
        public DocumentedDevice.CallbackVisitor description(final String v) {
            this.description = v;
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor returnValueDescription(final String v) {
            this.returnValueDescription = v;
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor parameterDescription(
                final String n, final String v) {
            parameterDescriptions.put(n, v);
            return this;
        }
    }
}
