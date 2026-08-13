package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.ShieldCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class BulwarkBashMessage implements IMessage {
    public BulwarkBashMessage() {}
    @Override public void fromBytes(ByteBuf buffer) {}
    @Override public void toBytes(ByteBuf buffer) {}

    public static final class Handler implements IMessageHandler<BulwarkBashMessage, IMessage> {
        @Override public IMessage onMessage(BulwarkBashMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> ShieldCombat.bash(player));
            return null;
        }
    }
}
