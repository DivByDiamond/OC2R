package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class AxisAlignedLinearPosTest extends PosRuleTest {
    public static final MapCodec<AxisAlignedLinearPosTest> CODEC = RecordCodecBuilder.mapCodec(
        p_73977_ -> p_73977_.group(
                    Codec.FLOAT.fieldOf("min_chance").orElse(0.0F).forGetter(p_163719_ -> p_163719_.minChance),
                    Codec.FLOAT.fieldOf("max_chance").orElse(0.0F).forGetter(p_163717_ -> p_163717_.maxChance),
                    Codec.INT.fieldOf("min_dist").orElse(0).forGetter(p_163715_ -> p_163715_.minDist),
                    Codec.INT.fieldOf("max_dist").orElse(0).forGetter(p_163713_ -> p_163713_.maxDist),
                    Direction.Axis.CODEC.fieldOf("axis").orElse(Direction.Axis.Y).forGetter(p_163711_ -> p_163711_.axis)
                )
                .apply(p_73977_, AxisAlignedLinearPosTest::new)
    );
    private final float minChance;
    private final float maxChance;
    private final int minDist;
    private final int maxDist;
    private final Direction.Axis axis;

    public AxisAlignedLinearPosTest(float minChance, float maxChance, int minDist, int maxDist, Direction.Axis axis) {
        if (minDist >= maxDist) {
            throw new IllegalArgumentException("Invalid range: [" + minDist + "," + maxDist + "]");
        } else {
            this.minChance = minChance;
            this.maxChance = maxChance;
            this.minDist = minDist;
            this.maxDist = maxDist;
            this.axis = axis;
        }
    }

    @Override
    public boolean test(BlockPos localPos, BlockPos relativePos, BlockPos structurePos, RandomSource random) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, this.axis);
        float f = (float)Math.abs((relativePos.getX() - structurePos.getX()) * direction.getStepX());
        float f1 = (float)Math.abs((relativePos.getY() - structurePos.getY()) * direction.getStepY());
        float f2 = (float)Math.abs((relativePos.getZ() - structurePos.getZ()) * direction.getStepZ());
        int i = (int)(f + f1 + f2);
        float f3 = random.nextFloat();
        return f3 <= Mth.clampedLerp(this.minChance, this.maxChance, Mth.inverseLerp((float)i, (float)this.minDist, (float)this.maxDist));
    }

    @Override
    protected PosRuleTestType<?> getType() {
        return PosRuleTestType.AXIS_ALIGNED_LINEAR_POS_TEST;
    }
}
