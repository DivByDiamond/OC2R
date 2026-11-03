package li.cil.oc2.common.blockentity.radio;

import li.cil.oc2.common.radio.RadioAddress;
import li.cil.oc2.common.radio.service.RadioMessage;
import li.cil.oc2.common.radio.service.RadioServiceBroker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/** A client identity is bound to its host; it never enters the saved relay topology. */
public final class OraEndpoint implements RadioServiceBroker.Endpoint {
    private final Supplier<ServerLevel> level;
    private final Supplier<UUID> owner;
    private final java.util.function.DoubleSupplier x, z;
    private final java.util.function.BooleanSupplier online;
    private UUID id = UUID.randomUUID(), binding;
    private String credential = "";
    private RadioServiceBroker.Session session;
    private long sessionGeneration = -1;
    private final ArrayDeque<RadioMessage> messages = new ArrayDeque<>();
    private final Map<UUID, RadioServiceBroker.Command> commands = new LinkedHashMap<>();
    private final Set<String> services = new HashSet<>();
    private byte[] tlsPrivateMaterial = new byte[0];

    public OraEndpoint(
            Supplier<ServerLevel> level,
            Supplier<UUID> owner,
            java.util.function.DoubleSupplier x,
            java.util.function.DoubleSupplier z,
            java.util.function.BooleanSupplier online) {
        this.level = level;
        this.owner = owner;
        this.x = x;
        this.z = z;
        this.online = online;
    }

    public UUID id() {
        validateBinding();
        return id;
    }

    public String credential() {
        validateBinding();
        return credential;
    }

    public boolean online() {
        return online.getAsBoolean();
    }

    public void identity(UUID id, String credential) {
        if (!this.id.equals(id) || !this.credential.equals(credential)) {
            if (session != null) session.close();
            session = null;
            this.id = id;
            this.credential = credential;
        }
    }

    public double x() {
        return x.getAsDouble();
    }

    public double z() {
        return z.getAsDouble();
    }

    public byte[] tlsPrivateMaterial() {
        return tlsPrivateMaterial.clone();
    }

    public void tlsPrivateMaterial(byte[] value) {
        tlsPrivateMaterial = value.clone();
    }

    private void validateBinding() {
        if (binding == null) binding = owner.get();
        else if (!binding.equals(owner.get())) {
            if (session != null) session.close();
            session = null;
            id = UUID.randomUUID();
            credential = "";
            binding = owner.get();
            tlsPrivateMaterial = new byte[0];
            commands.clear();
            messages.clear();
            services.clear();
        }
    }

    private RadioNetworkData data() {
        validateBinding();
        return RadioNetworkData.get(level.get());
    }

    public String dimension() {
        //? if >=26.1 {
        /*return level.get().dimension().identifier().toString();
         */
        //?} else {
        return level.get().dimension().location().toString();
        //?}
    }

    public void joinRadioNetwork(String invitation, String secret) {
        var data = data();
        var invitationId = UUID.fromString(invitation);
        var invite =
                data.registry().exportState().invites().stream()
                        .filter(value -> value.id().equals(invitationId))
                        .findFirst()
                        .orElseThrow(() -> new SecurityException("Invalid invitation"));
        if (!invite.device().equals(id) || !invite.dimension().equals(dimension()))
            throw new SecurityException("Invitation belongs to another endpoint");
        credential =
                data.registry().enroll(invitationId, secret, level.get().getGameTime()).secret();
        tlsPrivateMaterial = new byte[0];
        session = null;
        data.setDirty();
        session();
    }

    public boolean enrolled() {
        var data = data();
        return data.registry()
                .authenticate(id, credential)
                .filter(member -> member.dimension().equals(dimension()))
                .isPresent();
    }

    public void tick() {
        commands.values()
                .removeIf(
                        command ->
                                command.state() != RadioServiceBroker.State.ACCEPTED
                                        && command.state() != RadioServiceBroker.State.RUNNING);
        if (enrolled()) session();
    }

    public String address() {
        var data = data();
        if (!enrolled()) throw new SecurityException("Not enrolled");
        return data.registry().address(id).toString();
    }

    public RadioServiceBroker.Session session() {
        var data = data();
        data.attach(this);
        long generation =
                data.registry()
                        .authenticate(id, credential)
                        .orElseThrow(SecurityException::new)
                        .generation();
        if (session == null || sessionGeneration != generation) {
            if (session != null) session.close();
            session = data.broker().connect(id, credential);
            sessionGeneration = generation;
            commands.clear();
            for (String service : services) data.broker().register(id, service, this);
        }
        return session;
    }

