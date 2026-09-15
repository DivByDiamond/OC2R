package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ServerLevelAccessor;

public class CappedProcessor extends StructureProcessor {
    public static final MapCodec<CappedProcessor> CODEC = RecordCodecBuilder.mapCodec(
        p_277598_ -> p_277598_.group(
                    StructureProcessorType.SINGLE_CODEC.fieldOf("delegate").forGetter(p_277456_ -> p_277456_.delegate),
                    IntProvider.POSITIVE_CODEC.fieldOf("limit").forGetter(p_277680_ -> p_277680_.limit)
                )
                .apply(p_277598_, CappedProcessor::new)
    );
    private final StructureProcessor delegate;
    private final IntProvider limit;

    public CappedProcessor(StructureProcessor delegate, IntProvider limit) {
        this.delegate = delegate;
        this.limit = limit;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.CAPPED;
    }

    @Override
    public final List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
        ServerLevelAccessor serverLevel,
        BlockPos offset,
        BlockPos pos,
        List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
        List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
        StructurePlaceSettings settings
    ) {
        if (this.limit.getMaxValue() != 0 && !processedBlockInfos.isEmpty()) {
            if (originalBlockInfos.size() != processedBlockInfos.size()) {
                Util.logAndPauseIfInIde(
                    "Original block info list not in sync with processed list, skipping processing. Original size: "
                        + originalBlockInfos.size()
                        + ", Processed size: "
                        + processedBlockInfos.size()
                );
                return processedBlockInfos;
            } else {
                RandomSource randomsource = RandomSource.create(serverLevel.getLevel().getSeed()).forkPositional().at(offset);
                int i = Math.min(this.limit.sample(randomsource), processedBlockInfos.size());
                if (i < 1) {
                    return processedBlockInfos;
                } else {
                    IntArrayList intarraylist = Util.toShuffledList(IntStream.range(0, processedBlockInfos.size()), randomsource);
                    IntIterator intiterator = intarraylist.intIterator();
                    int j = 0;

                    while (intiterator.hasNext() && j < i) {
                        int k = intiterator.nextInt();
                        StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = originalBlockInfos.get(k);
                        StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo1 = processedBlockInfos.get(k);
                        StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo2 = this.delegate
                            .processBlock(
                                serverLevel, offset, pos, structuretemplate$structureblockinfo, structuretemplate$structureblockinfo1, settings
                            );
                        if (structuretemplate$structureblockinfo2 != null
                            && !structuretemplate$structureblockinfo1.equals(structuretemplate$structureblockinfo2)) {
                            j++;
                            processedBlockInfos.set(k, structuretemplate$structureblockinfo2);
                        }
                    }

                    return processedBlockInfos;
                }
            }
        } else {
            return processedBlockInfos;
        }
    }
}
