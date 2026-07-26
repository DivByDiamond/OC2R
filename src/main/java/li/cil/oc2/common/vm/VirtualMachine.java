/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.common.bus.controller.BusState;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public interface VirtualMachine {
    BusState getBusState();

    @OnlyIn(Dist.CLIENT)
    void setBusStateClient(BusState value);

    VMRunState getRunState();

    @OnlyIn(Dist.CLIENT)
    void setRunStateClient(VMRunState value);

    @Nullable
    Component getBootError();

    @OnlyIn(Dist.CLIENT)
    void setBootErrorClient(@Nullable Component value);

    @Nullable
    Component getError();

    boolean isRunning();

    void start();

    void stop();
}
