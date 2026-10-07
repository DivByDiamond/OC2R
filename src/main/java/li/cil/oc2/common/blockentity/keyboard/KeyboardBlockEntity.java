package li.cil.oc2.common.blockentity.keyboard;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.CapabilityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class KeyboardBlockEntity extends ModBlockEntity {
    private final KeyboardDevice<BlockEntity> keyboardDevice = new KeyboardDevice<>(this);

    public KeyboardBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.KEYBOARD.get(), pos, state);
    }

    public void handleInput(final int keycode, final boolean isDown) {
        keyboardDevice.sendKeyEvent(keycode, isDown);
    }

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> {
                    if (side == Direction.DOWN && be instanceof final KeyboardBlockEntity self) {
                        return self.keyboardDevice;
                    }
                    return null;
                },
                Blocks.KEYBOARD.get());
    }
}