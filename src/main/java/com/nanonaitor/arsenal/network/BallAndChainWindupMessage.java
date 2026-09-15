package com.nanonaitor.arsenal.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Explicit attack heartbeat: active item use alone also means shielding. */
public final class BallAndChainWindupMessage implements IMessage {
    private int entityId;
    public BallAndChainWindupMessage() {}
    public BallAndChainWindupMessage(int entityId) {this.entityId=entityId;}
    @Override public void fromBytes(ByteBuf buf) {entityId=buf.readInt();}
    @Override public void toBytes(ByteBuf buf) {buf.writeInt(entityId);}
    public static final class Handler implements IMessageHandler<BallAndChainWindupMessage,IMessage> {
        @Override public IMessage onMessage(BallAndChainWindupMessage message,MessageContext context) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() ->
                com.nanonaitor.arsenal.client.BallAndChainAnimationHandler.receiveWindup(message.entityId));
            return null;
        }
    }
}
