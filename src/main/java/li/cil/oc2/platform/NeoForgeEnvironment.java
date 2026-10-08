package li.cil.oc2.platform;

import java.nio.file.Path;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

/** {@link PlatformEnvironment} backed by FML. */
public final class NeoForgeEnvironment implements PlatformEnvironment {
    @Override
    public boolean isClient() {
        //? if >=26.1 {
        /*return FMLLoader.getCurrent().getDist() == Dist.CLIENT;
        *///?} else {
        return FMLLoader.getDist() == Dist.CLIENT;
        //?}
    }

    @Override
    public boolean isModLoaded(final String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
