package li.cil.oc2.common.container;

import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class MonitorDisplayContainer extends AbstractMonitorContainer {
    public static void createServer(
            final MonitorBlockEntity monitor,
            final IEnergyStorage energy,
            final ServerPlayer player) {
        player.openMenu(
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(
                                monitor.getBlockState().getBlock().getDescriptionId());
                    }

                    @Override
                    public AbstractContainerMenu createMenu(
                            final int id, final Inventory inventory, final Player player) {
                        return new MonitorDisplayContainer(
                                id, player, monitor, createEnergyInfo(energy));
                    }
                },
                monitor.getBlockPos());
    }

    public static MonitorDisplayContainer createClient(
            final int id, final Inventory inventory, final FriendlyByteBuf data) {
        final BlockPos pos = data.readBlockPos();
        final BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof final MonitorBlockEntity monitor) {
            return new MonitorDisplayContainer(
                    id, inventory.player, monitor, createClientEnergyInfo());
        }

        throw new IllegalArgumentException();
    }

    private MonitorDisplayContainer(
            final int id,
            final Player player,
            final MonitorBlockEntity monitor,
            final IntPrecisionContainerData energyInfo) {
        super(Containers.MONITOR.get(), id, player, monitor, energyInfo);
    }
}