package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.FlailCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class FlailSwingMessage implements IMessage {
    public FlailSwingMessage() {}

    @Override
    public void fromBytes(ByteBuf buffer) {}

    @Override
    public void toBytes(ByteBuf buffer) {}

    public static final class Handler
        implements IMessageHandler<FlailSwingMessage, IMessage> {
        @Override
        public IMessage onMessage(FlailSwingMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> FlailCombat.tryServerSwing(player));
            return null;
        }
    }
}
