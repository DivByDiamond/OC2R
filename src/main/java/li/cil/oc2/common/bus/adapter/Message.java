package li.cil.oc2.common.bus.adapter;

import javax.annotation.Nullable;

public record Message(String type, @Nullable Object data) {
    // Device -> VM
    public static final String MESSAGE_TYPE_LIST = "list";
    public static final String MESSAGE_TYPE_METHODS = "methods";
    public static final String MESSAGE_TYPE_RESULT = "result";
    public static final String MESSAGE_TYPE_ERROR = "error";
    public static final String MESSAGE_TYPE_EVENT = "event";

    // VM -> Device
    public static final String MESSAGE_TYPE_INVOKE_METHOD = "invoke";
    public static final String MESSAGE_TYPE_SUBSCRIBE = "subscribe";
    public static final String MESSAGE_TYPE_UNSUBSCRIBE = "unsubscribe";
}