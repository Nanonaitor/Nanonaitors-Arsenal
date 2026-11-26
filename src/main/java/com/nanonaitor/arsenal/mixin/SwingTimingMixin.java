package com.nanonaitor.arsenal.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client: records every swing per hand as it happens, for the local player and
 * for other players (whose swings arrive as animation packets). Vanilla keeps a
 * single swing per player, so a hand swinging in the same tick as the other used
 * to hide the first; paired claws now show both hands striking at once.
 */
@Mixin(LivingEntity.class)
public abstract class SwingTimingMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void arsenal$noteSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        Object self = this;
        if (self instanceof Player player && player.level().isClientSide)
            com.nanonaitor.arsenal.client.pose.AttackTiming.noteSwing(player, hand);
    }
}
