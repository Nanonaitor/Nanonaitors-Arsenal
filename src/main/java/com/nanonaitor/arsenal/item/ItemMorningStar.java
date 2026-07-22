package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.MorningStarCombat;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemMorningStar extends ItemArsenalWeapon {
    public ItemMorningStar(WeaponTier tier) {
        super(tier, "morning_star", 4.0D + tier.getMaterial().getAttackDamage(), -3.0D);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.world.isRemote && entity instanceof EntityLivingBase) {
            boolean fullyCharged = player.getCooledAttackStrength(0.5F) >= 0.95F;
            MorningStarCombat.prepareAttack(player, (EntityLivingBase) entity, this, fullyCharged);
        }
        return false;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!attacker.world.isRemote && attacker instanceof EntityPlayer) {
            MorningStarCombat.applyConfirmedHit((EntityPlayer) attacker, target, this);
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_RED + "Fully charged hits inflict Armor Fracture.");
        tooltip.add(TextFormatting.GRAY + "20% less armor per level; max "
            + toRoman(getTier().getMorningStarFractureCap()) + ".");
        tooltip.add(TextFormatting.DARK_GRAY + "30 secs vs mobs; 10 secs vs players.");
    }

    private static String toRoman(int level) {
        switch (level) {
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return Integer.toString(level);
        }
    }
}
