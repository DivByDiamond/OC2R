package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RandomBlockMatchTest extends RuleTest {
    public static final MapCodec<RandomBlockMatchTest> CODEC = RecordCodecBuilder.mapCodec(
        p_344666_ -> p_344666_.group(
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(p_163766_ -> p_163766_.block),
                    Codec.FLOAT.fieldOf("probability").forGetter(p_163764_ -> p_163764_.probability)
                )
                .apply(p_344666_, RandomBlockMatchTest::new)
    );
    private final Block block;
    private final float probability;

    public RandomBlockMatchTest(Block block, float probability) {
        this.block = block;
        this.probability = probability;
    }

    @Override
    public boolean test(BlockState state, RandomSource random) {
        return state.is(this.block) && random.nextFloat() < this.probability;
    }

    @Override
    protected RuleTestType<?> getType() {
        return RuleTestType.RANDOM_BLOCK_TEST;
    }
}
