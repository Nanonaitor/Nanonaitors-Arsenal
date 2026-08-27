package com.nanonaitor.arsenal.effect;

import com.nanonaitor.arsenal.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Temporarily suspends mob AI, then restores the exact state it had before. */
public final class StunnedEffect extends MobEffect {
    private static final String MANAGED = "NanonaitorsArsenalStunManaged";
    private static final String PREVIOUS_NO_AI = "NanonaitorsArsenalStunPreviousNoAi";

    public StunnedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF6C945);
    }

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (!(living instanceof Mob mob) || living.level().isClientSide()) return;
        var data = mob.getPersistentData();
        boolean stunned = mob.hasEffect(ModEffects.STUNNED.getHolder().orElseThrow());
        if (stunned) {
            if (!data.getBooleanOr(MANAGED, false)) {
                data.putBoolean(PREVIOUS_NO_AI, mob.isNoAi());
                data.putBoolean(MANAGED, true);
            }
            mob.setNoAi(true);
        } else if (data.getBooleanOr(MANAGED, false)) {
            mob.setNoAi(data.getBooleanOr(PREVIOUS_NO_AI, false));
            data.remove(MANAGED);
            data.remove(PREVIOUS_NO_AI);
        }
    }
}
