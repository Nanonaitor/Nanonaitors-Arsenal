package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemBallAndChain extends ItemArsenalWeapon {
    public ItemBallAndChain(WeaponTier tier) {
        super(tier, "ball_and_chain", 3.0D + tier.getMaterial().getAttackDamage(), -3.4D);
        addPropertyOverride(new ResourceLocation(NanonaitorsArsenal.MOD_ID, "swinging"),
            (stack, world, entity) -> isActivelySwinging(stack, entity) ? 1.0F : 0.0F);
        addPropertyOverride(new ResourceLocation(NanonaitorsArsenal.MOD_ID, "animation_part"),
            (stack, world, entity) -> stack.hasTagCompound()
                ? stack.getTagCompound().getInteger("ArsenalAnimationPart") : 0.0F);
    }

    private static boolean isActivelySwinging(ItemStack stack, EntityLivingBase entity) {
        return entity != null
            && entity.getHeldItemMainhand().getItem() == stack.getItem()
            && (entity.getEntityData().getBoolean("ArsenalBallAndChainActive")
                || entity.isHandActive());
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(TextFormatting.RED + "Two-Handed");
        tooltip.add(TextFormatting.GOLD + "Hold attack to build up to 3 charges.");
        tooltip.add(TextFormatting.GRAY + "Wind-up sweeps 3 blocks ahead and +/-1 vertically.");
        tooltip.add(TextFormatting.GRAY + "Release to throw 4 blocks per charge.");
        tooltip.add(TextFormatting.GRAY + "Hits in both directions; stops at solid blocks.");
        tooltip.add(TextFormatting.DARK_RED + "3-charge throw pierces "
            + getTier().getArmorPiercePercent() + "% armor.");
        tooltip.add(TextFormatting.DARK_GRAY + "Throw hits fracture armor.");
        tooltip.add(TextFormatting.DARK_GRAY + "Requires an empty offhand.");
    }

}
