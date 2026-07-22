package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.BatteringRamCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class BatteringRamChargeMessage implements IMessage {
    public BatteringRamChargeMessage() {}

    @Override
    public void fromBytes(ByteBuf buffer) {}

    @Override
    public void toBytes(ByteBuf buffer) {}

    public static final class Handler
        implements IMessageHandler<BatteringRamChargeMessage, IMessage> {
        @Override
        public IMessage onMessage(BatteringRamChargeMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                BatteringRamCombat.markCharging(player));
            return null;
        }
    }
}
