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
            h.assertTrue(lines.get(2).getString().equals(" - "+LevelRequirements.aptitudeName()+": 1"),"Skill label and configured level");
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
        for(var tier:WeaponTier.activeValues()) {
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
    /**
     * Reads a real player's level from Talents (a renamed JustLevelingFork whose melee
     * aptitude is "attack"). Only meaningful in a -PdevMods=talents run; passes trivially otherwise.
     */
    @GameTest(template="empty",templateNamespace="forge")
    public static void talentsAttackLevelIsRead(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("talents")){h.succeed();return;}
        // Talents gives no levels to fake players, so this needs a real (unconnected) player.
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TalentsLevel"));
        var fake=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TalentsFake"));
        h.assertTrue(LevelRequirements.active(),"Talents activates the requirements");
        h.assertTrue("Attack".equals(LevelRequirements.aptitudeName()),"Talents uses its Attack aptitude");
        int level=LevelRequirements.level(player);
        h.assertTrue(level>=1,"Attack level read from Talents, got "+level);
        var iron=new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.IRON).get());
        h.assertTrue(level>=8 || !LevelRequirements.canUse(player,iron,false),"Iron needs Attack 8");
        var wood=new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.WOOD).get());
        h.assertTrue(LevelRequirements.canUse(player,wood,false),"Wood needs Attack 1");
        h.assertTrue(LevelRequirements.canUse(fake,iron,false),"Fake players (machines) have no levels and are not locked");
        h.succeed();
    }
}
