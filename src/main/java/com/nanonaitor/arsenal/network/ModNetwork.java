package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.combat.CombatEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    public static final byte FLAIL = 1, BALL_CHAIN = 2, RAM = 3, BULWARK_BASH = 4, CLAW = 5;
    private static SimpleChannel channel;

    public static void init() {
        channel = ChannelBuilder.named(Identifier.fromNamespaceAndPath(ArsenalMod.MOD_ID, "controls"))
            .networkProtocolVersion(1).simpleChannel();
        channel.messageBuilder(ControlPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> { buffer.writeByte(message.action); buffer.writeBoolean(message.active); })
            .decoder(buffer -> new ControlPacket(buffer.readByte(), buffer.readBoolean()))
            .consumerMainThread((message, context) -> {
                if (context.getSender() != null) CombatEvents.handleControl(context.getSender(), message.action, message.active);
            }).add();
        channel.build();
    }

    public static void send(byte action, boolean active) {
        channel.send(new ControlPacket(action, active), PacketDistributor.SERVER.noArg());
    }

    private record ControlPacket(byte action, boolean active) {}
    private ModNetwork() {}
}
