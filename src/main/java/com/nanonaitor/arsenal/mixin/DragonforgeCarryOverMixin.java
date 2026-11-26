package com.nanonaitor.arsenal.mixin;

import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Ice & Fire hook (skipped when Ice & Fire is absent). The Dragonforge
 * puts a fresh copy of its recipe result in the output slot, which would drop an
 * upgraded Arsenal weapon's name, enchantments, data and wear. This copies them
 * from the dragonbone weapon that went in. Slots: 0 input, 1 blood, 2 output.
 */
@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforge", remap = false)
public abstract class DragonforgeCarryOverMixin {
    @Unique private ItemStack arsenal$input = ItemStack.EMPTY;

    @Inject(method = "smeltItem", at = @At("HEAD"), require = 0, remap = false)
    private void arsenal$rememberInput(CallbackInfo ci) {
        Container forge = (Container)(Object)this;
        ItemStack input = forge.getItem(0);
        arsenal$input = input.getItem() instanceof ArsenalWeaponItem && forge.getItem(2).isEmpty()
            ? input.copy() : ItemStack.EMPTY;
    }

    @Inject(method = "smeltItem", at = @At("RETURN"), require = 0, remap = false)
    private void arsenal$carryOver(CallbackInfo ci) {
        ItemStack old = arsenal$input;
        arsenal$input = ItemStack.EMPTY;
        if (old.isEmpty()) return;
        ItemStack output = ((Container)(Object)this).getItem(2);
        if (output.getItem() instanceof ArsenalWeaponItem && output.getCount() == 1)
            com.nanonaitor.arsenal.registry.ModRecipes.carryOver(old, output);
    }
}
