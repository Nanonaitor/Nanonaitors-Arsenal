package com.nanonaitor.arsenal.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/** Delegate ordinary scimitar strikes to the installed combat engine, not a damage imitation. */
public final class ScimitarAttackBridge {
    private static final ThreadLocal<Boolean> ATTACK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> OFFHAND = ThreadLocal.withInitial(() -> false);
    private static Method attack, clear, add, offhandDamage;
    private static java.lang.reflect.Constructor<?> mainPacket, offPacket;
    private static java.lang.reflect.Field packetChannel;
    private static java.lang.reflect.Field mainCooldownTicks, offhandCooldownTicks;
    private static boolean initialized;
    private ScimitarAttackBridge() {}
    public static boolean isControlledAttack() { return ATTACK.get(); }
    public static boolean isControlledOffhandAttack() { return ATTACK.get() && OFFHAND.get(); }

    private static void init() {
        if (initialized) return;
        if (!Loader.isModLoaded("bettercombatmod")) { initialized=true; return; }
        try {
            Class<?> helper = Class.forName("bettercombat.mod.util.Helpers");
            Method resolvedAttack = helper.getMethod("attackTargetEntityItem", EntityPlayer.class, Entity.class,
                boolean.class, double.class, double.class, double.class);
            Method[] modifiers=CombatModifierMethods.resolve(helper);
            attack=resolvedAttack;clear=modifiers[0];add=modifiers[1];
            try {
                offhandDamage=helper.getMethod("getOffhandDamage",EntityPlayer.class);
            } catch(NoSuchMethodException legacyWithoutDamageHelper) {
                offhandDamage=null;
            }
            initialized=true;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unsupported RLCombat API: cannot safely resolve scimitar attacks", ex);
        }
    }

