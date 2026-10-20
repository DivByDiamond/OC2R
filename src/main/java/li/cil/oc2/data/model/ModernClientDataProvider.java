//? if >=26.1 {
/*package li.cil.oc2.data.model;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import li.cil.oc2.api.API;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public final class ModernClientDataProvider implements DataProvider {
    private final PackOutput output;

    public ModernClientDataProvider(final PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final Map<String, JsonObject> definitions = ModernModelDefinitions.create();
        BuiltInRegistries.ITEM.keySet().stream().filter(id -> API.MOD_ID.equals(id.getNamespace())).forEach(id -> {
            if (!definitions.containsKey("items/" + id.getPath() + ".json")) {
                throw new IllegalStateException("Missing generated item definition: " + id);
            }
        });
        BuiltInRegistries.BLOCK.keySet().stream().filter(id -> API.MOD_ID.equals(id.getNamespace())).forEach(id -> {
            if (!definitions.containsKey("blockstates/" + id.getPath() + ".json")) {
                throw new IllegalStateException("Missing generated blockstate: " + id);
            }
        });
        final var writes = new ArrayList<CompletableFuture<?>>();
        definitions.forEach((path, json) -> writes.add(DataProvider.saveStable(cache, json,
                output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(API.MOD_ID).resolve(path))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "OC2R 26.x blockstates, models and item definitions";
    }
}
*///?}
