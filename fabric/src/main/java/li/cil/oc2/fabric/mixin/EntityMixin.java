package li.cil.oc2.fabric.mixin;

import li.cil.oc2.fabric.PersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no per-entity persistent NBT; NeoForge keeps one in {@code Entity#getPersistentData()}.
 * This stores it next to the entity's other data so it survives saving, loading and relogging.
 */
@Mixin(Entity.class)
public abstract class EntityMixin implements PersistentDataHolder {
    @Unique
    private static final String OC2R_PERSISTENT_DATA_KEY = "oc2r_persistent_data";

    @Unique
    private CompoundTag oc2r$persistentData;

    @Override
    public CompoundTag oc2r$getPersistentData() {
        if (oc2r$persistentData == null) {
            oc2r$persistentData = new CompoundTag();
        }
        return oc2r$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void oc2r$savePersistentData(final CompoundTag tag, final CallbackInfoReturnable<CompoundTag> cir) {
        if (oc2r$persistentData != null && !oc2r$persistentData.isEmpty()) {
            cir.getReturnValue().put(OC2R_PERSISTENT_DATA_KEY, oc2r$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void oc2r$loadPersistentData(final CompoundTag tag, final CallbackInfo ci) {
        if (tag.contains(OC2R_PERSISTENT_DATA_KEY, 10)) {
            oc2r$persistentData = tag.getCompound(OC2R_PERSISTENT_DATA_KEY).copy();
        }
    }
}
