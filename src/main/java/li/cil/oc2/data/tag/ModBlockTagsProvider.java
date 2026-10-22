package li.cil.oc2.data.tag;

import static li.cil.oc2.common.block.common.Blocks.*;
import static li.cil.oc2.common.tags.BlockTags.*;

import java.util.concurrent.CompletableFuture;
//? if <26.1 {
import javax.annotation.Nullable;
//?}
import li.cil.oc2.api.API;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
//? if <26.1 {
import net.neoforged.neoforge.common.data.ExistingFileHelper;
//?}

public final class ModBlockTagsProvider extends BlockTagsProvider {
    //? if >=26.1 {
    /*public ModBlockTagsProvider(
            final PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider, API.MOD_ID);
    }
    *///?} else {
    public ModBlockTagsProvider(
            final PackOutput packOutput,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable final ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, API.MOD_ID, existingFileHelper);
    }
    //?}

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        //? if >=26.2 {
/*        tag(DEVICES)
                .add(COMPUTER.key())
                .add(REDSTONE_INTERFACE.key())
                .add(DISK_DRIVE.key())
                .add(PROJECTOR.key());
        tag(CABLES).add(BUS_CABLE.key());
        tag(WRENCH_BREAKABLE)
                .add(COMPUTER.key())
                .add(BUS_CABLE.key())
                .add(NETWORK_CONNECTOR.key())
                .add(NETWORK_HUB.key())
                .add(REDSTONE_INTERFACE.key())
                .add(DISK_DRIVE.key())
                .add(CHARGER.key())
                .add(PROJECTOR.key());
*///?} else {
        tag(DEVICES)
                .add(COMPUTER.get(), REDSTONE_INTERFACE.get(), DISK_DRIVE.get(), PROJECTOR.get());
        tag(CABLES).add(BUS_CABLE.get());
        tag(WRENCH_BREAKABLE)
                .add(
                        COMPUTER.get(),
                        BUS_CABLE.get(),
                        NETWORK_CONNECTOR.get(),
                        NETWORK_HUB.get(),
                        REDSTONE_INTERFACE.get(),
                        DISK_DRIVE.get(),
                        CHARGER.get(),
                        PROJECTOR.get());
        //?}
    }
}