package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemLinkedClaw;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.potion.PotionArmorFracture;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
    public static PotionArmorFracture ARMOR_FRACTURE;

    private ModContent() {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (WeaponTier tier : WeaponTier.values()) {
            ItemMorningStar morningStar = new ItemMorningStar(tier);
            ItemScimitar scimitar = new ItemScimitar(tier);
            ItemClaws claws = new ItemClaws(tier);
            ItemFlail flail = new ItemFlail(tier);
            ItemBatteringRam batteringRam = new ItemBatteringRam(tier);
            ItemBallAndChain ballAndChain = new ItemBallAndChain(tier);
            ItemLinkedClaw linkedClaw = new ItemLinkedClaw(tier);
            MORNING_STARS.put(tier, morningStar);
            SCIMITARS.put(tier, scimitar);
            CLAWS.put(tier, claws);
            FLAILS.put(tier, flail);
            BATTERING_RAMS.put(tier, batteringRam);
            BALLS_AND_CHAINS.put(tier, ballAndChain);
            LINKED_CLAWS.put(tier, linkedClaw);
            event.getRegistry().registerAll(morningStar, scimitar, claws, flail,
                batteringRam, ballAndChain, linkedClaw);
        }
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        ARMOR_FRACTURE = new PotionArmorFracture();
        event.getRegistry().register(ARMOR_FRACTURE);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
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
