package com.nanonaitor.arsenal.test;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.combat.*;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModItems;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.*;

@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScimitarBashGameTests {
    @GameTest(template="empty",templateNamespace="forge")
    public static void combinedBashAndCooldown(GameTestHelper h) {
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BashTest"));
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setYRot(0);p.setXRot(0);
        var main=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.WOOD).get());
        var off=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.DIAMOND).get());
        off.enchant(Enchantments.SHARPNESS,3);
        p.setItemSlot(EquipmentSlot.MAINHAND,main);p.setItemSlot(EquipmentSlot.OFFHAND,off);
        var target=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new net.minecraft.core.BlockPos(1,1,3));
        target.setNoAi(true);target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        float expected=(float)(ParityRules.attribute(p,main,Attributes.ATTACK_DAMAGE)
            +ParityRules.attribute(p,off,Attributes.ATTACK_DAMAGE))+2; // Sharpness III = +2.
        p.resetAttackStrengthTicker();p.startUsingItem(InteractionHand.MAIN_HAND);
        CombatEvents.handleControl(p,ModNetwork.SCIMITAR_BASH,true);
        h.assertTrue(Math.abs(target.getHealth()-(100-expected))<.01F,"Bash combines both stats and offhand enchantment at empty attack bar");
        h.assertTrue(main.getDamageValue()==1 && off.getDamageValue()==1,"Both blades wear once");
        h.assertTrue(ParityRules.disabled(p) && !p.isUsingItem(),"Bash ends guard and cools both blades");
        float health=target.getHealth();target.invulnerableTime=0;
        p.startUsingItem(InteractionHand.MAIN_HAND);
        CombatEvents.handleControl(p,ModNetwork.SCIMITAR_BASH,true);
        h.assertTrue(target.getHealth()==health,"Cooldown rejects repeated bash packets");
        p.stopUsingItem();p.getCooldowns().removeCooldown(main.getItem());p.getCooldowns().removeCooldown(off.getItem());
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.startUsingItem(InteractionHand.MAIN_HAND);
        CombatEvents.handleControl(p,ModNetwork.SCIMITAR_BASH,true);
        h.assertTrue(target.getHealth()==health,"Single scimitar cannot bash");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void compactTooltipLayout(GameTestHelper h) {
        java.util.List<net.minecraft.network.chat.Component> lines=new java.util.ArrayList<>();
        WeaponTooltipLayout.heading(lines,false,"Weakening, Dual Wield, Cross-Guard");
        h.assertTrue(lines.size()==2 && lines.get(0).getString().contains("SHIFT"),"Compact traits and expansion hint");
        lines.add(net.minecraft.network.chat.Component.literal("A long description of the weapon ability is wrapped into readable indented detail lines instead of stretching the tooltip across the screen."));
        WeaponTooltipLayout.details(lines,2);
        for(int i=2;i<lines.size();i++)h.assertTrue(lines.get(i).getString().startsWith("  ")
            && lines.get(i).getString().length()<=54,"Detail indentation and width");
        h.succeed();
    }
}
