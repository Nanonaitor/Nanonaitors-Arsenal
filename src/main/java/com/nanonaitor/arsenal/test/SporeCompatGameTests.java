package com.nanonaitor.arsenal.test;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.combat.TierEffects;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Living and Sentient weapons with Fungal Infection: Spore (mod id "spore") instead of
 * Scape and Run: Parasites. Only meaningful in a -PdevMods=spore run; each test passes
 * trivially when Spore is absent.
 */
@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SporeCompatGameTests {
    private static boolean spore(){return net.minecraftforge.fml.ModList.get().isLoaded("spore");}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SporeCompat"));
    }
    private static LivingEntity sporeMob(GameTestHelper h){
        var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("spore","inf_pig"));
        var mob=(net.minecraft.world.entity.Mob)h.spawn(type,1,2,2);mob.setNoAi(true);
        return mob;
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void sporeMobsAreParasites(GameTestHelper h){
        if(!spore()){h.succeed();return;}
        h.assertTrue(TierEffects.isParasite(sporeMob(h)),"Spore mobs count as parasites");
        h.assertTrue(!TierEffects.isParasite(h.spawn(EntityType.HUSK,1,2,1)),"Vanilla mobs do not");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void livingTierAvailableFromSymbioticCore(GameTestHelper h){
        if(!spore()){h.succeed();return;}
        h.assertTrue(WeaponTier.LIVING.available(),"The Symbiotic Core makes the Living tier available");
        var recipe=h.getLevel().getRecipeManager().byKey(new ResourceLocation(ArsenalMod.MOD_ID,"scimitar_living_symbiotic_core"));
        h.assertTrue(recipe.isPresent(),"Diamond scimitar + Symbiotic Core recipe loads");
        // The upgrade keeps the weapon's enchantments, as Spore's own Living upgrade does.
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);
        var diamond=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.DIAMOND).get());
        diamond.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);
        grid.setItem(0,diamond);
        grid.setItem(1,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("spore","symbiotic_core"))));
        var match=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel());
        h.assertTrue(match.isPresent(),"Grid matches a recipe");
        var result=match.get().assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(result.getItem() instanceof ArsenalWeaponItem w && w.tier()==WeaponTier.LIVING && w.kind()==WeaponKind.SCIMITAR,"Makes the Living scimitar");
        h.assertTrue(result.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)==3,"Keeps enchantments");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge")
    public static void livingClawsInflictHemorrhage(GameTestHelper h){
        if(!spore()){h.succeed();return;}
        var p=player(h);var claws=new ItemStack(ModItems.get(WeaponKind.CLAWS,WeaponTier.LIVING).get());
        p.setItemSlot(EquipmentSlot.MAINHAND,claws);
        var target=h.spawn(EntityType.HUSK,1,2,2);target.setNoAi(true);
        TierEffects.hurtWithStack(claws,target,p.damageSources().playerAttack(p),4);
        var hemorrhage=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("spore","hemorrhage"));
        h.assertTrue(hemorrhage!=null && target.hasEffect(hemorrhage),"Living Claws inflict Spore's Hemorrhage");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=40)
    public static void livingWeaponEvolvesOnSporeKills(GameTestHelper h){
        if(!spore()){h.succeed();return;}
        var p=player(h);var living=new ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.LIVING).get());
        // One kill short of the default 1000 slain max health.
        living.getOrCreateTag().putInt("srpkills",999);
        p.setItemSlot(EquipmentSlot.MAINHAND,living);
        var mob=sporeMob(h);mob.setHealth(1);
        mob.hurt(p.damageSources().playerAttack(p),1000);
        h.runAfterDelay(2,()->{
            h.assertTrue(p.getMainHandItem().getItem() instanceof ArsenalWeaponItem w && w.tier()==WeaponTier.SENTIENT,
                "Killing a Spore mob evolves the Living scimitar into Sentient");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            h.succeed();
        });
    }
}
