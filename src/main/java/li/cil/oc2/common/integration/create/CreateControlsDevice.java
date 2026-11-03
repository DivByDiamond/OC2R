package li.cil.oc2.common.integration.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.integration.Integrations;
import li.cil.oc2.common.util.world.level.LevelUtils;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Numeric and enum scroll-box settings exposed through the existing device bus. */
public final class CreateControlsDevice extends IdentityProxy<BlockEntity> implements NamedDevice {
    private final CreateControlsAccess access;

    // Resolve through the game module loader, independent of a worker thread context.
    @SuppressWarnings("PMD.UseProperClassLoader")
    private static final class Api {
        private static final Optional<CreateControlsAccess> INSTANCE =
                CreateControlsAccess.load(CreateControlsDevice.class.getClassLoader());
    }

    private CreateControlsDevice(final BlockEntity identity, final CreateControlsAccess access) {
        super(identity);
        this.access = access;
    }

    /** Finds public Create scroll controls without linking the optional mod into OC2R. */
    public static Optional<CreateControlsDevice> find(final BlockEntity blockEntity) {
        if (!Integrations.isCreateLoaded()) return Optional.empty();
        return Api.INSTANCE.filter(api -> api.supports(blockEntity) && !api.settings(blockEntity).isEmpty())
                .map(api -> new CreateControlsDevice(blockEntity, api));
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return List.of("create_controls");
    }

    /** Enumerates current scroll settings; re-enumerate after changing the machine configuration. */
    @Callback
    public List<CreateControlSetting> getSettings() {
        final List<?> settings = settings();
        final List<CreateControlSetting> result = new ArrayList<>(settings.size());
        for (int id = 0; id < settings.size(); id++) {
            final Object setting = settings.get(id);
            result.add(new CreateControlSetting(id, access.label(setting), access.value(setting),
                    access.formatted(setting), access.active(setting), access.options(setting)));
        }
        return List.copyOf(result);
    }

    /** Reads a setting ID returned by getSettings. */
    @Callback
    public int getValue(@Parameter("id") final int id) {
        return access.value(setting(id));
    }

    /** Writes through Create's setter and returns the actual clamped value. */
    @Callback
    public int setValue(@Parameter("id") final int id, @Parameter("value") final int value) {
        return access.set(setting(id), value);
    }

    private Object setting(final int id) {
        final List<?> values = settings();
        if (id < 0 || id >= values.size()) throw new IllegalArgumentException("Unknown Create setting ID");
        return values.get(id);
    }

    // A replacement block entity must not compare equal merely by its position or other value fields.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private List<?> settings() {
        final var level = identity.getLevel();
        if (level == null || level.isClientSide() || identity.isRemoved()
                || LevelUtils.getBlockEntityIfChunkExists(level, identity.getBlockPos()) != identity) {
            throw new IllegalStateException("Create block is no longer attached");
        }
        return access.settings(identity);
    }
}
