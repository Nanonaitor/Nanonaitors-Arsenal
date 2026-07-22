package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.BallAndChainCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class BallAndChainSwingMessage implements IMessage {
    private boolean swinging;

    public BallAndChainSwingMessage() {}

    public BallAndChainSwingMessage(boolean swinging) {
        this.swinging = swinging;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        swinging = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(swinging);
    }

    public static final class Handler
        implements IMessageHandler<BallAndChainSwingMessage, IMessage> {
        @Override
        public IMessage onMessage(BallAndChainSwingMessage message,
                                  MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                BallAndChainCombat.updateSwinging(player, message.swinging));
            return null;
        }
    }
}
