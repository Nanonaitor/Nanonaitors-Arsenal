package com.nanonaitor.arsenal.test;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.combat.*;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecentParityGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RecentParity"));
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void sharedClawWindow(GameTestHelper h) {
        var limit=new PairedClawHitLimiter<Object,Object>();
        Object p=new Object(),target=new Object();limit.confirmed(p,target,10);
        for(int i=10;i<14;i++)h.assertTrue(!limit.ready(p,target,i),"Both hands wait four ticks");
        h.assertTrue(limit.ready(p,target,14),"Exact boundary allowed");
        h.assertTrue(limit.ready(new Object(),target,10),"Other player independent");
        h.assertTrue(limit.ready(p,new Object(),10),"Other target independent");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=60)
    public static void noFourthClawCritical(GameTestHelper h) {
        var p=player(h);var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setYRot(0);p.setXRot(0);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.get(WeaponKind.CLAWS,WeaponTier.IRON).get()));
        p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ModItems.get(WeaponKind.LINKED_CLAWS,WeaponTier.IRON).get()));
        p.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1000);
        var target=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new net.minecraft.core.BlockPos(1,1,3));
        target.setNoAi(true);target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);
        final float[] first={-1};
        for(int hit=0;hit<4;hit++){final int index=hit;h.runAtTickTime(2+hit*10,()->{
            target.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2.5);target.invulnerableTime=0;
            float before=target.getHealth();CombatEvents.handleControl(p,ModNetwork.CLAW,true);
            float damage=before-target.getHealth();h.assertTrue(damage>0,"Claw hit registers");
            if(index==0)first[0]=damage;
            h.assertTrue(Math.abs(damage-first[0])<.001,"No fourth-hit bonus");
            float after=target.getHealth();int wear=p.getMainHandItem().getDamageValue();
            CombatEvents.handleControl(p,ModNetwork.CLAW,true);
            h.assertTrue(target.getHealth()==after && p.getMainHandItem().getDamageValue()==wear,"Fast duplicate does not hit or wear equipment");
            h.assertTrue(CombatEvents.onAttackEntity(new net.minecraftforge.event.entity.player.AttackEntityEvent(p,target)),"Main hand shares offhand's four-tick window");
            if(index==3)h.succeed();
        });}
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void shieldSurvivesStaffCleanup(GameTestHelper h) {
        var p=player(h);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.get(WeaponKind.BLADE_STAFF,WeaponTier.IRON).get()));
        CombatEvents.handleControl(p,ModNetwork.BLADE_STAFF_REFLECT,true);
        p.stopUsingItem();p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(net.minecraft.world.item.Items.SHIELD));
        p.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        p.getPersistentData().putInt("ArsenalClawCritChain",3);
        CombatEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(p.isUsingItem() && p.getUsedItemHand()==net.minecraft.world.InteractionHand.OFF_HAND,"Staff cleanup preserves active shield");
        h.assertTrue(!p.getPersistentData().contains("ArsenalClawCritChain"),"Old banked critical removed");
        h.succeed();
    }
}
