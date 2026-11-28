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
 public static final byte SCIMITAR_BASH=16;
 private static final String VERSION="6";
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ArsenalMod.MOD_ID,"controls"),()->VERSION,VERSION::equals,VERSION::equals);
 public static void init(){
  CHANNEL.messageBuilder(ControlPacket.class,0,NetworkDirection.PLAY_TO_SERVER)
   .encoder((m,b)->{b.writeByte(m.action);b.writeBoolean(m.active);})
   .decoder(b->new ControlPacket(b.readByte(),b.readBoolean()))
   .consumerMainThread((m,c)->{var player=c.get().getSender();if(player!=null && player.isAlive()) CombatEvents.handleControl(player,m.action,m.active);c.get().setPacketHandled(true);}).add();
  CHANNEL.messageBuilder(FxPacket.class,1,NetworkDirection.PLAY_TO_CLIENT)
   .encoder((m,b)->{b.writeByte(m.type);b.writeVarInt(m.entityId);b.writeVarInt(m.ticks);})
   .decoder(b->new FxPacket(b.readByte(),b.readVarInt(),b.readVarInt()))
   .consumerMainThread((m,c)->{
     net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
       ()->()->com.nanonaitor.arsenal.client.ArsenalClientFx.receive(m.type,m.entityId,m.ticks));
     c.get().setPacketHandled(true);}).add();
  CHANNEL.messageBuilder(ConfigPacket.class,2,NetworkDirection.PLAY_TO_CLIENT)
   .encoder((m,b)->b.writeMap(m.values,(buf,k)->buf.writeUtf(k),(buf,v)->buf.writeDouble(v)))
   .decoder(b->new ConfigPacket(b.readMap(buf->buf.readUtf(),buf->buf.readDouble())))
   .consumerMainThread((m,c)->{com.nanonaitor.arsenal.config.SyncedConfig.receive(m.values);c.get().setPacketHandled(true);}).add();
 }
 public static final byte FX_ARMOR_BREAK=1, FX_SHAKE=2, FX_RAM_CRASH=3;
 /** An effect for one player only (camera shake, ram crash). */
 public static void fx(net.minecraft.server.level.ServerPlayer player,byte type,int entityId,int value){
  CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),new FxPacket(type,entityId,value));
 }
 /** Sends the server's combat settings to one player (on join) or to everyone (after a reload). */
 public static void syncConfig(net.minecraft.server.level.ServerPlayer player){
  var packet=new ConfigPacket(com.nanonaitor.arsenal.config.SyncedConfig.snapshot());
  if(player!=null)CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),packet);
  else CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),packet);
 }
 private record ConfigPacket(java.util.Map<String,Double> values){}
 /** Shows the Armor Fracture icon over this entity for a moment, to everyone tracking it. */
 public static void armorBreakIcon(net.minecraft.world.entity.Entity target,int ticks){
  CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->target),
   new FxPacket(FX_ARMOR_BREAK,target.getId(),ticks));
 }
 private record FxPacket(byte type,int entityId,int ticks){}
 public static void send(byte action,boolean active){CHANNEL.sendToServer(new ControlPacket(action,active));}
 private record ControlPacket(byte action,boolean active){}
 private ModNetwork(){}
}
