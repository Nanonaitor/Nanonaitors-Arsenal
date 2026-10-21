package com.nanonaitor.arsenal.combat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import com.nanonaitor.arsenal.mixin.LivingDamageAccessor;

/**
 * Gives each player's selected weapon hand its own normal hurt window.
 *
 * <p>Minecraft stores one invulnerability window on the target. Alternating
 * paired weapons would therefore cause one hand to suppress the other even
 * though each hand has already passed its own attack-speed cooldown. We swap
 * in the selected hand's saved window only for the duration of the hit, then
 * restore the target's vanilla/global state. Repeated hits from that same hand
 * still obey the ordinary damage-window rules.</p>
 *
 * <p>The class name is retained for binary/source compatibility with earlier
 * 1.20.1 builds; it now supports both hands.</p>
 */
public final class OffhandDamage {
    private record HandKey(java.util.UUID attacker, boolean offhand) {}
    private record Window(long until,float damage) {}
    private static final java.util.Map<LivingEntity,java.util.Map<HandKey,Window>> WINDOWS=
        new java.util.WeakHashMap<>();

    /** Legacy entry point: an independent off-hand hit. */
    public static boolean hurt(ItemStack stack, Entity hitbox, LivingEntity target, DamageSource source,float amount) {
        return hurt(stack,hitbox,target,source,amount,true);
    }

    /** Applies a full selected-hand hit while preserving separate main/off-hand immunity. */
    public static boolean hurt(ItemStack stack, Entity hitbox, LivingEntity target, DamageSource source,
            float amount, boolean offhand) {
        long now=target.level().getGameTime();
        java.util.UUID attacker=source.getEntity() instanceof Player player
            ? player.getUUID() : new java.util.UUID(0L,0L);
        HandKey key=new HandKey(attacker,offhand);
        java.util.Map<HandKey,Window> targetWindows=WINDOWS.computeIfAbsent(target,
            ignored -> new java.util.HashMap<>());
        Window window=targetWindows.getOrDefault(key,new Window(now,0));
        var accessor=(LivingDamageAccessor)target;
        int mainTime=target.invulnerableTime;float mainDamage=accessor.arsenal$getLastHurt();
        target.invulnerableTime=(int)Math.max(0,window.until-now);
        accessor.arsenal$setLastHurt(window.damage);
        try { return TierEffects.hurtEntityWithStack(stack,hitbox,source,amount); }
        finally {
            targetWindows.put(key,new Window(now+target.invulnerableTime,accessor.arsenal$getLastHurt()));
            target.invulnerableTime=mainTime;accessor.arsenal$setLastHurt(mainDamage);
        }
    }
    private OffhandDamage() {}
}
