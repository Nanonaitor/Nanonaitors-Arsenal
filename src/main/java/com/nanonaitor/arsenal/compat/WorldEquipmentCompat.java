package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Optional integration: reuse existing material and generation conditions. */
@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID)
public final class WorldEquipmentCompat {
    private static final Set<EntityLiving> PENDING = Collections.newSetFromMap(new WeakHashMap<EntityLiving,Boolean>());
    private static boolean warned;
    private WorldEquipmentCompat() {}

    public static WeaponTier tierFor(String domain, String path) {
        if ("minecraft".equals(domain)) {
            if (!path.endsWith("_sword") && !path.endsWith("_axe")) return null;
            String material=path.substring(0,path.lastIndexOf('_'));
            if (material.equals("wooden")) material="wood";
            if (material.equals("golden")) material="gold";
            for (WeaponTier tier:WeaponTier.values()) if(tier.getId().equals(material)) return tier;
            return null;
        }
        if (!Arrays.asList("spartanweaponry","spartanfire","spartandefiled","spartandragonsteel").contains(domain)) return null;
        String[] types={"dagger","longsword","katana","saber","rapier","greatsword","hammer","warhammer","spear","halberd","pike","lance","battleaxe","mace","glaive","staff","quarterstaff","scythe"};
        String material=null;
        for(String type:types) if(path.startsWith(type+"_")){material=path.substring(type.length()+1);break;}
        if(material==null)return null;
        if(material.equals("fire_dragonbone"))material="flamed_dragonbone";
        if(material.equals("ice_dragonbone"))material="iced_dragonbone";
        if(material.equals("lightning_dragonbone"))material="electric_dragonbone";
        for(WeaponTier tier:WeaponTier.values())if(tier.getId().equals(material))return tier;
        return null;
    }
    private static WeaponTier tierFor(Item item) {
        ResourceLocation id=item.getRegistryName();
        if(id==null || !(item instanceof ItemSword || item instanceof ItemAxe))return null;
        return tierFor(id.getResourceDomain(),id.getResourcePath());
    }
    private static List<Item> alternatives(WeaponTier tier) {
        List<Item> items=new ArrayList<>();
        if(tier==null || !ArsenalCompatManager.isTierAvailable(tier))return items;
        ArsenalConfig.Weapons c=ArsenalConfig.weapons;
        if(c.morningStar)items.add(ModContent.MORNING_STARS.get(tier));
        if(c.scimitar)items.add(ModContent.SCIMITARS.get(tier));
        if(c.bladeStaff)items.add(ModContent.DOUBLE_BLADED_SCIMITARS.get(tier));
        if(c.claws)items.add(ModContent.CLAWS.get(tier));
        if(c.flail)items.add(ModContent.FLAILS.get(tier));
        if(c.batteringRam)items.add(ModContent.BATTERING_RAMS.get(tier));
        if(c.ballAndChain)items.add(ModContent.BALLS_AND_CHAINS.get(tier));
        items.removeIf(Objects::isNull);
        return items;
    }
    private static Object fieldByType(Class<?> owner,Class<?> type,Object instance)throws ReflectiveOperationException {
        for(Field field:owner.getDeclaredFields())if(field.getType()==type){field.setAccessible(true);return field.get(instance);}
        throw new NoSuchFieldException(owner.getName()+" / "+type.getName());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public static void loot(LootTableLoadEvent event) {
        if(!ArsenalConfig.worldEquipment.lootIntegration)return;
        try {
            List<LootPool> pools=(List<LootPool>)fieldByType(LootTable.class,List.class,event.getTable());
            Field entriesField=null;
            for(Field field:LootPool.class.getDeclaredFields()){
                if(field.getGenericType() instanceof java.lang.reflect.ParameterizedType &&
                    ((java.lang.reflect.ParameterizedType)field.getGenericType()).getActualTypeArguments()[0]==LootEntry.class){entriesField=field;entriesField.setAccessible(true);break;}
            }
            if(entriesField==null)throw new NoSuchFieldException("LootPool entries");
            for(LootPool pool:pools){
                List<LootEntry> entries=new ArrayList<>((List<LootEntry>)entriesField.get(pool));
                for(LootEntry entry:entries){
                    if(entry.getClass()!=LootEntryItem.class)continue;
                    Item source=(Item)fieldByType(LootEntryItem.class,Item.class,entry);
                    WeaponTier tier=tierFor(source);
                    if(tier==null || alternatives(tier).isEmpty())continue;
                    LootFunction[] functions=(LootFunction[])fieldByType(LootEntryItem.class,LootFunction[].class,entry);
                    LootCondition[] conditions=(LootCondition[])fieldByType(LootEntry.class,LootCondition[].class,entry);
                    LootEntry replacement=new AlternativeEntry((LootEntryItem)entry,source,tier,functions,conditions);
                    pool.removeEntry(entry.getEntryName());pool.addEntry(replacement);
                }
            }
        }catch(ReflectiveOperationException | RuntimeException ex){warn(ex);}
    }
    private static final class AlternativeEntry extends LootEntryItem {
        private final LootEntryItem original;
        private final WeaponTier tier;
        AlternativeEntry(LootEntryItem original,Item source,WeaponTier tier,LootFunction[] functions,LootCondition[] conditions){
            super(source,1,0,functions,conditions,original.getEntryName());this.original=original;this.tier=tier;
        }
        @Override public int getEffectiveWeight(float luck){return original.getEffectiveWeight(luck);}
        @Override public void addLoot(Collection<ItemStack> stacks,Random random,LootContext context){
            List<Item> choices=alternatives(tier);
            if(!choices.isEmpty() && random.nextDouble()<ArsenalConfig.worldEquipment.lootReplacementChance){
                // Use the real vanilla entry method: Socketed's existing mixin rolls once here.
                new LootEntryItem(choices.get(random.nextInt(choices.size())),1,0,functions,conditions,entryName).addLoot(stacks,random,context);
            }else original.addLoot(stacks,random,context);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void spawn(LivingSpawnEvent.SpecialSpawn event){
        if(!event.getWorld().isRemote && !event.isCanceled() && ArsenalConfig.worldEquipment.mobEquipment && event.getEntityLiving() instanceof EntityLiving)PENDING.add((EntityLiving)event.getEntityLiving());
    }
    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event){
        if(event.phase!=TickEvent.Phase.END || event.world.isRemote)return;
        Iterator<EntityLiving> iterator=PENDING.iterator();
        while(iterator.hasNext()){
            EntityLiving mob=iterator.next();if(mob.world!=event.world)continue;iterator.remove();
            if(mob.isDead || !ArsenalConfig.worldEquipment.mobEquipment)continue;
            ResourceLocation id=EntityList.getKey(mob);
            if(id==null || !id.getResourceDomain().equals("minecraft") || !Arrays.asList("zombie","husk","zombie_villager","zombie_pigman","skeleton","stray","wither_skeleton","vindication_illager").contains(id.getResourcePath()))continue;
            ItemStack held=mob.getHeldItemMainhand();
            if(held.isEmpty() || held.hasDisplayName())continue;
            List<Item> choices=alternatives(tierFor(held.getItem()));
            if(choices.isEmpty() || mob.getRNG().nextDouble()>=ArsenalConfig.worldEquipment.mobReplacementChance)continue;
            Item item=choices.get(mob.getRNG().nextInt(choices.size()));
            NBTTagCompound nbt=held.writeToNBT(new NBTTagCompound());nbt.setString("id",item.getRegistryName().toString());
            nbt.setShort("Damage",(short)Math.min(item.getMaxDamage()-1,Math.round((float)held.getItemDamage()/Math.max(1,held.getMaxDamage())*item.getMaxDamage())));
            ItemStack replacement=new ItemStack(nbt);
            // Socketed's API leaves existing sockets untouched, including copied capabilities.
            if(ArsenalConfig.worldEquipment.socketMobEquipment)rollSockets(replacement);
            mob.setItemStackToSlot(EntityEquipmentSlot.MAINHAND,replacement);
        }
    }
    private static void rollSockets(ItemStack stack){
        if(!Loader.isModLoaded("socketed"))return;
        try {
            Class<?> context=Class.forName("socketed.common.loot.DefaultSocketsGenerator$SocketedItemCreationContext");
            Object mode=context.getField("MOB_DROP").get(null);
            Class<?> api=Class.forName("socketed.api.util.SocketedUtil");
            for(Method method:api.getMethods())if(method.getName().equals("addSocketsToStack") && method.getParameterCount()==2 && method.getParameterTypes()[0]==ItemStack.class && method.getParameterTypes()[1].isInstance(mode)){method.invoke(null,stack,mode);return;}
            throw new NoSuchMethodException("Socketed addSocketsToStack");
        }catch(ReflectiveOperationException | LinkageError ex){warn(ex);}
    }
    private static void warn(Throwable ex){if(!warned){warned=true;NanonaitorsArsenal.LOGGER.warn("Optional world-equipment integration could not access a compatible API; leaving affected content unchanged",ex);}}
}
