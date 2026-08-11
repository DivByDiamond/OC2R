package li.cil.oc2.common.serialization.ceres;

import com.google.gson.JsonArray;
import li.cil.ceres.Ceres;
import li.cil.oc2.common.serialization.ceres.color.ColorDataSerializer;
import li.cil.oc2.common.serialization.ceres.color.ColorModeSerializer;
import li.cil.oc2.common.serialization.ceres.json.JsonArraySerializer;
import li.cil.oc2.common.serialization.ceres.memory.MemoryRangeListSerializer;
import li.cil.oc2.common.serialization.ceres.memory.MemoryRangeSerializer;
import li.cil.oc2.common.serialization.ceres.text.TextComponentSerializer;
import li.cil.oc2.common.vm.context.global.memory.MemoryRangeList;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.sedna.api.memory.MemoryRange;
import net.minecraft.network.chat.Component;

public final class Serializers {
    private static boolean isInitialized = false;

    static {
        initialize();
    }

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        isInitialized = true;

        Ceres.putSerializer(JsonArray.class, new JsonArraySerializer());
        Ceres.putSerializer(Component.class, new TextComponentSerializer());
        Ceres.putSerializer(MemoryRange.class, new MemoryRangeSerializer());
        Ceres.putSerializer(MemoryRangeList.class, new MemoryRangeListSerializer());
        Ceres.putSerializer(TerminalColors.ColorMode.class, new ColorModeSerializer());
        Ceres.putSerializer(TerminalColors.ColorData.class, new ColorDataSerializer());
    }
}