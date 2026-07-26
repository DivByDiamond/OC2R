package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.blockentity.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class InternetGatewayBlock extends HalfTransparentBlock implements EntityBlock {

    public InternetGatewayBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
    }

    @Override
    protected MapCodec<InternetGatewayBlock> codec() {
        return BlockCodecs.INTERNET_GATEWAY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntities.INTERNET_GATEWAY.get().create(pos, state);
    }

}
