package li.cil.oc2.api.inet;

import java.util.Optional;
import net.minecraft.nbt.Tag;

public interface InternetDeviceLifecycle {
    default Optional<Tag> onSave() {
        return Optional.empty();
    }

    default void onStop() {}
}