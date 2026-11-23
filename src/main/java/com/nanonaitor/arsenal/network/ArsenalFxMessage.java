package com.nanonaitor.arsenal.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: a short visual effect (screen shake, armour-break icon). */
public final class ArsenalFxMessage implements IMessage {
    /** Shake the receiving player's camera; value = strength in hundredths. */
    public static final byte SHAKE = 0;
    /** Show the armour-break icon above an entity; value = ticks. */
    public static final byte ARMOR_BREAK = 1;
    /** The receiving player's ram crashed into a wall: shake (value as SHAKE) and hold the charge. */
    public static final byte RAM_CRASH = 2;

    private byte type;
    private int entityId, value;

    public ArsenalFxMessage() {}

    public ArsenalFxMessage(byte type, int entityId, int value) {
        this.type = type;
        this.entityId = entityId;
        this.value = value;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        type = buffer.readByte();
        entityId = buffer.readInt();
        value = buffer.readInt();
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeByte(type);
        buffer.writeInt(entityId);
        buffer.writeInt(value);
    }

    public static final class Handler implements IMessageHandler<ArsenalFxMessage, IMessage> {
        @Override public IMessage onMessage(ArsenalFxMessage message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                com.nanonaitor.arsenal.client.ArsenalClientFx.receive(message.type, message.entityId, message.value));
            return null;
        }
    }
}
