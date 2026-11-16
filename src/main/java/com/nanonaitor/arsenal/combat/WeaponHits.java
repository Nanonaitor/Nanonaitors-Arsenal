package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Hit rules shared by Arsenal's custom (non-vanilla) attacks. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class WeaponHits {
    private WeaponHits() {}

    /** Vanilla's attack-cooldown damage curve: 20% at no charge, 100% at full charge. */
    public static float chargeMultiplier(float cooledStrength) {
        return 0.2F + cooledStrength * cooledStrength * 0.8F;
    }

    public static float chargeMultiplier(EntityPlayer player) {
        return chargeMultiplier(player.getCooledAttackStrength(0.5F));
    }

    /** Vanilla's falling-critical conditions (sprinting hits are never critical). */
    public static boolean isVanillaCritical(EntityPlayer player) {
        return player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder()
            && !player.isInWater() && !player.isPotionActive(MobEffects.BLINDNESS)
            && !player.isRiding() && !player.isSprinting();
    }

    public static void applyFireAspect(EntityPlayer player, EntityLivingBase target) {
        int fireAspect = EnchantmentHelper.getFireAspectModifier(player);
        if (fireAspect > 0) target.setFire(fireAspect * 4);
    }

    /** Thorns on the target's armor and Bane-of-Arthropods-style weapon effects. */
    public static void applyHitEnchantments(EntityPlayer player, EntityLivingBase target) {
        EnchantmentHelper.applyThornEnchantments(target, player);
        EnchantmentHelper.applyArthropodEnchantments(player, target);
    }

    /**
     * Fire Aspect, the Knockback enchantment along the player's facing (scaled),
     * and on-hit enchantments, as applied after a confirmed custom hit.
     */
    public static void applyPostHitEffects(EntityPlayer player, EntityLivingBase target,
                                           float knockbackScale) {
        applyFireAspect(player, target);
        int knockback = EnchantmentHelper.getKnockbackModifier(player);
        if (knockback > 0) {
            target.knockBack(player, knockback * knockbackScale,
                MathHelper.sin(player.rotationYaw * 0.017453292F),
                -MathHelper.cos(player.rotationYaw * 0.017453292F));
        }
        applyHitEnchantments(player, target);
    }

    /** Swing-driven weapons never mine; their left click belongs to the weapon. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventWeaponMining(PlayerInteractEvent.LeftClickBlock event) {
        Item held = event.getEntityPlayer().getHeldItemMainhand().getItem();
        if (held instanceof ItemBallAndChain || held instanceof ItemFlail
            || held instanceof ItemMorningStar) {
            denyBlockClick(event);
        }
    }

    public static void denyBlockClick(PlayerInteractEvent.LeftClickBlock event) {
        event.setCanceled(true);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
    }
}
