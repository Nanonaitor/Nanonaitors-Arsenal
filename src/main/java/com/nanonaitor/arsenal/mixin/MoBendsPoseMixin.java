package com.nanonaitor.arsenal.mixin;

import com.nanonaitor.arsenal.client.MoBendsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Mo' Bends hook (skipped when Mo' Bends is absent). After Mo' Bends has let the
 * animation mods it knows adopt their vanilla-model poses, Arsenal's weapon pose is adopted
 * the same way; see {@link MoBendsCompat}.
 */
@Pseudo
@Mixin(targets = "goblinbob.mobends.standard.mutators.BipedMutator", remap = false)
public abstract class MoBendsPoseMixin {
    @Inject(method = "adoptExternalArmPose", at = @At("TAIL"), require = 0, remap = false)
    private void arsenal$adoptWeaponPose(CallbackInfo ci) {
        MoBendsCompat.adopt(this);
    }
}
