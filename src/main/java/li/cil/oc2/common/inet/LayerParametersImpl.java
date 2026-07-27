package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.InternetManager;
import li.cil.oc2.api.inet.LayerParameters;

import net.minecraft.nbt.Tag;

import java.util.Optional;

public record LayerParametersImpl(Optional<Tag> getSavedState, InternetManager getInternetManager)
        implements LayerParameters {}
