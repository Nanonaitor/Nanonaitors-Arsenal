package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.client.FlailAnimationHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class FlailAnimationMessage implements IMessage {
    private int entityId;

    public FlailAnimationMessage() {}

    public FlailAnimationMessage(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
    }

    public static final class Handler
        implements IMessageHandler<FlailAnimationMessage, IMessage> {
        @Override
        public IMessage onMessage(FlailAnimationMessage message,
                                  MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                FlailAnimationHandler.startRemoteSwing(message.entityId));
            return null;
        }
    }
}
