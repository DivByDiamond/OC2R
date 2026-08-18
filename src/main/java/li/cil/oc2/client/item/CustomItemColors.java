package li.cil.oc2.client.item;

import static net.minecraft.core.component.DataComponents.DYED_COLOR;

import java.util.Map;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@SuppressWarnings("unused")
public final class CustomItemColors {
    public static final int BLACK = 0xFF404040;
    public static final int GREY = 0xFF555555;
    public static final int LIGHT_GREY = 0xFF898989;
    public static final int WHITE = 0xFFCACACA;

    public static final int LIME = 0xFF65DA2B;
    public static final int GREEN = 0xFF1C9C31;
    public static final int CYAN = 0xFF11C5BD;
    public static final int BLUE = 0xFF4F66E8;
    public static final int LIGHT_BLUE = 0xFF1192C5;
    public static final int PURPLE = 0xFF8F02CA;
    public static final int MAGENTA = 0xFFC61087;
    public static final int PINK = 0xFFDB51BD;
    public static final int ORANGE = 0xFFDD803D;
    public static final int RED = 0xFFDD3D3D;
    public static final int BROWN = 0xFF745C42;
    public static final int YELLOW = 0xFFFFFC49;

    private static final int NO_TINT = 0xFFFFFFFF;

    private static final Map<DyeColor, Integer> COLOR_BY_DYE =
            Map.ofEntries(
                    Map.entry(DyeColor.WHITE, WHITE),
                    Map.entry(DyeColor.ORANGE, ORANGE),
                    Map.entry(DyeColor.MAGENTA, MAGENTA),
                    Map.entry(DyeColor.LIGHT_BLUE, LIGHT_BLUE),
                    Map.entry(DyeColor.YELLOW, YELLOW),
                    Map.entry(DyeColor.LIME, LIME),
                    Map.entry(DyeColor.PINK, PINK),
                    Map.entry(DyeColor.GRAY, GREY),
                    Map.entry(DyeColor.LIGHT_GRAY, LIGHT_GREY),
                    Map.entry(DyeColor.CYAN, CYAN),
                    Map.entry(DyeColor.PURPLE, PURPLE),
                    Map.entry(DyeColor.BLUE, BLUE),
                    Map.entry(DyeColor.BROWN, BROWN),
                    Map.entry(DyeColor.GREEN, GREEN),
                    Map.entry(DyeColor.RED, RED),
                    Map.entry(DyeColor.BLACK, BLACK));

    public static void initialize(final RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, layer) -> layer == 1 ? getColor(stack) : NO_TINT,
                Items.HARD_DRIVE_SMALL.get(),
                Items.HARD_DRIVE_MEDIUM.get(),
                Items.HARD_DRIVE_LARGE.get(),
                Items.HARD_DRIVE_EXTRA_LARGE.get(),
                Items.HARD_DRIVE_ONYXOS.get(),
                Items.FLOPPY.get(),
                Items.FLOPPY_MODERN.get());
    }

    public static int getColorByDye(final DyeColor dye) {
        final Integer color = COLOR_BY_DYE.get(dye);
        if (color == null) {
            throw new AssertionError(dye);
        }
        return color;
    }

    public static int getColor(final ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, GREY);
    }

    public static ItemStack withColor(final ItemStack stack, final DyeColor color) {
        return withColor(stack, getColorByDye(color));
    }

    public static ItemStack withColor(final ItemStack stack, final int color) {
        stack.applyComponents(
                DataComponentPatch.builder()
                        .set(DYED_COLOR, new DyedItemColor(color, true))
                        .build());
        return stack;
    }
}