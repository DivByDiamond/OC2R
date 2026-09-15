package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class RuleProcessor extends StructureProcessor {
    public static final MapCodec<RuleProcessor> CODEC = ProcessorRule.CODEC.listOf().fieldOf("rules").xmap(RuleProcessor::new, p_74306_ -> p_74306_.rules);
    private final ImmutableList<ProcessorRule> rules;

    public RuleProcessor(List<? extends ProcessorRule> rules) {
        this.rules = ImmutableList.copyOf(rules);
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
        LevelReader level,
        BlockPos offset,
        BlockPos pos,
        StructureTemplate.StructureBlockInfo blockInfo,
        StructureTemplate.StructureBlockInfo relativeBlockInfo,
        StructurePlaceSettings settings
    ) {
        RandomSource randomsource = RandomSource.create(Mth.getSeed(relativeBlockInfo.pos()));
        BlockState blockstate = level.getBlockState(relativeBlockInfo.pos());

        for (ProcessorRule processorrule : this.rules) {
            if (processorrule.test(relativeBlockInfo.state(), blockstate, blockInfo.pos(), relativeBlockInfo.pos(), pos, randomsource)) {
                return new StructureTemplate.StructureBlockInfo(
                    relativeBlockInfo.pos(), processorrule.getOutputState(), processorrule.getOutputTag(randomsource, relativeBlockInfo.nbt())
                );
            }
        }

        return relativeBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.RULE;
    }
}
