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
    BRONZE("bronze", material("ARSENAL_BRONZE", 2, 350, 6F, 2F, 12), 4,
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
        Family.SRP, "srparasites:infectious_blade_fragment", 3),

    // Player-defined materials (config: customMaterials). Unused slots register nothing.
    CUSTOM_1("custom_1", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2),
    CUSTOM_2("custom_2", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2),
    CUSTOM_3("custom_3", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2),
    CUSTOM_4("custom_4", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2),
    CUSTOM_5("custom_5", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2),
    CUSTOM_6("custom_6", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2),
    CUSTOM_7("custom_7", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2),
    CUSTOM_8("custom_8", Item.ToolMaterial.IRON, 4, Family.CUSTOM, "ore:ingotIron", 2);

    public enum Family { VANILLA, SPARTAN, DEFILED, ICE_AND_FIRE, FIRE_DRAGONBONE,
        ICE_DRAGONBONE, LIGHTNING_DRAGONBONE, DESERT_MYRMEX, JUNGLE_MYRMEX, SRP, CUSTOM }

    // Built-in tiers never change these; a custom slot takes them from the config.
    private String id;
    private Item.ToolMaterial material;
    private int morningStarFractureCap;
    private final Family family;
    private String repairIngredient;
    private int ramBreakLevel;
    private boolean configured;
    private String displayName;
    private int color = 0xFFFFFF;
    private int harvestLevel;

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
    public boolean isCustom() { return family == Family.CUSTOM; }
    /** Built-in tiers always; a custom slot only once a config entry fills it. */
    public boolean isActive() { return !isCustom() || configured; }
    /** Custom tiers: the name shown in place of "Iron" (e.g. "Copper"). */
    public String getDisplayName() { return displayName; }
    /** Custom tiers: the RGB tint for the weapon's metal parts. */
    public int getColor() { return color; }

    /**
     * Fills the custom slots from the config, once, before items are registered.
     * Entry: id, Display Name, #RRGGBB, repair ingredient, harvest level,
     * durability, attack damage, enchantability.
     */
    public static synchronized void configureCustom(String[] entries) {
        if (customConfigured) return;
        customConfigured = true;
        java.util.Set<String> taken = new java.util.HashSet<>();
        for (WeaponTier tier : values()) if (!tier.isCustom()) taken.add(tier.id);
        int slot = 0;
        WeaponTier[] slots = {CUSTOM_1, CUSTOM_2, CUSTOM_3, CUSTOM_4, CUSTOM_5, CUSTOM_6, CUSTOM_7, CUSTOM_8};
        for (String entry : entries == null ? new String[0] : entries) {
            if (entry == null || entry.trim().isEmpty() || entry.trim().startsWith("#")) continue;
            if (slot >= slots.length) {
                com.nanonaitor.arsenal.NanonaitorsArsenal.LOGGER.warn("Only {} custom materials are supported; ignoring '{}'", slots.length, entry);
                continue;
            }
            try {
                CustomSpec spec = CustomSpec.parse(entry);
                if (!taken.add(spec.id)) throw new IllegalArgumentException("id '" + spec.id + "' is already used");
                WeaponTier tier = slots[slot++];
                tier.id = spec.id;
                tier.displayName = spec.displayName;
                tier.color = spec.color;
                tier.repairIngredient = spec.repair;
                tier.harvestLevel = spec.harvest;
                tier.ramBreakLevel = Math.min(3, spec.harvest);
                tier.morningStarFractureCap = Math.min(5, 2 + spec.harvest);
                tier.material = EnumHelper.addToolMaterial("ARSENAL_CUSTOM_" + spec.id.toUpperCase(java.util.Locale.ROOT),
                    spec.harvest, spec.durability, 2.0F + 2.0F * spec.harvest, spec.damage, spec.enchantability);
                tier.configured = true;
            } catch (IllegalArgumentException bad) {
                com.nanonaitor.arsenal.NanonaitorsArsenal.LOGGER.warn("Skipping custom material '{}': {}", entry, bad.getMessage());
            }
        }
    }

    private static boolean customConfigured;

    /** One customMaterials entry, validated. */
    public static final class CustomSpec {
        public final String id, displayName, repair;
        public final int color, harvest, durability, enchantability;
        public final float damage;

        private CustomSpec(String id, String displayName, int color, String repair, int harvest,
                           int durability, float damage, int enchantability) {
            this.id = id; this.displayName = displayName; this.color = color; this.repair = repair;
            this.harvest = harvest; this.durability = durability; this.damage = damage;
            this.enchantability = enchantability;
        }

        public static CustomSpec parse(String entry) {
            String[] p = entry.split(",");
            if (p.length != 8) throw new IllegalArgumentException("needs 8 comma-separated values, got " + p.length);
            for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
            if (!p[0].matches("[a-z0-9_]{1,24}")) throw new IllegalArgumentException("id must be 1-24 of a-z, 0-9, _");
            if (p[1].isEmpty()) throw new IllegalArgumentException("display name is empty");
            String hex = p[2].startsWith("#") ? p[2].substring(1) : p[2];
            if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("color must be #RRGGBB");
            if (!p[3].startsWith("ore:") && !p[3].contains(":")) throw new IllegalArgumentException("repair ingredient must be ore:name or modid:item");
            try {
                int harvest = Integer.parseInt(p[4]);
                int durability = Integer.parseInt(p[5]);
                float damage = Float.parseFloat(p[6]);
                int enchantability = Integer.parseInt(p[7]);
                if (harvest < 0 || harvest > 10) throw new IllegalArgumentException("harvest level must be 0-10");
                if (durability < 1 || durability > 100000) throw new IllegalArgumentException("durability must be 1-100000");
                if (!(damage >= 0.0F && damage <= 100.0F)) throw new IllegalArgumentException("attack damage must be 0-100");
                if (enchantability < 0 || enchantability > 100) throw new IllegalArgumentException("enchantability must be 0-100");
                return new CustomSpec(p[0], p[1], Integer.parseInt(hex, 16), p[3], harvest, durability, damage, enchantability);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("harvest, durability, damage and enchantability must be numbers");
            }
        }
    }
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
    public boolean isOrganic() { return family == Family.ICE_AND_FIRE
        || family == Family.FIRE_DRAGONBONE || family == Family.ICE_DRAGONBONE
        || family == Family.LIGHTNING_DRAGONBONE || family == Family.DESERT_MYRMEX
        || family == Family.JUNGLE_MYRMEX || family == Family.SRP; }
    public int getArmorPiercePercent() {
        if (isCustom()) return harvestLevel <= 0 ? 25 : harvestLevel == 1 ? 50 : harvestLevel == 2 ? 75 : 100;
        if (this == WOOD) return 25;
        if (this == STONE || this == GOLD || this == BRONZE) return 50;
        if (this == IRON || this == SILVER || this == STEEL || this == UMBRIUM) return 75;
        return 100;
    }
    public float getReflectionMultiplier() {
        if (isCustom()) {
            return harvestLevel <= 0 ? 0.0F : harvestLevel == 1 ? 0.10F : harvestLevel == 2 ? 0.50F
                : harvestLevel == 3 ? 0.75F : 1.0F;
        }
        switch (this) {
            case WOOD: return 0.0F;
            case STONE: return 0.10F;
            case BRONZE: return 0.25F;
            case IRON: case GOLD: case SILVER: case STEEL: case UMBRIUM: return 0.50F;
            case DIAMOND: case DRAGONBONE: return 0.75F;
            default: return 1.0F;
        }
    }
}
