package li.cil.oc2.client.gui.screen.widget;

import javax.annotation.Nullable;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.client.gui.widget.misc.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.base.AbstractMachineContainer;
import li.cil.oc2.common.vm.PowerAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if <26.1 {
import net.minecraft.client.gui.screens.Screen;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PowerButton extends ToggleImageButton {
    private final AbstractMachineContainer menu;

    public PowerButton(final int x, final int y, final AbstractMachineContainer menu) {
        super(
                x,
                y,
                12,
                12,
                Sprites.POWER_BUTTON_BASE,
                Sprites.POWER_BUTTON_PRESSED,
                Sprites.POWER_BUTTON_ACTIVE);
        this.menu = menu;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {}

    @Override
    public void onPress() {
        super.onPress();
        menu.sendPowerActionToServer(getAction());
    }

    @Override
    public boolean isToggled() {
        return menu.getVirtualMachine().isRunning();
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        final PowerAction action = getAction();
        if (action == PowerAction.START) {
            return Tooltip.create(Component.translatable(Constants.COMPUTER_SCREEN_POWER_ON));
        }

        final MutableComponent text = Component.empty();
        text.append(line(Constants.COMPUTER_SCREEN_POWER_SHUTDOWN, action == PowerAction.SHUTDOWN))
                .append("\n")
                .append(line(Constants.COMPUTER_SCREEN_POWER_REBOOT, action == PowerAction.REBOOT))
                .append("\n")
                .append(
                        line(
                                Constants.COMPUTER_SCREEN_POWER_POWER_OFF,
                                action == PowerAction.POWER_OFF));
        return Tooltip.create(text);
    }

    private PowerAction getAction() {
        if (!isToggled()) {
            return PowerAction.START;
        }
        if (isControlDown()) {
            return PowerAction.POWER_OFF;
        }
        if (isShiftDown()) {
            return PowerAction.REBOOT;
        }
        return PowerAction.SHUTDOWN;
    }

    private static boolean isControlDown() {
        //? if >=26.1 {
        /*return Minecraft.getInstance().hasControlDown();
        *///?} else {
        return Screen.hasControlDown();
        //?}
    }

    private static boolean isShiftDown() {
        //? if >=26.1 {
        /*return Minecraft.getInstance().hasShiftDown();
        *///?} else {
        return Screen.hasShiftDown();
        //?}
    }

    private static Component line(final String key, final boolean highlighted) {
        return Component.translatable(key)
                .withStyle(highlighted ? ChatFormatting.WHITE : ChatFormatting.GRAY);
    }
}
