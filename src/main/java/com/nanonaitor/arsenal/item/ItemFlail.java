package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.FlailCombat;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemFlail extends ItemArsenalWeapon {
    public ItemFlail(WeaponTier tier) {
        super(tier, "flail", 2.0D + tier.getMaterial().getAttackDamage(), -3.2D);
        addPropertyOverride(new ResourceLocation("nanonaitors_arsenal", "swinging"),
            (stack, world, entity) -> entity != null
                && com.nanonaitor.arsenal.data.TransientData.of(entity).getBoolean("ArsenalFlailActive")
                && entity.getHeldItemMainhand().getItem() == this
                && (!(entity instanceof EntityPlayer)
                    || !FlailCombat.isBlockingConventionalShield((EntityPlayer) entity))
                ? 1.0F : 0.0F);
        addPropertyOverride(new ResourceLocation("nanonaitors_arsenal", "animation_part"),
            (stack, world, entity) -> stack.hasTagCompound()
                ? stack.getTagCompound().getInteger("ArsenalAnimationPart") : 0.0F);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "Hold left-click to swing continuously.")) return;
        tooltip.add(TextFormatting.GRAY + "Hits visible enemies within "
            + com.nanonaitor.arsenal.config.ArsenalConfig.reach.flailRadius + " base blocks.");
    }
}
