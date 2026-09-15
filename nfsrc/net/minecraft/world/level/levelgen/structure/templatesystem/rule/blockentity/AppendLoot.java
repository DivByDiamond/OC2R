package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;

public class AppendLoot implements RuleBlockEntityModifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<AppendLoot> CODEC = RecordCodecBuilder.mapCodec(
        p_335310_ -> p_335310_.group(ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(p_335311_ -> p_335311_.lootTable))
                .apply(p_335310_, AppendLoot::new)
    );
    private final ResourceKey<LootTable> lootTable;

    public AppendLoot(ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public CompoundTag apply(RandomSource random, @Nullable CompoundTag tag) {
        CompoundTag compoundtag = tag == null ? new CompoundTag() : tag.copy();
        ResourceKey.codec(Registries.LOOT_TABLE)
            .encodeStart(NbtOps.INSTANCE, this.lootTable)
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_277353_ -> compoundtag.put("LootTable", p_277353_));
        compoundtag.putLong("LootTableSeed", random.nextLong());
        return compoundtag;
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.APPEND_LOOT;
    }
}
