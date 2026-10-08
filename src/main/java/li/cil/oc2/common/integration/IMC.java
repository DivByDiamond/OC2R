package li.cil.oc2.common.integration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import li.cil.oc2.api.API;
import li.cil.oc2.api.imc.RPCMethodParameterTypeAdapter;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
//? if >=26.1 {
/*import net.minecraft.util.Util;
*///?} else {
import net.minecraft.Util;
//?}
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles inter-mod messages. Loader-independent: NeoForge feeds it from {@code InterModComms}, other
 * loaders have no such mechanism and simply never call {@link #handleMessages}.
 */
public final class IMC {
    /** A loader-independent inter-mod message. */
    public record Message(String senderModId, String method, Supplier<?> messageSupplier) {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, Consumer<Message>> METHODS =
            Util.make(
                    () -> {
                        Map<String, Consumer<Message>> map = new ConcurrentHashMap<>();

                        map.put(
                                API.IMC_ADD_RPC_METHOD_PARAMETER_TYPE_ADAPTER,
                                IMC::addRPCMethodParameterTypeAdapter);

                        return map;
                    });

    public static void handleMessages(final Stream<Message> messages) {
        messages
                .forEach(
                        message -> {
                            final Consumer<Message> method =
                                    METHODS.get(message.method());
                            if (method != null) {
                                method.accept(message);
                            } else {
                                LOGGER.error(
                                        "Received unknown IMC message [{}] from mod [{}],"
                                                + " ignoring.",
                                        message.method(),
                                        message.senderModId());
                            }
                        });
    }

    private static void addRPCMethodParameterTypeAdapter(final Message message) {
        getMessageParameter(message, RPCMethodParameterTypeAdapter.class)
                .ifPresent(
                        value -> {
                            try {
                                RPCMethodParameterTypeAdapters.addTypeAdapter(value);
                            } catch (final IllegalArgumentException e) {
                                LOGGER.error(
                                        "Received invalid type adapter registration [{}] for type"
                                                + " [{}] from mod [{}].",
                                        value.typeAdapter(),
                                        value.type(),
                                        message.senderModId());
                            }
                        });
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <T> Optional<T> getMessageParameter(
            final Message message, final Class<T> type) {
        final Object value = message.messageSupplier().get();
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        } else {
            LOGGER.error(
                    "Received incompatible parameter [{}] for IMC message [{}] from mod [{}]."
                            + " Expected type is [{}].",
                    message.messageSupplier().get(),
                    message.method(),
                    message.senderModId(),
                    type);
            return Optional.empty();
        }
    }
}