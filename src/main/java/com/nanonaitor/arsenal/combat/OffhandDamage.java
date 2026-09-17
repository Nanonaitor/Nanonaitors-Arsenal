package com.nanonaitor.arsenal.combat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import com.nanonaitor.arsenal.mixin.LivingDamageAccessor;

/** Independent offhand immunity, not unconditional i-frame piercing. */
public final class OffhandDamage {
    private record Window(long until,float damage) {}
    private static final java.util.Map<LivingEntity,Window> WINDOWS=new java.util.WeakHashMap<>();
    public static boolean hurt(ItemStack stack, Entity hitbox, LivingEntity target, DamageSource source,float amount) {
        long now=target.level().getGameTime();
        Window window=WINDOWS.getOrDefault(target,new Window(now,0));
        var accessor=(LivingDamageAccessor)target;
        int mainTime=target.invulnerableTime;float mainDamage=accessor.arsenal$getLastHurt();
        target.invulnerableTime=(int)Math.max(0,window.until-now);
        accessor.arsenal$setLastHurt(window.damage);
        try { return TierEffects.hurtEntityWithStack(stack,hitbox,source,amount); }
        finally {
            WINDOWS.put(target,new Window(now+target.invulnerableTime,accessor.arsenal$getLastHurt()));
            target.invulnerableTime=mainTime;accessor.arsenal$setLastHurt(mainDamage);
        }
    }
    private OffhandDamage() {}
}
