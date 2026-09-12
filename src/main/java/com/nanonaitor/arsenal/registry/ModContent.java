package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.block.BlockIronChain;
import com.nanonaitor.arsenal.client.ShieldItemStackRenderer;
import com.nanonaitor.arsenal.enchantment.EnchantmentLongChain;
import com.nanonaitor.arsenal.enchantment.EnchantmentRotationForce;
import com.nanonaitor.arsenal.enchantment.EnchantmentRecovery;
import com.nanonaitor.arsenal.enchantment.EnchantmentBreeched;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemLinkedClaw;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import com.nanonaitor.arsenal.item.ItemTartsyShield;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.potion.PotionArmorFracture;
import com.nanonaitor.arsenal.potion.PotionStunned;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ModContent {
    public static final Map<WeaponTier, ItemMorningStar> MORNING_STARS =
        new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, ItemScimitar> SCIMITARS =
        new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, ItemClaws> CLAWS =
        new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, ItemFlail> FLAILS =
        new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, ItemBatteringRam> BATTERING_RAMS =
        new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, ItemBallAndChain> BALLS_AND_CHAINS =
        new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, ItemLinkedClaw> LINKED_CLAWS =
        new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, ItemDoubleBladedScimitar> DOUBLE_BLADED_SCIMITARS =
        new EnumMap<>(WeaponTier.class);
    public static PotionArmorFracture ARMOR_FRACTURE;
    public static PotionStunned STUNNED;
    public static EnchantmentLongChain LONG_CHAIN;
    public static EnchantmentRotationForce ROTATION_FORCE;
    public static EnchantmentRecovery RECOVERY;
    public static EnchantmentBreeched BREECHED;
    public static ItemSunWarBulwark SUN_WAR_BULWARK;
    public static ItemTartsyShield TARTSY_SHIELD;
    public static final BlockIronChain IRON_CHAIN = new BlockIronChain();
    public static ItemBlock IRON_CHAIN_ITEM;

    private ModContent() {}

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(IRON_CHAIN);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IRON_CHAIN_ITEM = new ItemBlock(IRON_CHAIN);
        IRON_CHAIN_ITEM.setRegistryName(IRON_CHAIN.getRegistryName());
        // ItemBlock does not reliably inherit its block's tab early enough on
        // every 1.12.2 Forge setup. Set it explicitly so the standalone chain
        // is always available from Arsenal's creative tab.
        IRON_CHAIN_ITEM.setCreativeTab(NanonaitorsArsenal.CREATIVE_TAB);
        event.getRegistry().register(IRON_CHAIN_ITEM);
        registerChainOreEntries();
        registerHandleOreEntry();
        SUN_WAR_BULWARK = new ItemSunWarBulwark();
        TARTSY_SHIELD = new ItemTartsyShield();
        event.getRegistry().registerAll(SUN_WAR_BULWARK, TARTSY_SHIELD);
        for (WeaponTier tier : WeaponTier.values()) {
            ItemMorningStar morningStar = new ItemMorningStar(tier);
            ItemScimitar scimitar = new ItemScimitar(tier);
            ItemClaws claws = new ItemClaws(tier);
            ItemFlail flail = new ItemFlail(tier);
            ItemBatteringRam batteringRam = new ItemBatteringRam(tier);
            ItemBallAndChain ballAndChain = new ItemBallAndChain(tier);
            ItemLinkedClaw linkedClaw = new ItemLinkedClaw(tier);
            ItemDoubleBladedScimitar doubleBladedScimitar =
                new ItemDoubleBladedScimitar(tier);
            MORNING_STARS.put(tier, morningStar);
            SCIMITARS.put(tier, scimitar);
            CLAWS.put(tier, claws);
            FLAILS.put(tier, flail);
            BATTERING_RAMS.put(tier, batteringRam);
            BALLS_AND_CHAINS.put(tier, ballAndChain);
            LINKED_CLAWS.put(tier, linkedClaw);
            DOUBLE_BLADED_SCIMITARS.put(tier, doubleBladedScimitar);
            event.getRegistry().registerAll(morningStar, scimitar, claws, flail,
                batteringRam, ballAndChain, linkedClaw, doubleBladedScimitar);
        }
    }

    /**
     * Prefer Quark's chain while retaining Arsenal's standalone chain as the
     * recipe fallback. Quark's chain is registered first so recipe viewers
     * present it as the primary ingredient when Quark is installed.
     */
    private static void registerChainOreEntries() {
        Item external = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation("quark", "chain"));
        if (external != null) registerOreOnce("chainIron", new ItemStack(external));
        registerOreOnce("chainIron", new ItemStack(IRON_CHAIN_ITEM));
    }

    /**
     * Spartan Weaponry's Handle replaces the vanilla stick in Arsenal recipes
     * when available. Keeping the choice behind an ore key preserves fully
     * standalone recipes without making Spartan Weaponry a hard dependency.
     */
    private static void registerHandleOreEntry() {
        Item spartanHandle = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation("spartanweaponry", "material"));
        ItemStack handle = spartanHandle == null || spartanHandle == Items.AIR
            ? new ItemStack(Items.STICK)
            : new ItemStack(spartanHandle, 1, 0);
        registerOreOnce("handleWeapon", handle);
    }

    private static void registerOreOnce(String name, ItemStack candidate) {
        for (ItemStack existing : OreDictionary.getOres(name, false)) {
            if (OreDictionary.itemMatches(existing, candidate, false)) return;
        }
        OreDictionary.registerOre(name, candidate);
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        ARMOR_FRACTURE = new PotionArmorFracture();
        STUNNED = new PotionStunned();
        event.getRegistry().registerAll(ARMOR_FRACTURE, STUNNED);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (com.nanonaitor.arsenal.config.ArsenalConfig.enchantments.enableLongChain)
            event.getRegistry().register(LONG_CHAIN = new EnchantmentLongChain());
        if (com.nanonaitor.arsenal.config.ArsenalConfig.enchantments.enableRotationForce)
            event.getRegistry().register(ROTATION_FORCE = new EnchantmentRotationForce());
        if (com.nanonaitor.arsenal.config.ArsenalConfig.enchantments.enableRecovery)
            event.getRegistry().register(RECOVERY = new EnchantmentRecovery());
        if (com.nanonaitor.arsenal.config.ArsenalConfig.enchantments.enableBreeched)
            event.getRegistry().register(BREECHED = new EnchantmentBreeched());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        // Morning Star and selected Scimitar pixel art by Star Artsy, commissioned by Nanonaitor.
        ShieldItemStackRenderer shieldRenderer = new ShieldItemStackRenderer();
        SUN_WAR_BULWARK.setTileEntityItemStackRenderer(shieldRenderer);
        TARTSY_SHIELD.setTileEntityItemStackRenderer(shieldRenderer);
        registerModel(IRON_CHAIN_ITEM);
        registerModel(SUN_WAR_BULWARK);
        registerModel(TARTSY_SHIELD);
        for (ItemMorningStar item : MORNING_STARS.values()) {
            registerModel(item);
        }
        for (ItemScimitar item : SCIMITARS.values()) {
            registerModel(item);
        }
        for (ItemClaws item : CLAWS.values()) {
            registerModel(item);
        }
        for (ItemFlail item : FLAILS.values()) {
            registerModel(item);
        }
        for (ItemBatteringRam item : BATTERING_RAMS.values()) {
            registerModel(item);
        }
        for (ItemBallAndChain item : BALLS_AND_CHAINS.values()) {
            registerModel(item);
        }
        for (ItemDoubleBladedScimitar item : DOUBLE_BLADED_SCIMITARS.values()) {
            registerModel(item);
        }
        for (WeaponTier tier : WeaponTier.values()) {
            ItemLinkedClaw linked = LINKED_CLAWS.get(tier);
            if (linked != null) {
                ModelLoader.setCustomModelResourceLocation(linked, 0,
                    new ModelResourceLocation(NanonaitorsArsenal.MOD_ID
                        + ":claws_" + tier.getId(), "inventory"));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
