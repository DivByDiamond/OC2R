package li.cil.oc2.common.blockentity.radio;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.radio.RadioAddress;
import li.cil.oc2.common.radio.RadioRegistry;
import li.cil.oc2.common.radio.RadioTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

class RadioNetworkDataTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void revokedUnloadedShortestRelayIsPrunedSoValidLongerRouteRemainsUsable() {
        final RadioNetworkData data = new RadioNetworkData();
        final var network = data.registry().createNetwork();
        final int range = Config.gpsTowerRange;
        final UUID source = enroll(data, network, 0, 0, true);
        final UUID revokedRelay = enroll(data, network, range, 0, false);
        final UUID target = enroll(data, network, range * 2, 0, true);
        final UUID first = enroll(data, network, 0, range, false);
        final UUID second = enroll(data, network, range, range, false);
        final UUID third = enroll(data, network, range * 2, range, false);
        assertEquals(List.of(source, revokedRelay, target), data.topology().route(source, target));
        data.registry().revoke(network.network(), network.adminSecret(), revokedRelay);
        final var blockReads = new java.util.concurrent.atomic.AtomicInteger();
        data.setDirty(false);
        data.reconcile(
                pos -> pos.getZ() == 0 && pos.getX() != range,
                node -> {
                    blockReads.incrementAndGet();
                    return true;
                });
        assertTrue(data.isDirty());
        assertTrue(data.topology().node(revokedRelay).isEmpty());
        assertEquals(2, blockReads.get());
        assertEquals(
                List.of(source, first, second, third, target),
                data.topology().route(source, target));
        final var scheduler =
                new li.cil.oc2.common.radio.RadioScheduler(
                        data.registry(),
                        data.topology(),
                        li.cil.oc2.common.radio.RadioScheduler.Config.defaults());
        assertEquals(
                li.cil.oc2.common.radio.RadioScheduler.Result.ACCEPTED,
                scheduler.submit(UUID.randomUUID(), source, target, new byte[64], 16, 0));
        final var deliveries = new java.util.concurrent.atomic.AtomicInteger();
        scheduler.tick(10, (from, to, frame, ttl) -> deliveries.incrementAndGet());
        assertEquals(1, deliveries.get());
    }

    private static UUID enroll(
            final RadioNetworkData data,
            final RadioRegistry.CreatedNetwork network,
            final int x,
            final int z,
            final boolean online) {
        final UUID endpoint = UUID.randomUUID();
        final var invitation =
                data.registry()
                        .invite(
                                network.network(),
                                network.adminSecret(),
                                OVERWORLD,
                                endpoint,
                                Set.of());
        data.registry().enroll(invitation.invite(), invitation.secret());
        data.register(
                new RadioTopology.Node(
                        endpoint,
                        network.network(),
                        OVERWORLD,
                        x,
                        64,
                        z,
                        Config.gpsTowerRange,
                        online));
        return endpoint;
    }

    @Test
    void unchangedNodesDoNotDirtyAndCopiedIdentitiesCannotMoveTheirSavedEndpoint() {
        final RadioNetworkData data = new RadioNetworkData();
        final UUID device = UUID.randomUUID();
        final UUID network = UUID.randomUUID();
        final var original =
                new RadioTopology.Node(
                        device, network, OVERWORLD, 10, 64, 10, 45, true);
        assertTrue(data.register(original));
        data.setDirty(false);
        assertTrue(data.register(original));
        assertFalse(data.isDirty());
        assertFalse(
                data.register(
                        new RadioTopology.Node(
                                device, network, OVERWORLD, 20, 64, 10, 45, true)));
        assertEquals(original, data.topology().nodes().getFirst());
    }

    @Test
    void nbtRoundTripRetainsAuthenticationAndUnloadedRelaysWithoutBearerSecrets() {
        final RadioNetworkData data = new RadioNetworkData();
        final var network = data.registry().createNetwork();
        final UUID endpoint = UUID.randomUUID();
        final var invitation =
                data.registry()
                        .invite(
                                network.network(),
                                network.adminSecret(),
                                OVERWORLD,
                                endpoint,
                                Set.of());
        final var enrollment = data.registry().enroll(invitation.invite(), invitation.secret());
        data.register(
                new RadioTopology.Node(
                        endpoint, network.network(), OVERWORLD, 10, 250, 20, 45, true));
        final var nbt = RadioNetworkData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        final var payload = NbtOps.INSTANCE.getMap(nbt).getOrThrow().get("radio");
        final String json = StandardCharsets.UTF_8.decode(
                NbtOps.INSTANCE.getByteBuffer(payload).getOrThrow()).toString();
        assertFalse(json.contains(network.adminSecret()));
        assertFalse(json.contains(enrollment.secret()));
        final RadioNetworkData restored =
                RadioNetworkData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        assertTrue(restored.registry().authenticate(endpoint, enrollment.secret()).isPresent());
        assertEquals(data.registry().address(endpoint), restored.registry().address(endpoint));
        assertEquals(1, restored.topology().nodes().size());
        assertFalse(restored.topology().nodes().getFirst().online());
        restored.removeAt(new BlockPos(10, 250, 20));
        assertTrue(restored.topology().nodes().isEmpty());
    }

    @Test
    void nbtRoundTripPreservesRandomAddressReservationsAfterReenrollment() {
        final RadioNetworkData data = new RadioNetworkData();
        final var network = data.registry().createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = data.registry().invite(network.network(), network.adminSecret(),
                OVERWORLD, device, Set.of());
        final var first = data.registry().enroll(invite.invite(), invite.secret());
        data.registry().revoke(network.network(), network.adminSecret(), device);
        final var next = data.registry().invite(network.network(), network.adminSecret(),
                OVERWORLD, device, Set.of());
        final var second = data.registry().enroll(next.invite(), next.secret());
        final var nbt = RadioNetworkData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        final var restored = RadioNetworkData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        assertNotEquals(first.member().number(), second.member().number());
        assertEquals(data.registry().address(device), restored.registry().address(device));
        assertTrue(restored.registry().exportState().reservations().contains(
                new RadioRegistry.Reservation(network.network(), first.member().number())));
        assertTrue(restored.registry().exportState().reservations().contains(
                new RadioRegistry.Reservation(network.network(), second.member().number())));
        assertTrue(restored.registry().authenticate(device, first.secret()).isEmpty());
        assertTrue(restored.registry().authenticate(device, second.secret()).isPresent());
    }

    @Test
    void oldJsonWithoutReservationFieldRetainsAddressAndLegacyAllocationFloor() {
        final RadioNetworkData data = new RadioNetworkData();
        final var created = data.registry().createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = data.registry().invite(created.network(), created.adminSecret(),
                OVERWORLD, device, Set.of());
        final var enrolled = data.registry().enroll(invite.invite(), invite.secret());
        final var original = data.registry().exportState().networks().getFirst();
        final var network = new RadioRegistry.Network(original.id(), 17, original.adminHash(), 4);
        final var member = new RadioRegistry.Member(device, original.id(), OVERWORLD,
                3, 1, enrolled.member().credentialHash(), Set.of(), false);
        final var legacy = new RadioRegistry.State(18, List.of(network), List.of(member), List.of());
        final var registryJson = new Gson().toJsonTree(legacy).getAsJsonObject();
        registryJson.remove("reservations");
        final var snapshot = new JsonObject();
        snapshot.add("registry", registryJson);
        snapshot.add("nodes", new JsonArray());
        final CompoundTag tag = new CompoundTag();
        tag.putString("radio", snapshot.toString());
        final var restored = RadioNetworkData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        assertEquals(new RadioAddress(17, 3), restored.registry().address(device));
        assertTrue(restored.registry().authenticate(device, enrolled.secret()).isPresent());
        assertEquals(4, restored.registry().exportState().networks().getFirst().nextDevice());
        assertTrue(restored.registry().exportState().reservations().isEmpty());
        restored.registry().revoke(network.id(), created.adminSecret(), device);
        final var next = restored.registry().invite(network.id(), created.adminSecret(),
                OVERWORLD, device, Set.of());
        final var reenrolled = restored.registry().enroll(next.invite(), next.secret());
        assertTrue(reenrolled.member().number() >= 4);
    }

    @Test
    void largeReservationHistorySurvivesActualBinaryNbtSerialization() throws IOException {
        final RadioNetworkData data = new RadioNetworkData();
        final var network = data.registry().createNetwork();
        final UUID device = UUID.randomUUID();
        final var invite = data.registry().invite(network.network(), network.adminSecret(),
                OVERWORLD, device, Set.of());
        final var enrolled = data.registry().enroll(invite.invite(), invite.secret());
        final var saved = data.registry().exportState();
        final var reservations = new java.util.HashSet<>(saved.reservations());
        for (int number = 1; number <= 2000; number++)
            reservations.add(new RadioRegistry.Reservation(network.network(), number));
        final var history = new RadioRegistry.State(saved.nextPrefix(), saved.networks(),
                saved.members(), saved.invites(), List.copyOf(reservations));
        final var snapshot = new JsonObject();
        snapshot.add("registry", new Gson().toJsonTree(history));
        snapshot.add("nodes", new JsonArray());
        final CompoundTag legacy = new CompoundTag();
        legacy.putString("radio", snapshot.toString());
        final var populated = RadioNetworkData.CODEC.parse(NbtOps.INSTANCE, legacy).getOrThrow();
        final var encoded = (CompoundTag) RadioNetworkData.CODEC
                .encodeStart(NbtOps.INSTANCE, populated).getOrThrow();
        final var bytes = new ByteArrayOutputStream();
        NbtIo.write(encoded, new DataOutputStream(bytes));
        assertTrue(bytes.size() > 65535);
        final var binary = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),
                NbtAccounter.unlimitedHeap());
        final var restored = RadioNetworkData.CODEC.parse(NbtOps.INSTANCE, binary).getOrThrow();
        assertEquals(reservations, Set.copyOf(restored.registry().exportState().reservations()));
        assertEquals(populated.registry().address(device), restored.registry().address(device));
        assertTrue(restored.registry().authenticate(device, enrolled.secret()).isPresent());
    }

}
