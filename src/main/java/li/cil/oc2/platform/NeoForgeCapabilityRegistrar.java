package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/** {@link CapabilityRegistrar} wrapping NeoForge's one-shot {@link RegisterCapabilitiesEvent}. */
public final class NeoForgeCapabilityRegistrar implements CapabilityRegistrar {
    private final RegisterCapabilitiesEvent event;

    // The event is valid only for the duration of the one-shot setup callback that constructs
    // this registrar and is never retained past it, so storing the reference cannot leak mutable
    // state to anything outside that call.
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "event outlives only the setup callback")
    public NeoForgeCapabilityRegistrar(final RegisterCapabilitiesEvent event) {
        this.event = event;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerBlock(
            final BlockCapability<T> capability,
            final BlockCapabilityProvider<T> provider,
            final Block... blocks) {
        event.registerBlock(
                NeoForgeCapabilities.blockKey(capability),
                (level, pos, state, blockEntity, context) ->
                        (T)
                                NeoForgeCapabilities.toNeoForge(
                                        capability.getType(),
                                        provider.get(
                                                level,
                                                pos,
                                                state,
                                                blockEntity,
                                                asSide(capability.isSided(), context)),
                                        null),
                blocks);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerEntity(
            final li.cil.oc2.platform.EntityCapability<T> capability,
            final EntityType<?> entityType,
            final EntityCapabilityProvider<T> provider) {
        event.registerEntity(
                NeoForgeCapabilities.entityKey(capability),
                entityType,
                (entity, context) ->
                        (T)
                                NeoForgeCapabilities.toNeoForge(
                                        capability.getType(),
                                        provider.get(entity, asSide(capability.isSided(), context)),
                                        null));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerItem(
            final li.cil.oc2.platform.ItemCapability<T> capability,
            final ItemCapabilityProvider<T> provider,
            final ItemLike... items) {
        event.registerItem(
                NeoForgeCapabilities.itemKey(capability),
                (stack, context) ->
                        (T)
                                NeoForgeCapabilities.toNeoForge(
                                        capability.getType(), provider.get(stack), stack),
                items);
    }

    @Nullable
    private static Direction asSide(final boolean sided, @Nullable final Object context) {
        return sided && context instanceof final Direction direction ? direction : null;
    }
}
