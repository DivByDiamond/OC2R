
package li.cil.oc2.data;

import li.cil.oc2.api.API;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

import static li.cil.oc2.common.block.Blocks.*;
import static li.cil.oc2.common.tags.BlockTags.*;

public final class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(final PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable final ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DEVICES).add(
            COMPUTER.get(),
            REDSTONE_INTERFACE.get(),
            DISK_DRIVE.get(),
            PROJECTOR.get()
        );
        tag(CABLES).add(
            BUS_CABLE.get()
        );
        tag(WRENCH_BREAKABLE).add(
            COMPUTER.get(),
            BUS_CABLE.get(),
            NETWORK_CONNECTOR.get(),
            NETWORK_HUB.get(),
            REDSTONE_INTERFACE.get(),
            DISK_DRIVE.get(),
            CHARGER.get(),
            PROJECTOR.get()
        );
    }
}
