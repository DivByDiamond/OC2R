package li.cil.oc2.common.bus.device.rpc.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RPCItemStackTagFilters {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<RPCItemStackTagFilter> FILTERS = new ArrayList<>();

    public static CompoundTag getFilteredTag(final ItemStack stack, final CompoundTag tag) {
        final CompoundTag result = new CompoundTag();
        for (final RPCItemStackTagFilter filter : FILTERS) {
            final CompoundTag filtered = filter.apply(stack, tag);
            if (filtered != null) {
                result.merge(filtered);
            }
        }

        return result;
    }

    /** Subscribes to the data pack reload event. */
    public static void register() {
        CommonEvents.ADD_RELOAD_LISTENER.register(adder -> adder.accept(ReloadListener.INSTANCE));
    }

    //? if >=26.1 {
    /*private static final class ReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
    *///?} else {
    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
    //?}
        private static final Gson GSON =
                new GsonBuilder()
                        //? if >=26.1 {
                        /*.registerTypeAdapter(
                                ResourceLocation.class,
                                (com.google.gson.JsonDeserializer<ResourceLocation>)
                                        (json, type, context) ->
                                                ResourceLocation.parse(json.getAsString()))
                        *///?} else {
                        .registerTypeAdapter(
                                ResourceLocation.class, new ResourceLocation.Serializer())
                        //?}
                        .create();

        public static final ReloadListener INSTANCE = new ReloadListener();

        public ReloadListener() {
            //? if >=26.1 {
            /*super(
                    net.minecraft.util.ExtraCodecs.JSON,
                    net.minecraft.resources.FileToIdConverter.json("item_tag_filters"));
            *///?} else {
            super(GSON, "item_tag_filters");
            //?}
        }

        @Override
        protected void apply(
                final Map<ResourceLocation, JsonElement> objects,
                final ResourceManager resourceManager,
                final ProfilerFiller profiler) {
            FILTERS.clear();

            objects.forEach(
                    (location, element) -> {
                        try {
                            final RPCItemStackTagFilter filter =
                                    GSON.fromJson(element, RPCItemStackTagFilter.class);
                            if (filter != null) {
                                FILTERS.add(filter);
                            }
                        } catch (final Exception e) {
                            LOGGER.error("Failed loading item tag filter [{}].", location, e);
                        }
                    });
        }
    }
}