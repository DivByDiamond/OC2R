package li.cil.oc2.api.inet;

import java.util.Optional;
import net.minecraft.nbt.Tag;

public interface LayerParameters {
    Optional<Tag> getSavedState();

    InternetManager getInternetManager();
}