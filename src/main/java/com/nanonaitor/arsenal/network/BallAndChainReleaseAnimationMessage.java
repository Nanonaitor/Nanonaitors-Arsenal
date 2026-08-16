package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.client.BallAndChainAnimationHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class BallAndChainReleaseAnimationMessage implements IMessage {
    private int entityId;
    private int charge;
    private float distance;
    private float yaw;
    private float pitch;
    private int durationTicks;

    public BallAndChainReleaseAnimationMessage() {}

    public BallAndChainReleaseAnimationMessage(int entityId, int charge, float distance,
                                               float yaw, float pitch, int durationTicks) {
        this.entityId = entityId;
        this.charge = charge;
        this.distance = distance;
        this.yaw = yaw;
        this.pitch = pitch;
        this.durationTicks = durationTicks;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        charge = buffer.readUnsignedByte();
        distance = buffer.readFloat();
        yaw = buffer.readFloat();
        pitch = buffer.readFloat();
        durationTicks = buffer.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeByte(charge);
        buffer.writeFloat(distance);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeByte(durationTicks);
    }

    public static final class Handler
        implements IMessageHandler<BallAndChainReleaseAnimationMessage, IMessage> {
        @Override
        public IMessage onMessage(BallAndChainReleaseAnimationMessage message,
                                  MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                BallAndChainAnimationHandler.startReleaseAnimation(
                    message.entityId, message.charge, message.distance,
                    message.yaw, message.pitch, message.durationTicks));
            return null;
        }
    }
}