    public void register(String service) {
        session();
        data().broker().register(id, service, this);
        services.add(service);
    }

    public void publish(String service, String channel, String payload) {
        if (!services.contains(service)) register(service);
        session();
        data().broker().publish(id, service, channel, bytes(payload));
    }

    public void send(
            String address, String service, String channel, String request, String payload) {
        session()
                .message(
                        RadioAddress.parse(address),
                        service,
                        channel,
                        UUID.fromString(request),
                        bytes(payload));
    }

    public String command(
            String address,
            String service,
            String channel,
            String request,
            String payload,
            int timeout) {
        return session()
                .command(
                        RadioAddress.parse(address),
                        service,
                        channel,
                        UUID.fromString(request),
                        bytes(payload),
                        level.get().getGameTime() + timeout)
                .state()
                .name();
    }

    public String subscribe(String address, String service, String[] fields, int staleTicks) {
        return session()
                .subscribe(RadioAddress.parse(address), service, Set.of(fields), staleTicks)
                .toString();
    }

    public void unsubscribe(String subscription) {
        session().unsubscribe(UUID.fromString(subscription));
    }

    public List<String> discover(String address) {
        return session().services(RadioAddress.parse(address));
    }

    public List<RadioServiceBroker.Peer> peers() {
        return session().peers();
    }

    public RadioServiceBroker.Estimate estimate(String address) {
        return data().broker().estimate(session(), RadioAddress.parse(address));
    }

    public void ping(String address, String request) {
        session().ping(RadioAddress.parse(address), UUID.fromString(request));
    }

    public List<RadioServiceBroker.Event> poll() {
        return session().poll();
    }

    public List<RadioMessage> pollMessages() {
        session();
        var result = List.copyOf(messages);
        messages.clear();
        return result;
    }

    public void commandStatus(String request, String state, String result) {
        session();
        var command = commands.get(UUID.fromString(request));
        if (command == null) throw new IllegalArgumentException("Unknown command");
        switch (state) {
            case "RUNNING" -> command.running();
            case "COMPLETED" -> command.completed(bytes(result));
            case "FAILED" -> command.failed(bytes(result));
            default -> throw new IllegalArgumentException("Invalid command state");
        }
        if (!state.equals("RUNNING")) commands.remove(UUID.fromString(request));
    }

    @Override
    public void message(RadioMessage message) {
        if (messages.size() >= 256) throw new IllegalStateException("Inbox full");
        messages.add(message);
    }

    @Override
    public void command(RadioMessage message, RadioServiceBroker.Command command) {
        if (commands.containsKey(message.requestId())
                || commands.size() >= 256
                || messages.size() >= 256) {
            command.failed(bytes("Inbox full"));
            return;
        }
        commands.put(message.requestId(), command);
        messages.add(message);
    }

    public Map<String, Object> status() {
        var data = data();
        var enrolled = data.registry().authenticate(id, credential).isPresent();
        return Map.of(
                "endpoint",
                id.toString(),
                "enrolled",
                enrolled,
                "attached",
                enrolled && data.attachment(level.get(), id, credential, x(), z()).isPresent(),
                "applicationSecurity",
                data.broker().security(),
                "ethernetSecurity",
                "plaintext-compatibility",
                "security",
                "trusted-local-plaintext-no-tls");
    }

    public void save(CompoundTag tag) {
        validateBinding();
        tag.putString("ora_endpoint", id.toString());
        tag.putString("ora_owner", binding.toString());
        tag.putString("ora_credential", credential);
        tag.putByteArray("ora_tls", tlsPrivateMaterial);
    }

    public void load(CompoundTag tag) {
        //? if >=26.1 {
        /*String endpoint = tag.getStringOr("ora_endpoint", "");
        String host = tag.getStringOr("ora_owner", "");
        credential = tag.getStringOr("ora_credential", "");
        tlsPrivateMaterial = tag.getByteArray("ora_tls").orElse(new byte[0]);
        */
        //?} else {
        String endpoint = tag.getString("ora_endpoint");
        String host = tag.getString("ora_owner");
        credential = tag.getString("ora_credential");
        tlsPrivateMaterial = tag.getByteArray("ora_tls");
        //?}
        try {
            id = UUID.fromString(endpoint);
            binding = UUID.fromString(host);
        } catch (IllegalArgumentException malformed) {
            id = UUID.randomUUID();
            binding = null;
            credential = "";
            tlsPrivateMaterial = new byte[0];
        }
        session = null;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
