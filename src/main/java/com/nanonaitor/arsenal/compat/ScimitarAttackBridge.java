package com.nanonaitor.arsenal.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

/** Delegate ordinary scimitar strikes to the installed combat engine, not a damage imitation. */
public final class ScimitarAttackBridge {
    private static final ThreadLocal<Boolean> ATTACK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> OFFHAND = ThreadLocal.withInitial(() -> false);
    private static Method attack, clear, add;
    private static boolean initialized;
    private ScimitarAttackBridge() {}
    public static boolean isControlledAttack() { return ATTACK.get(); }
    public static boolean isControlledOffhandAttack() { return ATTACK.get() && OFFHAND.get(); }

    private static void init() {
        if (initialized) return;
        initialized = true;
        if (!Loader.isModLoaded("bettercombatmod")) return;
        try {
            Class<?> helper = Class.forName("bettercombat.mod.util.Helpers");
            attack = helper.getMethod("attackTargetEntityItem", EntityPlayer.class, Entity.class,
                boolean.class, double.class, double.class, double.class);
            clear = helper.getMethod("clearOldModifiers", EntityLivingBase.class, ItemStack.class,
                boolean.class, boolean.class, boolean.class);
            add = helper.getMethod("addNewModifiers", EntityLivingBase.class, ItemStack.class,
                boolean.class, boolean.class, boolean.class);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unsupported RLCombat API: cannot safely resolve scimitar attacks", ex);
        }
    }

    private static void exchange(EntityPlayer player, ItemStack from, ItemStack to) {
        try {
            if (clear != null) {
                clear.invoke(null, player, from, true, true, true);
                add.invoke(null, player, to, true, true, true);
            } else {
                player.getAttributeMap().removeAttributeModifiers(from.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
                player.getAttributeMap().applyAttributeModifiers(to.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
            }
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    public static double cooldown(EntityPlayer player, boolean offhand) {
        init();
        if (!offhand) return Math.max(1.0D, player.getCooldownPeriod());
        ItemStack main = player.getHeldItemMainhand(), off = player.getHeldItemOffhand();
        exchange(player, main, off);
        try { return Math.max(1.0D, player.getCooldownPeriod()); }
        finally { exchange(player, off, main); }
    }

    public static void strike(EntityPlayer player, Entity target, boolean offhand) {
        init();
        net.minecraft.entity.ai.attributes.IAttributeInstance speed = player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_SPEED);
        net.minecraft.entity.ai.attributes.AttributeModifier pairedSpeed = null;
        if (ScimitarShieldCompat.isPair(player)) {
            double ratio = cooldown(player, offhand) / pairedInterval(player);
            pairedSpeed = new net.minecraft.entity.ai.attributes.AttributeModifier(
                java.util.UUID.fromString("4b1c5919-bf2e-420a-ae71-19d702619782"), "Arsenal paired strike timing", ratio - 1.0D, 2).setSaved(false);
            speed.applyModifier(pairedSpeed);
        }
        ATTACK.set(true);
        OFFHAND.set(offhand);
        try {
            if (attack != null) {
                attack.invoke(null, player, target, offhand, player.motionX, player.motionY, player.motionZ);
                return;
            }
            if (!offhand) { player.attackTargetEntityWithCurrentItem(target); return; }
            ItemStack main = player.getHeldItemMainhand(), off = player.getHeldItemOffhand();
            exchange(player, main, off);
            player.inventory.mainInventory.set(player.inventory.currentItem, off);
            player.inventory.offHandInventory.set(0, main);
            try { player.attackTargetEntityWithCurrentItem(target); }
            finally {
                player.inventory.mainInventory.set(player.inventory.currentItem, main);
                player.inventory.offHandInventory.set(0, off);
                exchange(player, off, main);
            }
        } catch (ReflectiveOperationException ex) {
            // A failed attack must never be retried, which could apply damage twice.
            throw new IllegalStateException("RLCombat scimitar attack failed", ex);
        } finally {
            ATTACK.remove();
            OFFHAND.remove();
            if (pairedSpeed != null) speed.removeModifier(pairedSpeed);
        }
    }

    public static double pairedInterval(EntityPlayer player) {
        return com.nanonaitor.arsenal.combat.GuardRules.pairedInterval(cooldown(player, false), cooldown(player, true));
    }

    /** Sum fully charged per-hand attribute damage and target-specific enchantment damage. */
    public static float bashDamage(EntityPlayer player, EntityLivingBase target) {
        init();
        ItemStack main = player.getHeldItemMainhand(), off = player.getHeldItemOffhand();
        double damage = player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue()
            + net.minecraft.enchantment.EnchantmentHelper.getModifierForCreature(main, target.getCreatureAttribute());
        exchange(player, main, off);
        try {
            damage += player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue()
                + net.minecraft.enchantment.EnchantmentHelper.getModifierForCreature(off, target.getCreatureAttribute());
        } finally { exchange(player, off, main); }
        return (float)Math.max(0.0D, damage);
    }

    public static void bashEnchantments(EntityPlayer player, EntityLivingBase target) {
        net.minecraft.enchantment.EnchantmentHelper.applyThornEnchantments(target, player);
        if (Loader.isModLoaded("bettercombatmod")) {
            try {
                Class<?> handler=Class.forName("bettercombat.mod.compat.EnchantCompatHandler");
                java.lang.reflect.Field hand=handler.getField("arthropodFromOffhand");
                java.lang.reflect.Field strength=handler.getField("arthropodCooledStrength");
                boolean previousHand=hand.getBoolean(null);
                float previousStrength=strength.getFloat(null);
                try {
                    strength.setFloat(null,1.0F);
                    hand.setBoolean(null,false);
                    net.minecraft.enchantment.EnchantmentHelper.applyArthropodEnchantments(player,target);
                    hand.setBoolean(null,true);
                    net.minecraft.enchantment.EnchantmentHelper.applyArthropodEnchantments(player,target);
                } finally {hand.setBoolean(null,previousHand);strength.setFloat(null,previousStrength);}
            } catch(ReflectiveOperationException ex) {throw new IllegalStateException("Unable to resolve bash enchantment context",ex);}
        } else {
            net.minecraft.enchantment.EnchantmentHelper.applyArthropodEnchantments(player,target);
            ItemStack main=player.getHeldItemMainhand(),off=player.getHeldItemOffhand();
            player.inventory.mainInventory.set(player.inventory.currentItem,off);
            try {net.minecraft.enchantment.EnchantmentHelper.applyArthropodEnchantments(player,target);}
            finally {player.inventory.mainInventory.set(player.inventory.currentItem,main);}
        }
    }

    public static double reach(EntityPlayer player, boolean offhand) {
        init();
        if (!offhand) return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        ItemStack main = player.getHeldItemMainhand(), off = player.getHeldItemOffhand();
        exchange(player, main, off);
        try { return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue(); }
        finally { exchange(player, off, main); }
    }
}
