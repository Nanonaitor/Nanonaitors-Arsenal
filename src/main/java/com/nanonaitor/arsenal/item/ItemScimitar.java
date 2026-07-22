package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.ScimitarCombat;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemScimitar extends ItemArsenalWeapon {
    public ItemScimitar(WeaponTier tier) {
        super(tier, "scimitar", 2.5D + tier.getMaterial().getAttackDamage(), -2.2D);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.world.isRemote && entity instanceof EntityLivingBase) {
            ScimitarCombat.prepareAttack(player, (EntityLivingBase) entity, this,
                player.getCooledAttackStrength(0.5F) >= 0.95F);
        }
        return false;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!attacker.world.isRemote && attacker instanceof EntityPlayer) {
            ScimitarCombat.confirmHit((EntityPlayer) attacker, target, this);
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_PURPLE
            + "Fully charged hits: 10% chance to inflict Weakness II for 2 secs.");
    }
}
