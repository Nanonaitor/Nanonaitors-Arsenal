package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.combat.ModernBackportCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Compact controls shared by the modern-mechanics 1.12.2 backport. */
public final class ModernWeaponControlMessage implements IMessage {
    public static final byte MORNING_STAR = 0;
    public static final byte SCIMITAR_ATTACK = 1;
    public static final byte BULWARK_MENU_GUARD = 2;
    public static final byte BALL_WIND_BOOST = 3;
    public static final byte BULWARK_ATTACK = 4;
    public static final byte MORNING_CANCEL = 5;
    public static final byte SCIMITAR_BASH = 6;
    public static final byte SHIELD_TAKEOVER = 7;
    private byte action;
    private boolean active;

    public ModernWeaponControlMessage() {}
    public ModernWeaponControlMessage(byte action, boolean active) {
        this.action = action;
        this.active = active;
    }
    @Override public void fromBytes(ByteBuf buf) { action = buf.readByte(); active = buf.readBoolean(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeByte(action); buf.writeBoolean(active); }

    public static final class Handler implements IMessageHandler<ModernWeaponControlMessage, IMessage> {
        @Override public IMessage onMessage(ModernWeaponControlMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                ModernBackportCombat.handleControl(player, message.action, message.active));
            return null;
        }
    }
}
