package li.cil.oc2.common.util.world;

import javax.annotation.Nullable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

@SuppressWarnings("LocalCanBeFinal")
public final class BlockEntityUtils {
    @SuppressWarnings("unchecked")
    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> haveType,
            BlockEntityType<E> wantType,
            BlockEntityTicker<? super E> ticker) {
        return wantType.equals(haveType) ? (BlockEntityTicker<A>) ticker : null;
    }
}