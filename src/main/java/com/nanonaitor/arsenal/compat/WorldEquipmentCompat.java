package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/** Safe modern replacement for the old spawn-equipment reflection hook. */
public final class WorldEquipmentCompat {
    private static final Set<String> HUMANOIDS=Set.of("zombie","husk","zombie_villager","zombified_piglin","skeleton","stray","wither_skeleton","vindicator");
    private static final List<WeaponKind> PRIMARY=List.of(WeaponKind.MORNING_STAR,WeaponKind.SCIMITAR,
        WeaponKind.BLADE_STAFF,WeaponKind.CLAWS,WeaponKind.FLAIL,WeaponKind.BATTERING_RAM,WeaponKind.BALL_AND_CHAIN);

    public static void onEntityJoin(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide || !ArsenalConfig.MOB_EQUIPMENT.get() || !(event.getEntity() instanceof Mob mob))return;
        ResourceLocation entityId=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if(!"minecraft".equals(entityId.getNamespace()) || !HUMANOIDS.contains(entityId.getPath()))return;
        ItemStack held=mob.getMainHandItem();
        // Named mobs and designed gear (names, lore, attributes, unbreakable, other data) keep their weapons.
        if(held.isEmpty() || mob.hasCustomName() || isSpecialStack(held))return;
        WeaponTier tier=tierFor(held);
        if(tier==null || mob.getRandom().nextDouble()>=ArsenalConfig.MOB_REPLACEMENT_CHANCE.get())return;
        List<ItemStack> choices=new ArrayList<>();
        for(WeaponKind kind:PRIMARY)if(ArsenalConfig.WEAPONS.get(kind.id).get())choices.add(new ItemStack(ModItems.get(kind,tier).get()));
        if(choices.isEmpty())return;
        ItemStack replacement=choices.get(mob.getRandom().nextInt(choices.size()));
        if(held.hasTag())replacement.setTag(held.getTag().copy());
        if(replacement.isDamageableItem() && held.isDamageableItem())replacement.setDamageValue(Math.min(replacement.getMaxDamage()-1,
            Math.round((float)held.getDamageValue()/Math.max(1,held.getMaxDamage())*replacement.getMaxDamage())));
        mob.setItemSlot(EquipmentSlot.MAINHAND,replacement);
    }

    /**
     * True for gear that was given an identity: a name or lore, attribute
     * modifiers, unbreakable, or any data beyond enchantments, wear and repair cost.
     * Such items are a modpack's or a player's designed gear and are never replaced.
     */
    public static boolean isSpecialStack(ItemStack stack){
        var tag=stack.getTag();
        if(tag==null)return false;
        for(String key:tag.getAllKeys())
            if(!key.equals("Enchantments") && !key.equals("RepairCost") && !key.equals("Damage"))return true;
        return false;
    }

    private static WeaponTier tierFor(ItemStack stack){
        if(!(stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem))return null;
        String path=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for(WeaponTier tier:WeaponTier.activeValues())if(!tier.custom() && path.startsWith(tier.id+"_") || path.startsWith((tier==WeaponTier.WOOD?"wooden":tier==WeaponTier.GOLD?"golden":tier.id)+"_"))return tier;
        return null;
    }
    private WorldEquipmentCompat(){}
}
