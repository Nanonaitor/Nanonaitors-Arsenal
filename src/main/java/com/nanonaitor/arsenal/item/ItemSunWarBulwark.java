package com.nanonaitor.arsenal.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import java.util.List;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;

public final class ItemSunWarBulwark extends ItemArsenalShield {
    public static final double ATTACK_SPEED = 0.25D;

    public ItemSunWarBulwark() { super("sun_war_bulwark", 4096); }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Strain/durability synchronization must not repeatedly lower the held shield.
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    public static int strain(ItemStack stack) {
        return stack.hasTagCompound()?stack.getTagCompound().getInteger("ArsenalGuardStrain"):0;
    }
    public static void recordBlock(EntityPlayer player,ItemStack stack) {
        if(player.world.isRemote || stack.isEmpty())return;
        if(!stack.hasTagCompound())stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        if(strain(stack)==0)stack.getTagCompound().setLong("ArsenalGuardRecovery",player.world.getTotalWorldTime());
        int load=strain(stack)+1;
        if(load>=com.nanonaitor.arsenal.combat.GuardRules.MAX_STRAIN){
            load=0;
            player.getCooldownTracker().setCooldown(stack.getItem(),com.nanonaitor.arsenal.combat.GuardRules.RECOVERY_TICKS);
            player.getEntityData().setBoolean("ArsenalBulwarkMenuGuard",false);
            player.resetActiveHand();
            player.world.setEntityState(player,(byte)30);
        }
        stack.getTagCompound().setInteger("ArsenalGuardStrain",load);
        if(!stack.getTagCompound().hasKey("ArsenalGuardRecovery"))
            stack.getTagCompound().setLong("ArsenalGuardRecovery",player.world.getTotalWorldTime());
    }
    @Override public void onUpdate(ItemStack stack,net.minecraft.world.World world,
            net.minecraft.entity.Entity entity,int slot,boolean selected){
        super.onUpdate(stack,world,entity,slot,selected);
        if(world.isRemote || strain(stack)<=0)return;
        long now=world.getTotalWorldTime(),last=stack.getTagCompound().getLong("ArsenalGuardRecovery");
        if(now<last || now-last>=20){
            stack.getTagCompound().setInteger("ArsenalGuardStrain",com.nanonaitor.arsenal.combat.GuardRules.recoveredStrain(strain(stack),now-last));
            stack.getTagCompound().setLong("ArsenalGuardRecovery",now-(now-last)%20);
        }
    }

    @Override
    public boolean canBeginGuard(EntityPlayer player, EnumHand hand) {
        boolean held = player.getHeldItem(hand).getItem() == this;
        boolean free = hand == EnumHand.MAIN_HAND ? player.getHeldItemOffhand().isEmpty()
            : player.getHeldItemMainhand().isEmpty();
        if (held && !free && !player.world.isRemote) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                "I need both hands to shield with the bulwark!"), true);
        }
        return held && free && !player.getCooldownTracker().hasCooldown(this);
    }

    public boolean isTwoHandedReady(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() == this && player.getHeldItemOffhand().isEmpty()
            || player.getHeldItemOffhand().getItem() == this && player.getHeldItemMainhand().isEmpty();
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 0.0D, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", ATTACK_SPEED - 4.0D, 0));
        }
        return modifiers;
    }

    @Override protected String shieldSummary() {
        return "Extremely durable two-handed fortress shield.";
    }

    @Override protected void appendShieldDetails(List<String> tooltip) {
        line(tooltip, TextFormatting.YELLOW, "Guard Strain: 25 blocked hits force 3 seconds of recovery");
        line(tooltip, TextFormatting.YELLOW, "Guard Strain recovers by 1 point each second");
        line(tooltip, TextFormatting.AQUA, "15% passive damage reduction when ready");
        line(tooltip, TextFormatting.BLUE, "Can shield all directed attacks from any direction");
        line(tooltip, TextFormatting.RED, "Damage: 1 + total armor points");
        line(tooltip, TextFormatting.DARK_GRAY, "Guard and attack for a 4-block area bash");
        line(tooltip, TextFormatting.GRAY, "40% slower while carried; 75% slower while guarding");
        line(tooltip, TextFormatting.DARK_RED, "Needs both hands free to guard; passive always works");
    }
}
