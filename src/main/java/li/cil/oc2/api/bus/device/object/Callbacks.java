package li.cil.oc2.api.bus.device.object;

import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.rpc.AbstractRPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

public final class Callbacks {

    private static final ReentrantLock lock = new ReentrantLock();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Class<?>, List<Method>> METHOD_BY_TYPE =
            Collections.synchronizedMap(new ConcurrentHashMap<>());
    private static final Map<Method, RPCParameter[]> PARAMETERS_BY_METHOD =
            Collections.synchronizedMap(new ConcurrentHashMap<>());

    private Callbacks() {}

    /** Collects Callback-annotated methods and generates RPCMethods. */
    public static List<RPCMethodGroup> collectMethods(final Object methodContainer) {
        final List<Method> reflectedMethods = getMethods(methodContainer.getClass());
        final List<RPCMethodGroup> methods = new ArrayList<>();
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
        lock.lock();
        try {

            return METHOD_BY_TYPE.computeIfAbsent(
                    type,
                    c ->
                            Arrays.stream(c.getMethods())
                                    .filter(m -> m.isAnnotationPresent(Callback.class))
                                    .collect(Collectors.toList()));
        
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("ArrayRecordComponent")
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
            final Documentation documentation =
                    collectDocumentation(target, methodName, annotation);
            final RPCParameter[] parameters =
                    collectParameters(method, documentation.parameterDescriptions());
            return new ConstructorData(
                    methodName,
                    annotation.synchronize(),
                    method.getReturnType(),
                    parameters,
                    MethodHandles.lookup().unreflect(method).bindTo(target),
                    documentation.description(),
                    documentation.returnValueDescription());
        }

        private static Documentation collectDocumentation(
                final Object target, final String methodName, final Callback annotation) {
            String desc =
                    Strings.isNotBlank(annotation.description()) ? annotation.description() : null;
            String retDesc =
                    Strings.isNotBlank(annotation.returnValueDescription())
                            ? annotation.returnValueDescription()
                            : null;
            final Map<String, String> paramDescs = new ConcurrentHashMap<>();
            if (target instanceof final DocumentedDevice dd) {
                final VisitorImpl dv = new VisitorImpl();
                dd.getDeviceDocumentation(dv);
                final VisitorImpl cv = dv.callbacks.get(methodName);
                if (cv == null) {
                    return new Documentation(desc, retDesc, paramDescs);
                }
                if (Strings.isNotBlank(cv.desc)) {
                    desc = cv.desc;
                }
                if (Strings.isNotBlank(cv.retValDesc)) {
                    retDesc = cv.retValDesc;
                }
                paramDescs.putAll(cv.parameterDescriptions);
            }
            return new Documentation(desc, retDesc, paramDescs);
        }

        private static RPCParameter[] collectParameters(
                final Method method, final Map<String, String> paramDescs) {
            return PARAMETERS_BY_METHOD.computeIfAbsent(
                    method,
                    m ->
                            Arrays.stream(m.getParameters())
                                    .map(p -> createParameter(p, paramDescs))
                                    .toArray(RPCParameter[]::new));
        }

        private static RPCParameter createParameter(
                final java.lang.reflect.Parameter p, final Map<String, String> paramDescs) {
            final Parameter a = p.getAnnotation(Parameter.class);
            final String pn =
                    a != null && Strings.isNotBlank(a.value())
                            ? a.value()
                            : p.isNamePresent() ? p.getName() : null;
            final Class<?> pt = p.getType();
            // pn may be null when the class wasn't compiled with -parameters AND the parameter has
            // no @Parameter annotation. ConcurrentHashMap rejects null keys, so we must skip the
            // lookup in that case.
            final String pd =
                    pn != null && paramDescs.containsKey(pn)
                            ? paramDescs.get(pn)
                            : a != null && Strings.isNotBlank(a.description())
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
                public Optional<String> getDescription() {
                    return Optional.ofNullable(pd);
                }
            };
        }
    }

    private record Documentation(
            String description,
            String returnValueDescription,
            Map<String, String> parameterDescriptions) {}

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
        public final Map<String, VisitorImpl> callbacks = new ConcurrentHashMap<>();
        public String desc;
        public String retValDesc;
        public final Map<String, String> parameterDescriptions = new ConcurrentHashMap<>();

        @Override
        public DocumentedDevice.CallbackVisitor visitCallback(final String n) {
            return callbacks.computeIfAbsent(n, u -> new VisitorImpl());
        }

        @Override
        public DocumentedDevice.CallbackVisitor description(final String v) {
            this.desc = v;
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor returnValueDescription(final String v) {
            this.retValDesc = v;
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