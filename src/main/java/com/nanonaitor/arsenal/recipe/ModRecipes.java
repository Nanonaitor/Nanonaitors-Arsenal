package com.nanonaitor.arsenal.recipe;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ModRecipes {
    private ModRecipes() {}
    @SubscribeEvent public static void register(RegistryEvent.Register<IRecipe> event) {
        WeaponTier[] base = {WeaponTier.SILVER, WeaponTier.BRONZE, WeaponTier.STEEL,
            WeaponTier.UMBRIUM, WeaponTier.DRAGONBONE, WeaponTier.DESERT_MYRMEX,
            WeaponTier.JUNGLE_MYRMEX};
        for (WeaponTier tier : base) registerFamily(event, tier, false);
        registerFamily(event, WeaponTier.DESERT_VENOM, true);
        registerFamily(event, WeaponTier.JUNGLE_VENOM, true);
        registerLiving(event);
        if (!ArsenalCompatManager.hasDragonForge()) {
            upgrades(event, WeaponTier.FLAMED_DRAGONBONE, "iceandfire:fire_dragon_blood");
            upgrades(event, WeaponTier.ICED_DRAGONBONE, "iceandfire:ice_dragon_blood");
            upgrades(event, WeaponTier.ELECTRIC_DRAGONBONE, "iceandfire:lightning_dragon_blood");
        }
    }
    private static void registerFamily(RegistryEvent.Register<IRecipe> e, WeaponTier t, boolean venom) {
        if (!ArsenalCompatManager.isTierAvailable(t)) return;
        Object m = ingredient(t.getRepairIngredient());
        if (m == null) return;
        Object v = venom && ArsenalCompatManager.hasItem("iceandfire:myrmex_stinger")
            ? ArsenalCompatManager.itemStack("iceandfire:myrmex_stinger") : m;
        recipe(e, "morning_star", t, ModContent.MORNING_STARS,
            venom ? " V " : " M ", venom ? "VMV" : "MMM", " S ",
            'M', m, 'V', v);
        recipe(e, "scimitar", t, ModContent.SCIMITARS, venom?" VM":" MM", "MM ", "S  ", 'M', m, 'V', v);
        recipe(e, "claws", t, ModContent.CLAWS, venom?"V V":"M M", "MWM", " S ", 'M', m, 'V', v, 'W', "plankWood");
        recipe(e, "flail", t, ModContent.FLAILS, venom?"VM ":"MM ",
            " C ", " S ", 'M', m, 'V', v,
            'C', "chainIron");
        Object heavyMaterial = t == WeaponTier.BRONZE ? m : blockIngredient(t);
        if (heavyMaterial != null) {
            recipe(e, "battering_ram", t, ModContent.BATTERING_RAMS, venom?" VM":"  M", "LLL", " S ", 'M', heavyMaterial, 'V', v, 'L', "logWood");
        }
        if (heavyMaterial != null) {
            recipe(e, "ball_and_chain", t, ModContent.BALLS_AND_CHAINS,
                "  B", " C ", "C  ", 'B', heavyMaterial,
                'C', "chainIron");
        }
    }
    private static Object blockIngredient(WeaponTier t) {
        String id = t.getId();
        if (id.contains("dragonbone")) {
            ItemStack dragonboneBlock = ArsenalCompatManager.itemStack("iceandfire:dragon_bone_block");
            return dragonboneBlock.isEmpty() ? null : dragonboneBlock;
        }
        if (id.contains("desert_venom")) {
            ItemStack cocoon = ArsenalCompatManager.itemStack("iceandfire:desert_myrmex_cocoon");
            return cocoon.isEmpty() ? null : cocoon;
        }
        if (id.contains("jungle_venom")) {
            ItemStack cocoon = ArsenalCompatManager.itemStack("iceandfire:jungle_myrmex_cocoon");
            return cocoon.isEmpty() ? null : cocoon;
        }
        if (id.contains("desert_myrmex")) {
            ItemStack resin = ArsenalCompatManager.itemStack(
                "iceandfire:myrmex_desert_resin_block");
            return resin.isEmpty() ? null : resin;
        }
        if (id.contains("jungle_myrmex")) {
            ItemStack resin = ArsenalCompatManager.itemStack(
                "iceandfire:myrmex_jungle_resin_block");
            return resin.isEmpty() ? null : resin;
        }
        String ore = "block" + Character.toUpperCase(id.charAt(0)) + id.substring(1);
        return ArsenalCompatManager.hasOre(ore) ? ore : null;
    }
    private static void registerLiving(RegistryEvent.Register<IRecipe> e) {
        WeaponTier t = WeaponTier.LIVING;
        if (!ArsenalCompatManager.isTierAvailable(t)
            || !ArsenalCompatManager.hasItem("srparasites:infectious_blade_fragment")
            || !ArsenalCompatManager.hasItem("srparasites:hardened_bone_handle")
            || !ArsenalCompatManager.hasItem("srparasites:dried_tendons")
            || !ArsenalCompatManager.hasItem("srparasites:vile_shell")) return;
        Object f = ArsenalCompatManager.itemStack("srparasites:infectious_blade_fragment");
        Object c = ArsenalCompatManager.itemStack("srparasites:living_core");
        Object h = ArsenalCompatManager.itemStack("srparasites:hardened_bone_handle");
        Object tendon = ArsenalCompatManager.itemStack("srparasites:dried_tendons");
        Object shell = ArsenalCompatManager.itemStack("srparasites:vile_shell");
        living(e, "morning_star", ModContent.MORNING_STARS.get(t), " F ", "FCF", " H ", f,c,h);
        living(e, "scimitar", ModContent.SCIMITARS.get(t), " FF", "FC ", "H  ", f,c,h);
        living(e, "claws", ModContent.CLAWS.get(t), "F F", "FCF", " H ", f,c,h);
        living(e, "flail", ModContent.FLAILS.get(t), "FC ", " T ", " H ", f,c,h, 'T',tendon);
        living(e, "battering_ram", ModContent.BATTERING_RAMS.get(t), "  C", "VVV", " H ", f,c,h, 'V',shell);
        ItemStack bolsterHusk = ArsenalCompatManager.itemStack(
            "srparasites:ada_bolster_drop");
        if (!bolsterHusk.isEmpty() && c instanceof ItemStack
            && !((ItemStack) c).isEmpty()) {
            recipe(e, "ball_and_chain", t, ModContent.BALLS_AND_CHAINS,
                " HC", " TH", "T  ", 'C', c, 'H', bolsterHusk,
                'T', tendon);
        }
    }
    private static void living(RegistryEvent.Register<IRecipe> e, String family,
                               ItemArsenalWeapon out, String a,String b,String c,
                               Object f,Object core,Object handle,Object... extraKeys) {
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add(a); args.add(b); args.add(c);
        addLivingIngredient(args, a,b,c, 'F',f);
        addLivingIngredient(args, a,b,c, 'C',core);
        addLivingIngredient(args, a,b,c, 'H',handle);
        for (int i=0; i<extraKeys.length; i+=2) {
            addLivingIngredient(args, a,b,c, (Character)extraKeys[i], extraKeys[i+1]);
        }
        ShapedOreRecipe r = new ShapedOreRecipe(new ResourceLocation(NanonaitorsArsenal.MOD_ID,"compat"),
            new ItemStack(out), args.toArray());
        r.setRegistryName(NanonaitorsArsenal.MOD_ID, family + "_living"); e.getRegistry().register(r);
    }
    private static void addLivingIngredient(java.util.List<Object> args,
                                            String a,String b,String c,char symbol,Object ingredient) {
        if (a.indexOf(symbol)<0 && b.indexOf(symbol)<0 && c.indexOf(symbol)<0) return;
        args.add(symbol);
        args.add(ingredient);
    }
    private static void upgrades(RegistryEvent.Register<IRecipe> e, WeaponTier target, String blood) {
        if (!ArsenalCompatManager.isTierAvailable(target)) return;
        upgrade(e,"morning_star",ModContent.MORNING_STARS,target,blood);
        upgrade(e,"scimitar",ModContent.SCIMITARS,target,blood);
        upgrade(e,"claws",ModContent.CLAWS,target,blood);
        upgrade(e,"flail",ModContent.FLAILS,target,blood);
        upgrade(e,"battering_ram",ModContent.BATTERING_RAMS,target,blood);
        upgrade(e,"ball_and_chain",ModContent.BALLS_AND_CHAINS,target,blood);
    }
    private static <T extends ItemArsenalWeapon> void upgrade(RegistryEvent.Register<IRecipe> e,
            String family, Map<WeaponTier,T> map, WeaponTier target, String blood) {
        ArsenalUpgradeRecipe r = new ArsenalUpgradeRecipe(map.get(WeaponTier.DRAGONBONE), map.get(target), blood);
        r.setRegistryName(NanonaitorsArsenal.MOD_ID, family + "_" + target.getId()); e.getRegistry().register(r);
    }
    private static <T extends ItemArsenalWeapon> void recipe(RegistryEvent.Register<IRecipe> e,
            String family, WeaponTier t, Map<WeaponTier,T> map, String a,String b,String c,Object... keys) {
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add(a);args.add(b);args.add(c);
        for (int i=0;i<keys.length;i+=2) {
            char key=(Character)keys[i]; if (a.indexOf(key)<0 && b.indexOf(key)<0 && c.indexOf(key)<0) continue;
            args.add(key);args.add(keys[i+1]);
        }
        if (a.indexOf('S') >= 0 || b.indexOf('S') >= 0 || c.indexOf('S') >= 0) {
            args.add('S');args.add(handleIngredient(t));
        }
        ShapedOreRecipe r = new ShapedOreRecipe(new ResourceLocation(NanonaitorsArsenal.MOD_ID,"compat"),
            new ItemStack(map.get(t)), args.toArray());
        r.setRegistryName(NanonaitorsArsenal.MOD_ID, family + "_" + t.getId()); e.getRegistry().register(r);
    }
    private static Object handleIngredient(WeaponTier tier) {
        if (tier.isMyrmex()) {
            ItemStack witherbone = ArsenalCompatManager.itemStack(
                "spartanfire:witherbone_handle");
            if (!witherbone.isEmpty()) return witherbone;
        }
        return "handleWeapon";
    }
    private static Object ingredient(String descriptor) {
        if (descriptor.startsWith("ore:")) return descriptor.substring(4);
        ItemStack stack=ArsenalCompatManager.itemStack(descriptor); return stack.isEmpty()?null:stack;
    }
}
