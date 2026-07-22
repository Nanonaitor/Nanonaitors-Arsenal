package com.nanonaitor.arsenal.item;

import net.minecraft.item.Item;
import net.minecraftforge.common.util.EnumHelper;

public enum WeaponTier {
    WOOD("wood", Item.ToolMaterial.WOOD, 2, Family.VANILLA, "ore:plankWood", 0),
    STONE("stone", Item.ToolMaterial.STONE, 3, Family.VANILLA, "ore:cobblestone", 1),
    GOLD("gold", Item.ToolMaterial.GOLD, 3, Family.VANILLA, "ore:ingotGold", 1),
    IRON("iron", Item.ToolMaterial.IRON, 4, Family.VANILLA, "ore:ingotIron", 2),
    DIAMOND("diamond", Item.ToolMaterial.DIAMOND, 5, Family.VANILLA, "ore:gemDiamond", 3),

    SILVER("silver", material("ARSENAL_SILVER", 2, 460, 7F, 1.5F, 16), 4,
        Family.SPARTAN, "ore:ingotSilver", 2),
    BRONZE("bronze", material("ARSENAL_BRONZE", 2, 200, 6F, 2F, 12), 4,
        Family.SPARTAN, "ore:ingotBronze", 2),
    STEEL("steel", material("ARSENAL_STEEL", 3, 480, 8F, 2.5F, 14), 5,
        Family.SPARTAN, "ore:ingotSteel", 3),
    UMBRIUM("umbrium", material("ARSENAL_UMBRIUM", 2, 320, 7F, 2F, 20), 4,
        Family.DEFILED, "defiledlands:umbrium_ingot", 3),
    DRAGONBONE("dragonbone", material("ARSENAL_DRAGONBONE", 3, 1660, 10F, 4F, 22), 5,
        Family.ICE_AND_FIRE, "iceandfire:dragonbone", 3),
    FLAMED_DRAGONBONE("flamed_dragonbone", material("ARSENAL_FLAMED", 3, 2000, 12F, 5.5F, 22), 5,
        Family.FIRE_DRAGONBONE, "iceandfire:dragonbone", 3),
    ICED_DRAGONBONE("iced_dragonbone", material("ARSENAL_ICED", 3, 2000, 12F, 5.5F, 22), 5,
        Family.ICE_DRAGONBONE, "iceandfire:dragonbone", 3),
    ELECTRIC_DRAGONBONE("electric_dragonbone", material("ARSENAL_ELECTRIC", 3, 2000, 12F, 5.5F, 22), 5,
        Family.LIGHTNING_DRAGONBONE, "iceandfire:dragonbone", 3),
    DESERT_MYRMEX("desert_myrmex", material("ARSENAL_DESERT_MYRMEX", 2, 600, 7F, 1F, 8), 4,
        Family.DESERT_MYRMEX, "iceandfire:myrmex_desert_chitin", 2),
    JUNGLE_MYRMEX("jungle_myrmex", material("ARSENAL_JUNGLE_MYRMEX", 2, 600, 7F, 1F, 8), 4,
        Family.JUNGLE_MYRMEX, "iceandfire:myrmex_jungle_chitin", 2),
    DESERT_VENOM("desert_venom", material("ARSENAL_DESERT_VENOM", 2, 600, 7F, 1F, 8), 4,
        Family.DESERT_MYRMEX, "iceandfire:myrmex_desert_chitin", 2),
    JUNGLE_VENOM("jungle_venom", material("ARSENAL_JUNGLE_VENOM", 2, 600, 7F, 1F, 8), 4,
        Family.JUNGLE_MYRMEX, "iceandfire:myrmex_jungle_chitin", 2),
    LIVING("living", material("ARSENAL_LIVING", 3, 1000, 12F, 11F, 1), 5,
        Family.SRP, "srparasites:infectious_blade_fragment", 3),
    SENTIENT("sentient", material("ARSENAL_SENTIENT", 3, 1000, 14F, 16F, 1), 5,
        Family.SRP, "srparasites:infectious_blade_fragment", 3);

    public enum Family { VANILLA, SPARTAN, DEFILED, ICE_AND_FIRE, FIRE_DRAGONBONE,
        ICE_DRAGONBONE, LIGHTNING_DRAGONBONE, DESERT_MYRMEX, JUNGLE_MYRMEX, SRP }

    private final String id;
    private final Item.ToolMaterial material;
    private final int morningStarFractureCap;
    private final Family family;
    private final String repairIngredient;
    private final int ramBreakLevel;

    WeaponTier(String id, Item.ToolMaterial material, int fractureCap, Family family,
               String repairIngredient, int ramBreakLevel) {
        this.id = id; this.material = material; this.morningStarFractureCap = fractureCap;
        this.family = family; this.repairIngredient = repairIngredient;
        this.ramBreakLevel = ramBreakLevel;
    }

    private static Item.ToolMaterial material(String name, int harvest, int uses,
                                               float efficiency, float damage, int enchantability) {
        return EnumHelper.addToolMaterial(name, harvest, uses, efficiency, damage, enchantability);
    }

    public String getId() { return id; }
    public Item.ToolMaterial getMaterial() { return material; }
    public int getMorningStarFractureCap() { return morningStarFractureCap; }
    public double getClawAttackDamage() { return (4.0D + material.getAttackDamage()) * 0.5D; }
    public Family getFamily() { return family; }
    public String getRepairIngredient() { return repairIngredient; }
    public int getRamBreakLevel() { return ramBreakLevel; }
    public boolean isMyrmex() { return this == DESERT_MYRMEX || this == JUNGLE_MYRMEX
        || this == DESERT_VENOM || this == JUNGLE_VENOM; }
    public boolean isVenom() { return this == DESERT_VENOM || this == JUNGLE_VENOM; }
    public boolean isDragonBlooded() { return this == FLAMED_DRAGONBONE
        || this == ICED_DRAGONBONE || this == ELECTRIC_DRAGONBONE; }
    public int getArmorPiercePercent() {
        if (this == WOOD) return 25;
        if (this == STONE || this == GOLD || this == BRONZE) return 50;
        if (this == IRON || this == SILVER || this == STEEL || this == UMBRIUM) return 75;
        return 100;
    }
}
