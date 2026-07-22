package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.ClawOffhandAttackHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class OffhandClawAttackMessage implements IMessage {
    private int targetId;

    public OffhandClawAttackMessage() {}

    public OffhandClawAttackMessage(int targetId) {
        this.targetId = targetId;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        targetId = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(targetId);
    }

    public static final class Handler
        implements IMessageHandler<OffhandClawAttackMessage, IMessage> {
        @Override
        public IMessage onMessage(OffhandClawAttackMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity target = player.world.getEntityByID(message.targetId);
                if (target instanceof EntityLivingBase && player.getDistanceSq(target) <= 36.0D) {
                    ClawOffhandAttackHandler.tryServerAttack(player, (EntityLivingBase) target);
                }
            });
            return null;
        }
    }
}
