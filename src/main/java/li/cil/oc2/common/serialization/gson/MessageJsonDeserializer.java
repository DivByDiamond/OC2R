
package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import li.cil.oc2.common.bus.adapter.Message;
import li.cil.oc2.common.bus.adapter.MethodInvocation;
import li.cil.oc2.common.bus.adapter.RPCDeviceBusAdapter;

import java.lang.reflect.Type;
import java.util.UUID;

public final class MessageJsonDeserializer implements JsonDeserializer<Message> {
    @Override
    public Message deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final String messageType = jsonObject.get("type").getAsString();
        final Object messageData = switch (messageType) {
            case Message.MESSAGE_TYPE_LIST -> null;
            case Message.MESSAGE_TYPE_SUBSCRIBE -> UUID.fromString(jsonObject.getAsJsonPrimitive("data").getAsString());
            case Message.MESSAGE_TYPE_UNSUBSCRIBE -> UUID.fromString(jsonObject.getAsJsonPrimitive("data").getAsString());
            case Message.MESSAGE_TYPE_METHODS -> UUID.fromString(jsonObject.getAsJsonPrimitive("data").getAsString());
            case Message.MESSAGE_TYPE_INVOKE_METHOD -> context.deserialize(jsonObject.getAsJsonObject("data"), MethodInvocation.class);
            default -> throw new JsonParseException(RPCDeviceBusAdapter.ERROR_UNKNOWN_MESSAGE_TYPE + messageType);
        };

        return new Message(messageType, messageData);
    }
}
