package com.nanonaitor.arsenal.test;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.compat.LevelRequirements;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LevelRequirementGameTests {
    @GameTest(template="empty",templateNamespace="forge")
    public static void requirementTooltipColors(GameTestHelper h) {
        for(int current:new int[]{-1,0,1,2}) {
            java.util.List<net.minecraft.network.chat.Component> lines=new java.util.ArrayList<>();
            com.nanonaitor.arsenal.compat.RequirementTooltip.append(lines,1,current);
            h.assertTrue(lines.size()==3 && lines.get(0).getString().isEmpty(),"Requirements separated from previous stats");
            h.assertTrue(lines.get(1).getStyle().getColor().getValue()==net.minecraft.ChatFormatting.DARK_PURPLE.getColor(),"Purple heading");
            h.assertTrue(lines.get(2).getString().equals(" - Strength: 1"),"Skill label and configured level");
            var number=lines.get(2).getSiblings().get(0);
            h.assertTrue(number.getStyle().getColor().getValue()==(current>=1
                ?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED).getColor(),"Live level threshold color");
        }
        java.util.List<net.minecraft.network.chat.Component> disabled=new java.util.ArrayList<>();
        com.nanonaitor.arsenal.compat.RequirementTooltip.append(disabled,0,0);
        h.assertTrue(disabled.isEmpty(),"No requirement section for unlocked tier");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge")
    public static void detectionAndTierBoundaries(GameTestHelper h) {
        h.assertTrue(!LevelRequirements.enabledFor(false,true),"Absent mod means no skill dependency");
        h.assertTrue(!LevelRequirements.enabledFor(true,false),"Config disables integration");
        h.assertTrue(LevelRequirements.enabledFor(true,true),"Installed and enabled means active");
        for(var tier:WeaponTier.values()) {
            int level=LevelRequirements.defaultLevel(tier);
            h.assertTrue(!LevelRequirements.meets(level-1,level),"Below level blocked: "+tier);
            h.assertTrue(LevelRequirements.meets(level,level),"Exact level accepted: "+tier);
            for(var kind:WeaponKind.values()) {
                var stack=new net.minecraft.world.item.ItemStack(ModItems.get(kind,tier).get());
                h.assertTrue(LevelRequirements.required(stack)==level,"Shared tier requirement: "+kind+"/"+tier);
            }
        }
        h.assertTrue(LevelRequirements.meets(-1,0),"Zero disables individual tier");
        h.succeed();
    }
}
