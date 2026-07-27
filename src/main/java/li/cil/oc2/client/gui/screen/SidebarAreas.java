package li.cil.oc2.client.gui.screen;

import li.cil.oc2.client.gui.Sprites;

import net.minecraft.client.renderer.Rect2i;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
class SidebarAreas {
    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + Sprites.SIDEBAR_3.height + 4;

    static List<Rect2i> getExtraAreas(
            final int leftPos, final int topPos, final boolean renderEnergy) {
        final List<Rect2i> list = new ArrayList<>();
        list.add(
                new Rect2i(
                        leftPos - Sprites.SIDEBAR_3.width,
                        topPos + CONTROLS_TOP,
                        Sprites.SIDEBAR_3.width,
                        Sprites.SIDEBAR_3.height));
        if (renderEnergy) {
            list.add(
                    new Rect2i(
                            leftPos - Sprites.SIDEBAR_2.width,
                            topPos + ENERGY_TOP,
                            Sprites.SIDEBAR_2.width,
                            Sprites.SIDEBAR_2.height));
        }
        return list;
    }
}
