package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.ShieldCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class TartsyBashMessage implements IMessage {
    public TartsyBashMessage() {}
    @Override public void fromBytes(ByteBuf buffer) {}
    @Override public void toBytes(ByteBuf buffer) {}

    public static final class Handler implements IMessageHandler<TartsyBashMessage, IMessage> {
        @Override public IMessage onMessage(TartsyBashMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> ShieldCombat.tartsyBash(player));
            return null;
        }
    }
}
