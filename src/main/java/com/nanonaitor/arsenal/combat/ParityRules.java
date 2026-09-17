package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared client/server rules; compute selected-hand attributes without changing player state. */
public final class ParityRules {
    public static boolean scimitar(ItemStack stack) {
        return stack.getItem() instanceof ArsenalWeaponItem w && w.kind() == WeaponKind.SCIMITAR;
    }
    public static boolean pair(Player p) { return scimitar(p.getMainHandItem()) && scimitar(p.getOffhandItem()); }
    public static boolean disabled(Player p) {
        return pair(p) && (p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem())
            || p.getCooldowns().isOnCooldown(p.getOffhandItem().getItem()));
    }
    public static boolean guarding(Player p) {
        return pair(p) && !disabled(p) && p.isUsingItem() && scimitar(p.getUseItem());
    }
    public static void disable(Player p, int ticks) {
        p.getCooldowns().addCooldown(p.getMainHandItem().getItem(), ticks);
        p.getCooldowns().addCooldown(p.getOffhandItem().getItem(), ticks);
        p.stopUsingItem();
    }
    public static double attribute(Player p, ItemStack selected, Attribute type) {
        AttributeInstance original = p.getAttribute(type);
        if (original == null) return 0;
        AttributeInstance copy = new AttributeInstance(type, ignored -> {});
        copy.setBaseValue(original.getBaseValue());
        original.getModifiers().forEach(copy::addTransientModifier);
        p.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).get(type)
            .forEach(m -> copy.removeModifier(m.getId()));
        selected.getAttributeModifiers(EquipmentSlot.MAINHAND).get(type).forEach(m -> {
            copy.removeModifier(m.getId()); copy.addTransientModifier(m);
        });
        return copy.getValue();
    }
    public static double speed(Player p, ItemStack stack) {
        double speed = attribute(p, stack, Attributes.ATTACK_SPEED);
        var haste = p.getEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED);
        if (haste != null) speed *= 1 + .2D * (haste.getAmplifier() + 1);
        return Math.max(.1D, speed);
    }
    public static double interval(Player p, boolean offhand) {
        return 20D / (pair(p) ? speed(p,p.getMainHandItem()) + speed(p,p.getOffhandItem())
            : speed(p, offhand ? p.getOffhandItem() : p.getMainHandItem()));
    }
    public static int blockWear(float amount) { return Math.max(1, (int)Math.ceil(amount / 2D)); }
    public static float ballMultiplier(int charge) { return new float[]{0,1,1.5F,2}[Math.max(1,Math.min(3,charge))]; }
    private ParityRules() {}
}
