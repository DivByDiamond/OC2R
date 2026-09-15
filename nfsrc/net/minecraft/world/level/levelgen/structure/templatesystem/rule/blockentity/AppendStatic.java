package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public class AppendStatic implements RuleBlockEntityModifier {
    public static final MapCodec<AppendStatic> CODEC = RecordCodecBuilder.mapCodec(
        p_277505_ -> p_277505_.group(CompoundTag.CODEC.fieldOf("data").forGetter(p_278105_ -> p_278105_.tag)).apply(p_277505_, AppendStatic::new)
    );
    private final CompoundTag tag;

    public AppendStatic(CompoundTag tag) {
        this.tag = tag;
    }

    @Override
    public CompoundTag apply(RandomSource random, @Nullable CompoundTag tag) {
        return tag == null ? this.tag.copy() : tag.merge(this.tag);
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.APPEND_STATIC;
    }
}
