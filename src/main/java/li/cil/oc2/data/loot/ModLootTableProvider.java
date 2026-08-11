package li.cil.oc2.data.loot;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.components.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(
            PackOutput output,
            Set<ResourceKey<LootTable>> requiredTables,
            List<SubProviderEntry> subProviders,
            CompletableFuture<HolderLookup.Provider> registries) {
        super(output, requiredTables, subProviders, registries);
    }

    @Override
    public List<SubProviderEntry> getTables() {
        return singletonList(
                new LootTableProvider.SubProviderEntry(
                        ModBlockLootTables::new, LootContextParamSets.BLOCK));
    }

    public static final class ModBlockLootTables extends BlockLootSubProvider {
        public ModBlockLootTables(HolderLookup.Provider registries) {
            super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(Blocks.CHARGER.get());
            add(Blocks.COMPUTER.get(), this::droppingWithInventory);
            dropSelf(Blocks.DISK_DRIVE.get());
            dropSelf(Blocks.KEYBOARD.get());
            dropSelf(Blocks.NETWORK_CONNECTOR.get());
            dropSelf(Blocks.NETWORK_HUB.get());
            dropSelf(Blocks.PROJECTOR.get());
            dropSelf(Blocks.REDSTONE_INTERFACE.get());
            dropSelf(Blocks.MONITOR.get());
            dropSelf(Blocks.FLASH_MEMORY_FLASHER.get());
            dropSelf(Blocks.NETWORK_SWITCH.get());
            dropSelf(Blocks.VXLAN_HUB.get());
            dropSelf(Blocks.PCI_CARD_CAGE.get());
            dropSelf(Blocks.INTERNET_GATEWAY.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Blocks.REGISTRY.getEntries().stream()
                    .filter(blockRegObj -> !blockRegObj.get().equals(Blocks.BUS_CABLE.get()))
                    .map(DeferredHolder::get)
                    .collect(Collectors.toList());
        }

        private LootTable.Builder droppingWithInventory(final Block block) {
            return LootTable.lootTable()
                    .withPool(
                            applyExplosionCondition(
                                    block,
                                    LootPool.lootPool()
                                            .setRolls(ConstantValue.exactly(1))
                                            .add(
                                                    LootItem.lootTableItem(block)
                                                            .apply(
                                                                    CopyComponentsFunction
                                                                            .copyComponents(
                                                                                    CopyComponentsFunction
                                                                                            .Source
                                                                                            .BLOCK_ENTITY)
                                                                            .include(
                                                                                    DataComponents
                                                                                            .RESTRICTED_CONTAINER
                                                                                            .get())))));
        }

    }
}