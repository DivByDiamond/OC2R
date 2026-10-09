package li.cil.oc2.data;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import li.cil.oc2.api.API;
import li.cil.oc2.data.loot.ModLootTableProvider;
//? if <26.1 {
import li.cil.oc2.data.model.ModBlockStateProvider;
import li.cil.oc2.data.model.ModItemModelProvider;
//?}
import li.cil.oc2.data.recipe.ModRecipesProvider;
import li.cil.oc2.data.tag.ModBlockTagsProvider;
import li.cil.oc2.data.tag.ModItemTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//? if <26.1 {
import net.neoforged.neoforge.common.data.ExistingFileHelper;
//?}
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class DataGenerators {
    //? if >=26.1 {
    /*// Model and blockstate generation is not ported to the 26.x model generators yet.
    @SubscribeEvent
    public static void gatherServerData(final GatherDataEvent.Server event) {
        event.createProvider(
                (output, lookupProvider) ->
                        new LootTableProvider(
                                output,
                                Set.of(),
                                List.of(
                                        new LootTableProvider.SubProviderEntry(
                                                ModLootTableProvider.ModBlockLootTables::new,
                                                LootContextParamSets.BLOCK)),
                                lookupProvider));
        event.createBlockAndItemTags(
                ModBlockTagsProvider::new,
                (output, lookupProvider, blockTags) ->
                        new ModItemTagsProvider(output, lookupProvider, blockTags, API.MOD_ID));
        event.createProvider(
                (output, lookupProvider) -> new ModRecipesProvider.Runner(output, lookupProvider));
    }
    *///?} else {
    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();
        final ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(
                event.includeServer(),
                (DataProvider.Factory<LootTableProvider>)
                        output ->
                                new LootTableProvider(
                                        output,
                                        Set.of(),
                                        Collections.singletonList(
                                                new LootTableProvider.SubProviderEntry(
                                                        ModLootTableProvider.ModBlockLootTables
                                                                ::new,
                                                        LootContextParamSets.BLOCK)),
                                        event.getLookupProvider()));
        var blockTagsProvider =
                generator.addProvider(
                        event.includeServer(),
                        (DataProvider.Factory<ModBlockTagsProvider>)
                                output ->
                                        new ModBlockTagsProvider(
                                                output,
                                                event.getLookupProvider(),
                                                existingFileHelper));
        generator.addProvider(
                event.includeServer(),
                (DataProvider.Factory<ModItemTagsProvider>)
                        output ->
                                new ModItemTagsProvider(
                                        output,
                                        event.getLookupProvider(),
                                        blockTagsProvider.contentsGetter(),
                                        API.MOD_ID,
                                        existingFileHelper));
        generator.addProvider(
                event.includeServer(),
                (DataProvider.Factory<ModRecipesProvider>)
                        output -> new ModRecipesProvider(output, event.getLookupProvider()));
        generator.addProvider(
                event.includeClient(),
                new ModBlockStateProvider(generator.getPackOutput(), existingFileHelper));
        generator.addProvider(
                event.includeClient(),
                new ModItemModelProvider(generator.getPackOutput(), existingFileHelper));
    }
    //?}
}
