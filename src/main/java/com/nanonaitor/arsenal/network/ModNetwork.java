package com.nanonaitor.arsenal.network;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.combat.CombatEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
public final class ModNetwork {
 public static final byte FLAIL=1, BALL_CHAIN=2, RAM=3, BULWARK_BASH=4, CLAW=5, CLAW_MAIN=6, MORNING_STAR=7, BULWARK_ATTACK=8, BULWARK_MENU_GUARD=9, SCIMITAR_ATTACK=10, BALL_WIND_BOOST=11, TARTSY_BASH=12, BLADE_STAFF_ATTACK=13, BLADE_STAFF_REFLECT=14;
 public static final byte CANCEL_WEAPON_INPUTS=15;
 private static final String VERSION="2";
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ArsenalMod.MOD_ID,"controls"),()->VERSION,VERSION::equals,VERSION::equals);
 public static void init(){
  CHANNEL.messageBuilder(ControlPacket.class,0,NetworkDirection.PLAY_TO_SERVER)
   .encoder((m,b)->{b.writeByte(m.action);b.writeBoolean(m.active);})
   .decoder(b->new ControlPacket(b.readByte(),b.readBoolean()))
   .consumerMainThread((m,c)->{var player=c.get().getSender();if(player!=null && player.isAlive()) CombatEvents.handleControl(player,m.action,m.active);c.get().setPacketHandled(true);}).add();
 }
 public static void send(byte action,boolean active){CHANNEL.sendToServer(new ControlPacket(action,active));}
 private record ControlPacket(byte action,boolean active){}
 private ModNetwork(){}
}
