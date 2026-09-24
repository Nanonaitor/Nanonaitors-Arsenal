package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.DoubleBladedScimitarCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class BladeStaffReflectionMessage implements IMessage {
    private int entityId,ticks;
    public BladeStaffReflectionMessage() {}
    public BladeStaffReflectionMessage(int entityId,int ticks){this.entityId=entityId;this.ticks=ticks;}
    @Override public void fromBytes(ByteBuf buffer){entityId=buffer.readInt();ticks=buffer.readInt();}
    @Override public void toBytes(ByteBuf buffer){buffer.writeInt(entityId);buffer.writeInt(ticks);}
    public static final class Handler implements IMessageHandler<BladeStaffReflectionMessage,IMessage> {
        @Override public IMessage onMessage(BladeStaffReflectionMessage message,MessageContext context){
            Minecraft.getMinecraft().addScheduledTask(()->{
                if(Minecraft.getMinecraft().world==null)return;
                Entity entity=Minecraft.getMinecraft().world.getEntityByID(message.entityId);
                if(entity instanceof EntityPlayer)DoubleBladedScimitarCombat.receiveReflection((EntityPlayer)entity,message.ticks);
            });
            return null;
        }
    }
}
