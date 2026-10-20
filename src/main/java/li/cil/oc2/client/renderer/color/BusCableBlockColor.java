package li.cil.oc2.client.renderer.color;

//? if >=26.1 {
/*import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
//?}

//? if <26.1 {
import net.neoforged.api.distmarker.OnlyIn;
//?}

// Tints the bus cable with the tint of its facade block.
@OnlyIn(Dist.CLIENT)
public final class BusCableBlockColor implements BlockTintSource {
    @Override
    public int color(final BlockState state) {
        return 0;
    }

    @Override
    public int colorInWorld(
            final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCable)) return 0;
        final BlockState facade = ItemStackUtils.getBlockState(busCable.getFacade());
        if (facade == null) return 0;
        final BlockTintSource facadeTint = Minecraft.getInstance().getBlockColors().getTintSource(facade, 0);
        if (facadeTint != null) return facadeTint.colorInWorld(facade, level, pos);

        return 0;
    }
}
*///?} else {
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

@OnlyIn(Dist.CLIENT)
public final class BusCableBlockColor implements BlockColor {
    @Override
    public int getColor(
            final BlockState state,
            @Nullable final BlockAndTintGetter level,
            @Nullable final BlockPos pos,
            final int tintIndex) {
        if (level == null || pos == null) {
            return 0;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final BusCableBlockEntity busCable) {
            final BlockState facade = ItemStackUtils.getBlockState(busCable.getFacade());
            if (facade != null) {
                return Minecraft.getInstance()
                        .getBlockColors()
                        .getColor(facade, level, pos, tintIndex);
            }
        }

        return 0;
    }
}
//?}
