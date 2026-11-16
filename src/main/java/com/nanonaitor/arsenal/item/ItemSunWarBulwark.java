package com.nanonaitor.arsenal.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
        // Strain only shows how hard the guard is being pressed. The bulwark's
        // guard is unbreakable, so a full bar never drops or disables it.
        int load=Math.min(com.nanonaitor.arsenal.combat.GuardRules.MAX_STRAIN,strain(stack)+1);
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
        line(tooltip, TextFormatting.GRAY, "Blocks from every direction; attack while guarding to bash.");
        line(tooltip, TextFormatting.GRAY, "Its guard cannot be broken; the strain bar shows pressure.");
        line(tooltip, TextFormatting.RED, "Two-Handed");
    }
}
