package com.nanonaitor.arsenal.test;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.combat.*;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParityGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ParityTest"));
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void offhandHitsDuringMainAbilities(GameTestHelper h) {
        for(var kind:new WeaponKind[]{WeaponKind.MORNING_STAR,WeaponKind.FLAIL}) {
            var p=player(h);
            var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
            p.setPos(pos.getX()+.5D,pos.getY(),pos.getZ()+.5D);p.setYRot(0);p.setXRot(0);
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.get(kind,WeaponTier.IRON).get()));
            var off=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.WOOD).get());
            p.setItemSlot(EquipmentSlot.OFFHAND,off);
            p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
            var target=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new net.minecraft.core.BlockPos(1,1,3));
            target.setNoAi(true);
            float health=target.getHealth();
            CombatEvents.handleControl(p,com.nanonaitor.arsenal.network.ModNetwork.SCIMITAR_ATTACK,true);
            h.assertTrue(target.getHealth()<health,kind+" permits actual offhand hit");
            h.assertTrue(off.getDamageValue()==1,"Only striking offhand wears");
            target.discard();
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void bulwarkStrainDisablesAndRecovers(GameTestHelper h) {
        var p=player(h);var stack=new ItemStack(ModItems.SUN_WAR.get());
        p.setItemSlot(EquipmentSlot.MAINHAND,stack);
        p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        var attacker=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new net.minecraft.core.BlockPos(1,1,1));
        for(int i=0;i<25;i++) h.assertTrue(CombatEvents.onLivingAttack(
            new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().mobAttack(attacker),2)),"Hit blocked");
        h.assertTrue(!p.isUsingItem() && p.getCooldowns().isOnCooldown(stack.getItem()),"25 hits disable guard");
        h.assertTrue(stack.getOrCreateTag().getInt("ArsenalGuardStrain")==0,"Strain resets on disable");
        stack.getOrCreateTag().putInt("ArsenalGuardStrain",5);p.tickCount=20;
        CombatEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(stack.getOrCreateTag().getInt("ArsenalGuardStrain")==4,"Strain decays once per second");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void independentHandImmunity(GameTestHelper h) {
        var p=player(h);var target=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new net.minecraft.core.BlockPos(1,1,1));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        var blade=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.IRON).get());
        p.setItemSlot(EquipmentSlot.MAINHAND,blade);
        var source=p.damageSources().playerAttack(p);
        h.assertTrue(target.hurt(source,4),"Main hit accepted");
        int mainTime=target.invulnerableTime;
        h.assertTrue(OffhandDamage.hurt(blade,target,target,source,4),"Offhand can hit during main immunity");
        h.assertTrue(target.invulnerableTime==mainTime,"Main immunity restored");
        h.assertTrue(!OffhandDamage.hurt(blade,target,target,source,4),"Repeated same-hand hit respects immunity");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void bulwarkBashIgnoresAttackCooldown(GameTestHelper h) {
        var p=player(h);var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        p.setPos(pos.getX()+.5D,pos.getY(),pos.getZ()+.5D);
        p.getAttribute(Attributes.ARMOR).setBaseValue(20);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.SUN_WAR.get()));
        p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);p.resetAttackStrengthTicker();
        var target=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new net.minecraft.core.BlockPos(1,1,3));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        CombatEvents.handleControl(p,com.nanonaitor.arsenal.network.ModNetwork.BULWARK_BASH,true);
        h.assertTrue(Math.abs(target.getHealth()-79)<.01F,"Empty-bar bash still deals 21 damage at 20 armor");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void staffSweepAndClawMiningRestrictions(GameTestHelper h) {
        var staff=new ItemStack(ModItems.get(WeaponKind.BLADE_STAFF,WeaponTier.IRON).get());
        h.assertTrue(!staff.canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP),"Staff cannot add vanilla sweep to its own splash");
        h.assertTrue(!staff.getItem().canApplyAtEnchantingTable(staff,net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE),"No Sweeping Edge application");
        var claw=new ItemStack(ModItems.get(WeaponKind.CLAWS,WeaponTier.IRON).get());
        h.assertTrue(!claw.getItem().canAttackBlock(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),h.getLevel(),h.absolutePos(net.minecraft.core.BlockPos.ZERO),player(h)),"Claws cannot mine");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void balanceAndWear(GameTestHelper h) {
        h.assertTrue(ParityRules.ballMultiplier(1)==1 && ParityRules.ballMultiplier(2)==1.5F
            && ParityRules.ballMultiplier(3)==2,"New throw multipliers");
        h.assertTrue(4+WeaponKind.BALL_AND_CHAIN.speedModifier==.5F,"Ball attack speed");
        h.assertTrue(ParityRules.blockWear(0)==1 && ParityRules.blockWear(5)==3
            && ParityRules.blockWear(6)==3,"Split block wear rounds up");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void disabledGuardAndSingleBlade(GameTestHelper h) {
        var p=player(h);
        var main=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.IRON).get());
        var off=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.DIAMOND).get());
        p.setItemSlot(EquipmentSlot.MAINHAND,main);p.setItemSlot(EquipmentSlot.OFFHAND,off);
        main.use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(ParityRules.guarding(p),"Pair guards");
        ParityRules.disable(p,30);
        main.use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!p.isUsingItem() && !ParityRules.guarding(p),"Disabled pair cannot re-guard");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        CombatEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(main.getUseAnimation()==net.minecraft.world.item.UseAnim.NONE,"Single blade no fake block");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void selectedHandAttributes(GameTestHelper h) {
        var p=player(h);
        var main=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.WOOD).get());
        var off=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.DIAMOND).get());
        p.setItemSlot(EquipmentSlot.MAINHAND,main);p.setItemSlot(EquipmentSlot.OFFHAND,off);
        var attr=p.getAttribute(Attributes.ATTACK_DAMAGE);
        attr.addTransientModifier(new AttributeModifier(java.util.UUID.randomUUID(),"External bonus",.5D,AttributeModifier.Operation.MULTIPLY_TOTAL));
        double before=attr.getValue();
        h.assertTrue(ParityRules.attribute(p,off,Attributes.ATTACK_DAMAGE)>ParityRules.attribute(p,main,Attributes.ATTACK_DAMAGE),"Diamond offhand owns its damage");
        h.assertTrue(before==attr.getValue(),"Attribute calculation never mutates player");
        double interval=ParityRules.interval(p,false);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED,200,1));
        h.assertTrue(ParityRules.interval(p,false)<interval,"Haste speeds paired cadence");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void staffRejectsEnvironmentalDamage(GameTestHelper h) {
        var p=player(h);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.get(WeaponKind.BLADE_STAFF,WeaponTier.IRON).get()));
        CombatEvents.handleControl(p,com.nanonaitor.arsenal.network.ModNetwork.BLADE_STAFF_REFLECT,true);
        h.assertTrue(!CombatEvents.onLivingAttack(new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().fall(),5)),"Fall is not reflected or blocked");
        var attacker=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new net.minecraft.core.BlockPos(1,1,1));
        p.invulnerableTime=10;
        float health=attacker.getHealth();
        h.assertTrue(CombatEvents.onLivingAttack(new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().mobAttack(attacker),5)),"Combat remains blocked during i-frames");
        h.assertTrue(attacker.getHealth()==health,"No reflection damage during defender i-frames");
        h.succeed();
    }
}
