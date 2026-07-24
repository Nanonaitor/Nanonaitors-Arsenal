package com.nanonaitor.arsenal.item;

import net.minecraft.world.item.ToolMaterial;

public enum WeaponTier {
    WOOD("wood", ToolMaterial.WOOD, 2, 0),
    STONE("stone", ToolMaterial.STONE, 3, 1),
    COPPER("copper", ToolMaterial.COPPER, 3, 1),
    GOLD("gold", ToolMaterial.GOLD, 3, 1),
    IRON("iron", ToolMaterial.IRON, 4, 2),
    DIAMOND("diamond", ToolMaterial.DIAMOND, 5, 3),
    NETHERITE("netherite", ToolMaterial.NETHERITE, 5, 4);

    public final String id;
    public final ToolMaterial material;
    public final int fractureCap;
    public final int ramBreakLevel;

    WeaponTier(String id, ToolMaterial material, int fractureCap, int ramBreakLevel) {
        this.id = id;
        this.material = material;
        this.fractureCap = fractureCap;
        this.ramBreakLevel = ramBreakLevel;
    }

    public float swordDamage() { return 4.0F + material.attackDamageBonus(); }
    public float clawDamage() { return swordDamage() * 0.5F; }
    public int armorPiercePercent() {
        return switch (this) {
            case WOOD -> 25;
            case STONE, COPPER, GOLD -> 50;
            case IRON -> 75;
            case DIAMOND, NETHERITE -> 100;
        };
    }
}
