package li.cil.oc2.common.util;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class Vec3Utils {
    public static Vec3i round(Vec3 vec) {
        return new Vec3i((int)Math.round(vec.x), (int)Math.round(vec.y), (int)Math.round(vec.z));
    }
}