    private static void exchange(EntityPlayer player, ItemStack from, ItemStack to,
                                 boolean damage, boolean speed, boolean reach) {
        try {
            if (clear != null) {
                CombatModifierMethods.invoke(clear,player,from,damage,speed,reach);
                CombatModifierMethods.invoke(add,player,to,damage,speed,reach);
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
        exchange(player, main, off, false, true, false);
        try { return Math.max(1.0D, player.getCooldownPeriod()); }
        finally { exchange(player, off, main, false, true, false); }
    }

    /** The charge value RLCombat itself will use, without resetting either hand. */
    public static float cooledStrength(EntityPlayer player, boolean offhand) {
        if (!offhand || !Loader.isModLoaded("bettercombatmod"))
            return player.getCooledAttackStrength(0.5F);
        try {
            Class<?> handlers=Class.forName("bettercombat.mod.handler.EventHandlers");
            Capability<?> capability=(Capability<?>)handlers.getField("OFFHAND_COOLDOWN").get(null);
            Object state=player.getCapability(capability,null);
            if(state==null)return 1.0F;
            int ticks=((Number)state.getClass().getMethod("getTicksSinceLastSwing").invoke(state)).intValue();
            return net.minecraft.util.math.MathHelper.clamp((float)((ticks+0.5D)/cooldown(player,true)),0.0F,1.0F);
        } catch(ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to read RLCombat offhand charge",ex);
        }
    }

    public static boolean ready(EntityPlayer player, boolean offhand) {
        return cooledStrength(player,offhand)>=0.95F;
    }

    /**
     * Send precisely the packet RLCombat sends for a manual swing. Arsenal owns
     * only the alternating input clock; RLCombat remains the sole authority for
     * the selected hand's cooldown, attributes, damage, enchantments and i-frames.
     */
    public static boolean sendNativeAttack(EntityPlayer player,int targetId,boolean offhand) {
        if(!Loader.isModLoaded("bettercombatmod") || targetId<0)return false;
        try {
            if(mainPacket==null) {
                Class<?> main=Class.forName("bettercombat.mod.network.PacketMainhandAttack");
                Class<?> off=Class.forName("bettercombat.mod.network.PacketOffhandAttack");
                mainPacket=main.getConstructor(int.class,double.class,double.class,double.class);
                offPacket=off.getConstructor(int.class,double.class,double.class,double.class);
                Class<?> handler=Class.forName("bettercombat.mod.network.PacketHandler");
                packetChannel=handler.getField("instance");
            }
            Object packet=(offhand?offPacket:mainPacket).newInstance(targetId,
                player.motionX,player.motionY,player.motionZ);
            ((SimpleNetworkWrapper)packetChannel.get(null)).sendToServer((IMessage)packet);
            consumeClientCooldown(player,offhand);
            return true;
        } catch(ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to send RLCombat's native scimitar attack",ex);
        }
    }

    private static void consumeClientCooldown(EntityPlayer player,boolean offhand)
            throws ReflectiveOperationException {
        if(!offhand) { player.resetCooldown(); return; }
        Class<?> handlers=Class.forName("bettercombat.mod.handler.EventHandlers");
        Capability<?> capability=(Capability<?>)handlers.getField("OFFHAND_COOLDOWN").get(null);
        Object state=player.getCapability(capability,null);
        if(state!=null)state.getClass().getMethod("resetTicksSinceLastSwing").invoke(state);
    }

    /**
     * A paired automatic turn represents one fully charged manual swing from the
     * selected hand.  The pair clock controls cadence; RLCombat still performs
     * the complete per-hand damage/enchantment/offhand calculation and resets
     * only that hand's native cooldown after the strike.
     */
    public static void prepareScheduledStrike(EntityPlayer player, boolean offhand) {
        init();
        int readyTicks=(int)Math.ceil(cooldown(player,offhand));
        try {
            if(!offhand) {
                if(mainCooldownTicks==null) {
                    mainCooldownTicks=resolveMainCooldownTicksField();
                    mainCooldownTicks.setAccessible(true);
                }
                mainCooldownTicks.setInt(player,readyTicks);
                return;
            }
            if(!Loader.isModLoaded("bettercombatmod"))return;
            Class<?> handlers=Class.forName("bettercombat.mod.handler.EventHandlers");
            Capability<?> capability=(Capability<?>)handlers.getField("OFFHAND_COOLDOWN").get(null);
            Object state=player.getCapability(capability,null);
            if(state==null)return;
            if(offhandCooldownTicks==null) {
                offhandCooldownTicks=state.getClass().getDeclaredField("ticksSinceLastSwing");
                offhandCooldownTicks.setAccessible(true);
            }
            offhandCooldownTicks.setInt(state,readyTicks);
        } catch(ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to prepare a full-strength scheduled scimitar strike",ex);
        }
    }

    /**
     * The vanilla main-hand cooldown belongs to EntityLivingBase in 1.12.2.
     * ReflectionHelper only checks fields declared by the supplied class, so
     * resolving it from EntityPlayer fails in an obfuscated RLCraft runtime.
     */
    static java.lang.reflect.Field resolveMainCooldownTicksField() {
        return ReflectionHelper.findField(EntityLivingBase.class,
            "ticksSinceLastSwing","field_184617_aD","aE");
    }

    public static void strike(EntityPlayer player, Entity target, boolean offhand) {
        init();
        // This is deliberately the same attack path used by a manual RLCombat
        // swing. Scheduling is handled per hand by the input/server clocks; do
        // not modify attack speed, cooled strength, hurt resistance, or the
        // configured weaker-offhand multiplier here.
        ATTACK.set(true);
        OFFHAND.set(offhand);
        try {
            if (attack != null) {
                attack.invoke(null, player, target, offhand, player.motionX, player.motionY, player.motionZ);
                return;
            }
            if (!offhand) { player.attackTargetEntityWithCurrentItem(target); return; }
            ItemStack main = player.getHeldItemMainhand(), off = player.getHeldItemOffhand();
            exchange(player, main, off, true, true, true);
            player.inventory.mainInventory.set(player.inventory.currentItem, off);
            player.inventory.offHandInventory.set(0, main);
            try { player.attackTargetEntityWithCurrentItem(target); }
            finally {
                player.inventory.mainInventory.set(player.inventory.currentItem, main);
                player.inventory.offHandInventory.set(0, off);
                exchange(player, off, main, true, true, true);
            }
        } catch (ReflectiveOperationException ex) {
            // A failed attack must never be retried, which could apply damage twice.
            throw new IllegalStateException("RLCombat scimitar attack failed", ex);
        } finally {
            ATTACK.remove();
            OFFHAND.remove();
        }
    }

    public static double pairedInterval(EntityPlayer player) {
        return com.nanonaitor.arsenal.combat.GuardRules.pairedInterval(cooldown(player, false), cooldown(player, true));
    }

    /** Each hand has separate RLCombat i-frames, but the same hand still needs ten ticks before an equal hit. */
    public static double pairedDamageInterval(EntityPlayer player) {
        return com.nanonaitor.arsenal.combat.GuardRules.damageSafeTurnInterval(pairedInterval(player));
    }

    /**
     * Sum one fully charged main-hand hit and one fully charged off-hand hit.
     * RLCombat applies its weaker-offhand multiplier only to attack attributes;
     * target-specific enchantment damage remains full strength for either hand.
     */
    public static float bashDamage(EntityPlayer player, EntityLivingBase target) {
        init();
        ItemStack main = player.getHeldItemMainhand(), off = player.getHeldItemOffhand();
        double mainAttack=player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float mainEnchantment=net.minecraft.enchantment.EnchantmentHelper.getModifierForCreature(
            main,target.getCreatureAttribute());
        exchange(player, main, off, true, false, false);
        try {
            double offAttack=offhandAttackDamage(player);
            float offEnchantment=net.minecraft.enchantment.EnchantmentHelper.getModifierForCreature(
                off,target.getCreatureAttribute());
            return combineBashDamage(mainAttack,mainEnchantment,offAttack,offEnchantment);
        } finally { exchange(player, off, main, true, false, false); }
    }

    private static double offhandAttackDamage(EntityPlayer player) {
        if(offhandDamage==null)
            return player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE)
                .getAttributeValue();
        try {
            return ((Number)offhandDamage.invoke(null,player)).doubleValue();
        } catch(ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to calculate RLCombat offhand bash damage",ex);
        }
    }

    static float combineBashDamage(double mainAttack,float mainEnchantment,
                                   double offAttack,float offEnchantment) {
        return (float)Math.max(0.0D,mainAttack+mainEnchantment+offAttack+offEnchantment);
    }

    public static void bashEnchantments(EntityPlayer player, EntityLivingBase target) {
        net.minecraft.enchantment.EnchantmentHelper.applyThornEnchantments(target, player);
        if (Loader.isModLoaded("bettercombatmod") && hasModernEnchantContext()) {
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

    private static boolean hasModernEnchantContext() {
        try {
            Class<?> handler=Class.forName("bettercombat.mod.compat.EnchantCompatHandler");
            handler.getField("arthropodFromOffhand");handler.getField("arthropodCooledStrength");
            return true;
        } catch(ClassNotFoundException | NoSuchFieldException legacyVersion) { return false; }
    }

    public static double reach(EntityPlayer player, boolean offhand) {
        init();
        if (!offhand) return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        ItemStack main = player.getHeldItemMainhand(), off = player.getHeldItemOffhand();
        exchange(player, main, off, false, false, true);
        try { return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue(); }
        finally { exchange(player, off, main, false, false, true); }
    }
}
