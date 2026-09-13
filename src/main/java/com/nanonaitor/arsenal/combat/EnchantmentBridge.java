package com.nanonaitor.arsenal.combat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Applies the actual striking stack's enchantments, including offhand attacks. */
final class EnchantmentBridge {
    static float modifyDamage(ServerLevel level,ItemStack stack,LivingEntity target,DamageSource source,float base){
        return base+EnchantmentHelper.getDamageBonus(stack,target.getMobType());
    }
    static float modifyKnockback(ServerLevel level,ItemStack stack,LivingEntity target,DamageSource source,float base){
        return base+EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK,stack);
    }
    static void doPostAttackEffectsWithItemSource(ServerLevel level,LivingEntity target,DamageSource source,ItemStack stack){
        if(!(source.getEntity() instanceof LivingEntity attacker)) return;
        EnchantmentHelper.getEnchantments(stack).forEach((enchantment,value)->enchantment.doPostAttack(attacker,target,value));
        EnchantmentHelper.doPostHurtEffects(target,attacker);
        int fire=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT,stack);
        if(fire>0) target.setSecondsOnFire(fire*4);
    }
}
