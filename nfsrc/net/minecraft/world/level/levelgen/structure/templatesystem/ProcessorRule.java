package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Passthrough;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;

public class ProcessorRule {
    public static final Passthrough DEFAULT_BLOCK_ENTITY_MODIFIER = Passthrough.INSTANCE;
    public static final Codec<ProcessorRule> CODEC = RecordCodecBuilder.create(
        p_338110_ -> p_338110_.group(
                    RuleTest.CODEC.fieldOf("input_predicate").forGetter(p_163747_ -> p_163747_.inputPredicate),
                    RuleTest.CODEC.fieldOf("location_predicate").forGetter(p_163745_ -> p_163745_.locPredicate),
                    PosRuleTest.CODEC.lenientOptionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter(p_163743_ -> p_163743_.posPredicate),
                    BlockState.CODEC.fieldOf("output_state").forGetter(p_163741_ -> p_163741_.outputState),
                    RuleBlockEntityModifier.CODEC
                        .lenientOptionalFieldOf("block_entity_modifier", DEFAULT_BLOCK_ENTITY_MODIFIER)
                        .forGetter(p_277333_ -> p_277333_.blockEntityModifier)
                )
                .apply(p_338110_, ProcessorRule::new)
    );
    private final RuleTest inputPredicate;
    private final RuleTest locPredicate;
    private final PosRuleTest posPredicate;
    private final BlockState outputState;
    private final RuleBlockEntityModifier blockEntityModifier;

    public ProcessorRule(RuleTest inputPredicate, RuleTest locPredicate, BlockState outputState) {
        this(inputPredicate, locPredicate, PosAlwaysTrueTest.INSTANCE, outputState);
    }

    public ProcessorRule(RuleTest inputPredicate, RuleTest locPredicate, PosRuleTest posPredicate, BlockState outputState) {
        this(inputPredicate, locPredicate, posPredicate, outputState, DEFAULT_BLOCK_ENTITY_MODIFIER);
    }

    public ProcessorRule(RuleTest inputPredicate, RuleTest locPredicate, PosRuleTest posPredicate, BlockState outputState, RuleBlockEntityModifier blockEntityModifier) {
        this.inputPredicate = inputPredicate;
        this.locPredicate = locPredicate;
        this.posPredicate = posPredicate;
        this.outputState = outputState;
        this.blockEntityModifier = blockEntityModifier;
    }

    /**
     * @param inputState    The incoming state from the structure.
     * @param existingState The current state in the world.
     * @param localPos      The local position of the target state, relative to the
     *                      structure origin.
     * @param relativePos   The actual position of the target state. {@code
     *                      existingState} is the current in world state at this
     *                      position.
     * @param structurePos  The origin position of the structure.
     */
    public boolean test(BlockState inputState, BlockState existingState, BlockPos localPos, BlockPos relativePos, BlockPos structurePos, RandomSource random) {
        return this.inputPredicate.test(inputState, random)
            && this.locPredicate.test(existingState, random)
            && this.posPredicate.test(localPos, relativePos, structurePos, random);
    }

    public BlockState getOutputState() {
        return this.outputState;
    }

    @Nullable
    public CompoundTag getOutputTag(RandomSource random, @Nullable CompoundTag tag) {
        return this.blockEntityModifier.apply(random, tag);
    }
}
