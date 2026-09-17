package com.nanonaitor.arsenal.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LivingEntity.class)
public interface LivingDamageAccessor {
    @Accessor("lastHurt") float arsenal$getLastHurt();
    @Accessor("lastHurt") void arsenal$setLastHurt(float value);
}
