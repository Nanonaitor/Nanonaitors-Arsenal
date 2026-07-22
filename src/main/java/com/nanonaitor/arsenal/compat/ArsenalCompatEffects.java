package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ArsenalCompatEffects {
    private static final String PROGRESS = "srpkills";
    private static final String LIGHTNING_TICK = "ArsenalElectricEffectTick";
    private static final String RAGE_MARKER = "ArsenalSrpRamRage";
    private ArsenalCompatEffects() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyDamage(LivingHurtEvent event) {
        EntityPlayer player = directPlayer(event.getSource());
        ItemArsenalWeapon weapon = heldWeapon(player);
        if (weapon == null) return;
        WeaponTier tier = weapon.getTier();
        EntityLivingBase target = event.getEntityLiving();
        float bonus = 0F;
        if (tier == WeaponTier.SILVER && target.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD) bonus += 2F;
        if (tier.isMyrmex()) {
            if (target.getCreatureAttribute() != EnumCreatureAttribute.ARTHROPOD) bonus += 4F;
            if (isEntity(target, "deathworm")) bonus += 4F;
        }
        if (tier == WeaponTier.FLAMED_DRAGONBONE && isEntity(target,"icedragon")) bonus += 13.5F;
        if (tier == WeaponTier.ICED_DRAGONBONE && isEntity(target,"firedragon")) bonus += 13.5F;
        if (tier == WeaponTier.ELECTRIC_DRAGONBONE
            && (isEntity(target,"firedragon") || isEntity(target,"icedragon"))) bonus += 6.75F;
        event.setAmount(event.getAmount() + bonus);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void confirmedDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0F) return;
        EntityPlayer player = directPlayer(event.getSource());
        ItemArsenalWeapon weapon = heldWeapon(player);
        if (weapon == null) return;
        WeaponTier tier = weapon.getTier();
        EntityLivingBase target = event.getEntityLiving();
        if (tier.isVenom()) target.addPotionEffect(new PotionEffect(MobEffects.POISON,200,2));
        if (tier == WeaponTier.LIVING || tier == WeaponTier.SENTIENT) {
            applyLivingFamilyProc(weapon, target, tier == WeaponTier.SENTIENT);
        }
        if (tier == WeaponTier.FLAMED_DRAGONBONE) {
            target.setFire(5); knockBack(target,player);
        } else if (tier == WeaponTier.ICED_DRAGONBONE) {
            setFrozen(target,200);
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,100,2));
            target.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE,100,2));
            knockBack(target,player);
        } else if (tier == WeaponTier.ELECTRIC_DRAGONBONE) {
            long now=player.world.getTotalWorldTime();
            if (player.getEntityData().getLong(LIGHTNING_TICK)!=now) {
                player.getEntityData().setLong(LIGHTNING_TICK,now); createChainLightning(target,player);
            }
            knockBack(target,player);
        }
    }

    /** Maintains SRP's held-item effects without making SRP a hard dependency. */
    @SubscribeEvent
    public static void heldSrpEffects(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        ItemArsenalWeapon weapon = heldWeapon(player);
        boolean srpRam = weapon instanceof ItemBatteringRam
            && (weapon.getTier() == WeaponTier.LIVING || weapon.getTier() == WeaponTier.SENTIENT);
        Potion rage = ArsenalCompatManager.potion("rage");
        if (srpRam && rage != null) {
            int amplifier = weapon.getTier() == WeaponTier.SENTIENT ? 1 : 0;
            player.addPotionEffect(new PotionEffect(rage, 10, amplifier, false, false));
            player.getEntityData().setBoolean(RAGE_MARKER, true);
        } else if (player.getEntityData().getBoolean(RAGE_MARKER)) {
            if (rage != null) player.removePotionEffect(rage);
            player.getEntityData().removeTag(RAGE_MARKER);
        }

        // Native sentient SRP tools occasionally mark their wielder as Prey.
        if (weapon != null && weapon.getTier() == WeaponTier.SENTIENT
            && ArsenalCompatManager.isSrpScentEnabled()
            && player.ticksExisted % 40 == 0 && player.getRNG().nextInt(100) == 0) {
            addPotion(player, "prey", 1200, 0);
        }
    }

    private static void applyLivingFamilyProc(ItemArsenalWeapon weapon,
                                               EntityLivingBase target,
                                               boolean sentient) {
        int amplifier = sentient ? 1 : 0;
        if (weapon instanceof ItemMorningStar) {
            addPotion(target, "corrosive", 100, amplifier);
        } else if (weapon instanceof ItemClaws) {
            addPotion(target, "bleed", 100, amplifier);
        } else if (weapon instanceof ItemFlail) {
            addPotion(target, "antimall", 100, amplifier);
        } else if (weapon instanceof ItemBallAndChain) {
            if (ArsenalCompatManager.isSrpParasite(target)) {
                addPotion(target, "debar", 100, amplifier);
            }
        } else if (weapon instanceof ItemScimitar) {
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100,
                sentient ? 2 : 1));
        }
        // Battering Rams keep their proc on the wielder through heldSrpEffects().
    }

    private static void addPotion(EntityLivingBase target, String path,
                                  int ticks, int amplifier) {
        Potion potion = ArsenalCompatManager.potion(path);
        if (potion != null) target.addPotionEffect(
            new PotionEffect(potion, ticks, amplifier, false, true));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void evolution(LivingDeathEvent event) {
        EntityPlayer player=directPlayer(event.getSource());
        ItemArsenalWeapon weapon=heldWeapon(player);
        if (weapon==null || weapon.getTier()!=WeaponTier.LIVING
            || !ArsenalCompatManager.isSrpParasite(event.getEntityLiving())) return;
        ItemStack stack=player.getHeldItemMainhand();
        NBTTagCompound tag=stack.hasTagCompound()?stack.getTagCompound():new NBTTagCompound();
        int points=tag.getInteger(PROGRESS)+(int)event.getEntityLiving().getMaxHealth();
        tag.setInteger(PROGRESS,points); stack.setTagCompound(tag);
        if (points > ArsenalCompatManager.getSrpEvolutionThreshold()) evolve(player,stack);
    }
    private static void evolve(EntityPlayer player, ItemStack living) {
        ItemArsenalWeapon sentient=sentientCounterpart((ItemArsenalWeapon)living.getItem());
        if (sentient==null) return;
        ItemStack result=new ItemStack(sentient);
        if (living.hasTagCompound()) { NBTTagCompound tag=living.getTagCompound().copy(); tag.removeTag(PROGRESS); result.setTagCompound(tag); }
        float used=living.getMaxDamage()<=0?0F:(float)living.getItemDamage()/living.getMaxDamage();
        result.setItemDamage(Math.min(result.getMaxDamage()-1,Math.round(used*result.getMaxDamage())));
        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND,result);
        player.world.playSound(null,player.posX,player.posY,player.posZ,SoundEvents.ENTITY_LIGHTNING_THUNDER,SoundCategory.PLAYERS,.7F,1.3F);
        if (player.world instanceof WorldServer) {
            ((WorldServer)player.world).addWeatherEffect(new EntityLightningBolt(player.world,player.posX,player.posY,player.posZ,true));
            ((WorldServer)player.world).spawnParticle(EnumParticleTypes.SPELL_MOB,player.posX,player.posY+1,player.posZ,35,.35,.6,.35,.05);
        }
    }
    private static ItemArsenalWeapon sentientCounterpart(ItemArsenalWeapon item) {
        ItemArsenalWeapon found=find(ModContent.MORNING_STARS,item); if(found!=null)return found;
        found=find(ModContent.SCIMITARS,item); if(found!=null)return found;
        found=find(ModContent.CLAWS,item); if(found!=null)return found;
        found=find(ModContent.FLAILS,item); if(found!=null)return found;
        found=find(ModContent.BATTERING_RAMS,item); if(found!=null)return found;
        return find(ModContent.BALLS_AND_CHAINS,item);
    }
    private static <T extends ItemArsenalWeapon> T find(Map<WeaponTier,T> map, ItemArsenalWeapon item) {
        return map.get(WeaponTier.LIVING)==item?map.get(WeaponTier.SENTIENT):null;
    }
    private static EntityPlayer directPlayer(DamageSource source) {
        Entity trueSource=source.getTrueSource(), immediate=source.getImmediateSource();
        return trueSource instanceof EntityPlayer && trueSource==immediate?(EntityPlayer)trueSource:null;
    }
    private static ItemArsenalWeapon heldWeapon(EntityPlayer player) {
        if (player==null) return null;
        return player.getHeldItemMainhand().getItem() instanceof ItemArsenalWeapon
            ?(ItemArsenalWeapon)player.getHeldItemMainhand().getItem():null;
    }
    private static boolean isEntity(EntityLivingBase e,String path) {
        ResourceLocation id=EntityList.getKey(e); return id!=null&&"iceandfire".equals(id.getResourceDomain())&&path.equals(id.getResourcePath());
    }
    private static void knockBack(EntityLivingBase target,EntityLivingBase source) {
        target.knockBack(source,1F,source.posX-target.posX,source.posZ-target.posZ);
    }
    private static void setFrozen(EntityLivingBase target,int ticks) {
        try { Class<?> api=Class.forName("com.github.alexthe666.iceandfire.api.InFCapabilities");
            Object capability=api.getMethod("getEntityEffectCapability",EntityLivingBase.class).invoke(null,target);
            Method setter=capability.getClass().getMethod("setFrozen",int.class); setter.invoke(capability,ticks);
        } catch(ReflectiveOperationException|LinkageError ignored) {}
    }
    private static void createChainLightning(EntityLivingBase target,EntityLivingBase source) {
        try { Class<?> api=Class.forName("com.github.alexthe666.iceandfire.api.ChainLightningUtils");
            api.getMethod("createChainLightningFromTarget",net.minecraft.world.World.class,EntityLivingBase.class,EntityLivingBase.class)
                .invoke(null,target.world,target,source);
        } catch(ReflectiveOperationException|LinkageError ignored) {}
    }
}
