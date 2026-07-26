/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.logging.Logger;

public record RobotInitializationMessage(int entityId, CommonDeviceBusController.BusState busState, VMRunState runState, @Nullable Component bootError, CompoundTag terminal) implements AbstractMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, RobotInitializationMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        RobotInitializationMessage::entityId,
        NeoForgeStreamCodecs.enumCodec(CommonDeviceBusController.BusState.class),
        RobotInitializationMessage::busState,
        NeoForgeStreamCodecs.enumCodec(VMRunState.class),
        RobotInitializationMessage::runState,
        ComponentSerialization.STREAM_CODEC,
        RobotInitializationMessage::bootError,
        ByteBufCodecs.COMPOUND_TAG,
        RobotInitializationMessage::terminal,
        RobotInitializationMessage::new
    );

    public static final CustomPacketPayload.Type<RobotInitializationMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot_initialization_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public RobotInitializationMessage(final Robot robot) {
        this (
            robot.getId(),
            robot.getVirtualMachine().getBusState(),
            robot.getVirtualMachine().getRunState(),
            robot.getVirtualMachine().getBootError(),
            NBTSerialization.serialize(robot.getTerminal())
        );
    }

    ///////////////////////////////////////////////////////////////////

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientEntity(entityId, Robot.class,
            robot -> {
                robot.getVirtualMachine().setBusStateClient(busState);
                robot.getVirtualMachine().setRunStateClient(runState);
                robot.getVirtualMachine().setBootErrorClient(bootError);
                NBTSerialization.deserialize(terminal, robot.getTerminal());
            });
    }
}
