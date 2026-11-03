package li.cil.oc2.common.block.machine;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.machine.MachineInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/** Front connects to a machine, the remaining faces accept the device bus and automation. */
public final class MachineInterfaceBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public MachineInterfaceBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
                .strength(2.5f, 6.0f).pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<MachineInterfaceBlock> codec() {
        return BlockCodecs.MACHINE_INTERFACE.get();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction face = context.getClickedFace();
        return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal()
                ? face.getOpposite() : context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                                final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineInterfaceBlockEntity machine) {
            machine.inventory.recover();
            player.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.MACHINE_INTERFACE.get().create(pos, state);
    }
}
