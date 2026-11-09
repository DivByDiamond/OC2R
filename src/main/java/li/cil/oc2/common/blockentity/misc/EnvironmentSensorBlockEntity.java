package li.cil.oc2.common.blockentity.misc;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.LifecycleAwareDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.rpc.IEventSink;
import li.cil.oc2.api.bus.device.rpc.RPCEventSource;
import li.cil.oc2.common.block.misc.EnvironmentSensorBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/** Samples only its own loaded block, at most once per second, and emits environmental changes. */
public final class EnvironmentSensorBlockEntity extends ModBlockEntity
        implements NamedDevice, TickableBlockEntity, RPCEventSource, LifecycleAwareDevice {
    public static final int SAMPLE_INTERVAL_TICKS = 20;
    private final AtomicInteger activeMounts = new AtomicInteger();
    private final Map<IEventSink, UUID> subscribers = new ConcurrentHashMap<>();
    private Map<String, Object> sample = Map.of();
    private Map<String, Object> previousEnvironment = Map.of();
    private long sampledAt = Long.MIN_VALUE;

    public EnvironmentSensorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.ENVIRONMENT_SENSOR.get(), pos, state);
    }

    @Override
    public void setBlockState(final BlockState state) {
        final BlockState previous = getBlockState();
        super.setBlockState(state);
        // The physical socket can move while the block entity itself remains the same object.
        if (level != null && !level.isClientSide()
                && previous.getValue(EnvironmentSensorBlock.FACING) != state.getValue(EnvironmentSensorBlock.FACING)) {
            Platform.capabilities().invalidateBlock(level, worldPosition);
        }
    }

    @Override
    public void serverTick() {
        if (level == null || !isValid()) return;
        final boolean active = activeMounts.get() > 0;
        if (getBlockState().getValue(EnvironmentSensorBlock.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(EnvironmentSensorBlock.LIT, active), 3);
        if (active) sampleIfDue();
    }

    @Override
    public void onDeviceMounted() {
        activeMounts.incrementAndGet();
    }

    @Override
    public void onDeviceUnmounted() {
        if (activeMounts.updateAndGet(count -> Math.max(0, count - 1)) == 0) subscribers.clear();
    }

    @Callback
    public Map<String, Object> getEnvironment() {
        if (!isValid() || level == null) throw new IllegalStateException("Sensor is no longer attached");
        sampleIfDue();
        return sample;
    }

    @Callback
    public int getSampleIntervalTicks() {
        return SAMPLE_INTERVAL_TICKS;
    }

    @Callback
    public Map<String, String> getUnits() {
        return Map.of("temperature", "minecraft_biome_temperature",
                "skyLight", "0..15", "blockLight", "0..15", "dayTime", "ticks",
                "sampledAt", "server_game_ticks");
    }

    // The mutable builder stays on the server thread; only immutable snapshots escape.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private void sampleIfDue() {
        if (level == null || level.isClientSide() || !isValid()) return;
        final long now = level.getGameTime();
        if (sampledAt != Long.MIN_VALUE && now - sampledAt < SAMPLE_INTERVAL_TICKS) return;
        // This block entity is ticked only while its chunk is loaded. No neighboring chunk is queried.
        final var biome = level.getBiome(worldPosition);
        //? if >=26.1 {
        /*final String biomeName = biome.unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
        *///?} else {
        final String biomeName = biome.unwrapKey().map(key -> key.location().toString()).orElse("unknown");
        //?}
        final Map<String, Object> environment = Map.of(
                "biome", biomeName,
                "temperature", biome.value().getBaseTemperature(),
                "skyLight", level.getBrightness(LightLayer.SKY, worldPosition),
                "blockLight", level.getBrightness(LightLayer.BLOCK, worldPosition),
                "raining", level.isRainingAt(worldPosition),
                "thundering", level.isThundering(),
                "skyVisible", level.canSeeSky(worldPosition));
        final Map<String, Object> next = new java.util.HashMap<>(environment);
        //? if >=26.1 {
        /*next.put("dayTime", Math.floorMod(level.getOverworldClockTime(), 24000));
        *///?} else {
        next.put("dayTime", Math.floorMod(level.getDayTime(), 24000));
        //?}
        next.put("sampledAt", now);
        sample = Map.copyOf(next);
        sampledAt = now;
        if (!previousEnvironment.isEmpty() && !previousEnvironment.equals(environment)) {
            final JsonObject event = new JsonObject();
            event.addProperty("event", "environment_changed");
            event.addProperty("sampledAt", now);
            for (final Map.Entry<IEventSink, UUID> subscriber : subscribers.entrySet())
                subscriber.getKey().postEvent(subscriber.getValue(), event);
        }
        previousEnvironment = environment;
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return List.of("environment_sensor");
    }

    @Override
    public void subscribe(final IEventSink sink, final UUID sourceId) {
        subscribers.put(sink, sourceId);
    }

    @Override
    public void unsubscribe(final IEventSink sink) {
        subscribers.remove(sink);
    }

    @Override
    protected void unloadServer(final boolean removed) {
        super.unloadServer(removed);
        activeMounts.set(0);
        subscribers.clear();
    }
}
