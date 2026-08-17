package li.cil.oc2.common.block.misc;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.blockentity.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class SpeakerBlock extends Block implements EntityBlock {
    public SpeakerBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
    }

    @Override
    protected MapCodec<SpeakerBlock> codec() {
        return BlockCodecs.SPEAKER.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.SPEAKER.get().create(pos, state);
    }
}