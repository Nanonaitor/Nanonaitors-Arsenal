package com.nanonaitor.arsenal.test;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.compat.LootAlternativesModifier;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModItems;
import com.nanonaitor.arsenal.registry.ModRecipes;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.*;

/** 1.12.2 features restored in this branch: loot alternatives, recycling, heavy weapon recipes. */
@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortCompatGameTests {
    /** Reads one of this mod's built-in recipe files, as the datapack ships it. */
    private static JsonObject recipeJson(String name) {
        var in=ArsenalMod.class.getResourceAsStream("/data/"+ArsenalMod.MOD_ID+"/recipes/"+name+".json");
        if(in==null)return null;
        try(var reader=new java.io.InputStreamReader(in)){return JsonParser.parseReader(reader).getAsJsonObject();}
        catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    /** Every crafting form of a recipe file: the plain recipe, or each forge:conditional alternative. */
    private static java.util.List<JsonObject> forms(JsonObject json) {
        var out=new java.util.ArrayList<JsonObject>();
        if(json.get("type").getAsString().equals("forge:conditional"))
            for(var e:json.getAsJsonArray("recipes"))out.add(e.getAsJsonObject().getAsJsonObject("recipe"));
        else out.add(json);
        return out;
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void lootAlternativesRegistered(GameTestHelper h) {
        // Runs the loaded global loot modifiers the way a chest roll does; at the default 25%
        // chance an iron sword stays plain in all 100 rolls with odds of about 3 in 10^13.
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(net.minecraft.core.BlockPos.ZERO)))
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).withOptionalRandomSeed(42L).create(null);
        boolean swapped=false;
        for(int i=0;i<100 && !swapped;i++){
            var loot=net.minecraftforge.common.ForgeHooks.modifyLoot(new ResourceLocation("minecraft","chests/simple_dungeon"),
                new ObjectArrayList<>(java.util.List.of(new ItemStack(Items.IRON_SWORD))),context);
            swapped=loot.get(0).getItem() instanceof ArsenalWeaponItem;
        }
        h.assertTrue(swapped,"Global loot modifier loaded from data/forge/loot_modifiers swaps chest weapons");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void lootSwapsOnlyPlainWeapons(GameTestHelper h) {
        var random=RandomSource.create(7);
        ItemStack worn=new ItemStack(Items.IRON_SWORD);worn.setDamageValue(125);
        worn.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,2);
        ItemStack named=new ItemStack(Items.DIAMOND_SWORD).setHoverName(net.minecraft.network.chat.Component.literal("Legend"));
        var loot=new ObjectArrayList<ItemStack>(java.util.List.of(worn.copy(),named.copy(),new ItemStack(Items.BREAD),new ItemStack(Items.GOLDEN_AXE)));
        LootAlternativesModifier.replace(loot,random,1.0);
        h.assertTrue(loot.get(0).getItem() instanceof ArsenalWeaponItem w && w.tier()==WeaponTier.IRON,"Iron sword becomes an iron Arsenal weapon");
        h.assertTrue(loot.get(0).getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)==2,"Enchantments carried over");
        float wear=(float)loot.get(0).getDamageValue()/loot.get(0).getMaxDamage();
        h.assertTrue(Math.abs(wear-125F/Items.IRON_SWORD.getMaxDamage(ItemStack.EMPTY))<.02F,"Proportional wear carried over");
        h.assertTrue(loot.get(1).is(Items.DIAMOND_SWORD),"Named loot (a pack legendary) is kept");
        h.assertTrue(loot.get(2).is(Items.BREAD),"Non-weapons are kept");
        h.assertTrue(loot.get(3).getItem() instanceof ArsenalWeaponItem g && g.tier()==WeaponTier.GOLD,"Golden axe becomes gold");
        var none=new ObjectArrayList<ItemStack>(java.util.List.of(new ItemStack(Items.IRON_SWORD)));
        LootAlternativesModifier.replace(none,random,0.0);
        h.assertTrue(none.get(0).is(Items.IRON_SWORD),"Chance 0 never swaps");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void heavyWeaponsUseTheirOwnMaterial(GameTestHelper h) {
        // Before: silver, bronze, steel, umbrium, dragonbone, myrmex and living rams and balls
        // all used an iron block, the same grid as the iron ones.
        for(var tier:WeaponTier.activeValues()) {
            if(tier.custom() || tier==WeaponTier.IRON)continue;
            for(String kind:new String[]{"battering_ram","ball_and_chain"}) {
                var json=recipeJson(kind+"_"+tier.id);
                if(json==null)continue;
                for(var form:forms(json)) {
                    if(!form.has("key"))continue;
                    for(var entry:form.getAsJsonObject("key").entrySet()) {
                        var value=entry.getValue().getAsJsonObject();
                        h.assertTrue(!value.has("item") || !value.get("item").getAsString().equals("minecraft:iron_block"),
                            kind+"_"+tier.id+" must not share the iron recipe");
                    }
                }
            }
        }
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void everyMetalWeaponRecycles(GameTestHelper h) {
        String[] tiers={"copper","gold","iron","diamond","netherite","silver","bronze","steel","umbrium"};
        for(String tier:tiers)for(var kind:WeaponKind.values()) {
            if(kind==WeaponKind.LINKED_CLAWS)continue;
            // Copper and diamond have no vanilla nugget, so their flails had no recycling in 1.12.2 either.
            if(kind==WeaponKind.FLAIL && (tier.equals("copper")||tier.equals("diamond")))continue;
            h.assertTrue(recipeJson("recycling_"+kind.id+"_"+tier)!=null,"Recycling for "+kind.id+"_"+tier);
        }
        // A vanilla-tier Blade Staff smelts in the real furnace recipe list.
        var container=new net.minecraft.world.SimpleContainer(new ItemStack(ModItems.get(WeaponKind.BLADE_STAFF,WeaponTier.IRON).get()));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,container,h.getLevel());
        h.assertTrue(recipe.isPresent() && recipe.get().assemble(container,h.getLevel().registryAccess()).is(Items.IRON_INGOT),"Iron Blade Staff smelts to an iron ingot");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void tagSmeltingResolvesFirstTagItem(GameTestHelper h) {
        var json=JsonParser.parseString("{\"type\":\"nanonaitors_arsenal:tag_smelting\",\"ingredient\":{\"item\":\"minecraft:stick\"},"
            +"\"result\":{\"tag\":\"minecraft:planks\",\"count\":3},\"experience\":0.1,\"cookingtime\":200}").getAsJsonObject();
        var recipe=ModRecipes.TAG_SMELTING.get().fromJson(new ResourceLocation(ArsenalMod.MOD_ID,"test_tag_smelting"),json);
        var expected=net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(net.minecraft.tags.ItemTags.PLANKS).get().get(0).value();
        var result=recipe.getResultItem(h.getLevel().registryAccess());
        h.assertTrue(result.is(expected) && result.getCount()==3,"First item of the tag, with the count");
        // Clients receive a plain smelting recipe with the resolved item.
        var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        ModRecipes.TAG_SMELTING.get().toNetwork(buf,recipe);
        var client=ModRecipes.TAG_SMELTING.get().fromNetwork(recipe.getId(),buf);
        h.assertTrue(client.getResultItem(h.getLevel().registryAccess()).is(expected) && client.getCookingTime()==200,"Network form keeps the result");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void jeiHidesWhatPlayersCannotGet(GameTestHelper h) {
        var hidden=com.nanonaitor.arsenal.compat.jei.JeiHiding.hiddenStacks();
        java.util.function.Predicate<net.minecraft.world.item.Item> isHidden=item->hidden.stream().anyMatch(s->s.is(item));
        h.assertTrue(isHidden.test(ModItems.get(WeaponKind.LINKED_CLAWS,WeaponTier.IRON).get()),"Linked Claws are hidden");
        h.assertTrue(!isHidden.test(ModItems.get(WeaponKind.CLAWS,WeaponTier.IRON).get()),"Claws stay visible");
        h.assertTrue(!isHidden.test(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.NETHERITE).get()),"Vanilla tiers stay visible");
        // Tiers whose material no installed mod provides cannot be crafted (silver, steel... in a bare test world).
        for(var tier:WeaponTier.activeValues())
            h.assertTrue(tier.available()!=isHidden.test(ModItems.get(WeaponKind.SCIMITAR,tier).get()),"Hidden exactly when unavailable: "+tier);
        h.assertTrue(java.util.Arrays.stream(WeaponTier.values()).anyMatch(t->!t.available()),"Test world has an unavailable tier to check");
        h.assertTrue(hidden.stream().noneMatch(s->s.is(Items.ENCHANTED_BOOK)),"Enabled enchantments keep their books");
        h.succeed();
    }
}
