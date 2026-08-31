package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import com.nanonaitor.arsenal.network.ModernWeaponControlMessage;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemShield;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Forge-1.12 implementation of the mechanics shared with the 26.1 release. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ModernBackportCombat {
    private static final Map<EntityPlayer, MorningState> MORNING = new WeakHashMap<>();
    private static final Map<UUID, Long> LAST_SCIMITAR = new java.util.HashMap<>();
    private static final Map<UUID, Long> LAST_AUXILIARY_SCIMITAR = new java.util.HashMap<>();

    private ModernBackportCombat() {}

    public static void handleControl(EntityPlayerMP player, byte action, boolean active) {
        if (action == ModernWeaponControlMessage.MORNING_STAR) morningStar(player, active);
        else if (action == ModernWeaponControlMessage.MORNING_CANCEL) cancelMorning(player);
        else if (action == ModernWeaponControlMessage.SCIMITAR_ATTACK) scimitar(player, active);
        else if (action == ModernWeaponControlMessage.BULWARK_MENU_GUARD) menuGuard(player, active);
        else if (action == ModernWeaponControlMessage.BALL_WIND_BOOST)
            player.getEntityData().setBoolean("ArsenalBallWindBoost", active
                && player.getHeldItemMainhand().getItem()
                    instanceof com.nanonaitor.arsenal.item.ItemBallAndChain
                && player.getHeldItemOffhand().isEmpty());
        else if (action == ModernWeaponControlMessage.BULWARK_ATTACK) bulwarkAttack(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelMorningVanillaAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof ItemMorningStar)
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelMorningMining(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof ItemMorningStar) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
            || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        MorningState state = MORNING.get(player);
        if (state == null) return;
        ItemStack stack = player.getHeldItemMainhand();
        long now = player.world.getTotalWorldTime();
        if (!(stack.getItem() instanceof ItemMorningStar) || isUsingShield(player)
            || now - state.heartbeat > 4L) {
            cancelMorning(player);
            return;
        }
        if (!player.isHandActive()) player.setActiveHand(EnumHand.MAIN_HAND);
        if (!state.chimed && now - state.started >= chargeTicks()) {
            state.chimed = true;
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.55F, 1.55F);
        }
    }

    private static int chargeTicks() {
        return Math.max(1, (int)Math.ceil(ArsenalConfig.morningStar.fullChargeSeconds * 20.0D));
    }

    private static void morningStar(EntityPlayerMP player, boolean active) {
        if (!active) {
            if (!MORNING.containsKey(player)
                && player.getHeldItemMainhand().getItem() instanceof ItemMorningStar)
                MORNING.put(player, new MorningState(player.world.getTotalWorldTime()));
            releaseMorning(player);
            return;
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemMorningStar) || isUsingShield(player)) {
            cancelMorning(player);
            return;
        }
        long now = player.world.getTotalWorldTime();
        MorningState state = MORNING.get(player);
        if (state == null) {
            if (player.getCooledAttackStrength(0.5F) < 0.95F) return;
            state = new MorningState(now);
            MORNING.put(player, state);
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        state.heartbeat = now;
    }

    private static void cancelMorning(EntityPlayer player) {
        MORNING.remove(player);
        if (player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemMorningStar)
            player.resetActiveHand();
    }

    private static boolean isUsingShield(EntityPlayer player) {
        return player.isHandActive()
            && player.getActiveItemStack().getItem() instanceof ItemShield;
    }

    private static void releaseMorning(EntityPlayerMP player) {
        MorningState state = MORNING.remove(player);
        if (state == null) return;
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemMorningStar)) { player.resetActiveHand(); return; }
        ItemMorningStar item = (ItemMorningStar) stack.getItem();
        long elapsed = Math.max(0L, player.world.getTotalWorldTime() - state.started);
        int quarters = Math.min(4, (int)Math.floor(elapsed * 4.0D / chargeTicks()));
        boolean full = quarters >= 4;
        float multiplier = 1.0F + quarters * 0.10F;
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        double halfWidth = 2.0D * quarters / 4.0D;
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec().normalize();
        Vec3d horizontal = new Vec3d(look.x, 0, look.z);
        if (horizontal.lengthSquared() < 0.0001D) horizontal = new Vec3d(0, 0, 1);
        horizontal = horizontal.normalize();
        Vec3d right = new Vec3d(-horizontal.z, 0, horizontal.x);
        List<EntityLivingBase> targets = new ArrayList<>();
        if (quarters == 0) {
            // An instant click is a normal three-dimensional weapon strike.
            // This preserves aiming above and below the player instead of
            // forcing the charged attack's horizontal sweep plane.
            EntityLivingBase direct = aimedTarget(player, reach);
            if (direct != null) targets.add(direct);
        } else {
            AxisAlignedBB query = player.getEntityBoundingBox().grow(
                reach + 2.0D, 1.5D, reach + 2.0D);
            for (EntityLivingBase target : player.world.getEntitiesWithinAABB(
                    EntityLivingBase.class, query,
                    e -> e != player && !e.isDead && !player.isOnSameTeam(e))) {
                Vec3d rel = target.getEntityBoundingBox().getCenter().subtract(eye);
                double forward = rel.dotProduct(horizontal);
                double lateral = rel.dotProduct(right);
                if (forward >= -0.25D && forward <= reach + target.width * 0.5D
                    && Math.abs(lateral) <= halfWidth + target.width * 0.5D
                    && Math.abs(rel.y) <= 2.0D && player.canEntityBeSeen(target)) targets.add(target);
            }
            targets.sort(Comparator.comparingDouble(player::getDistanceSq));
        }
        spawnMorningSweep(player, eye, horizontal, right, reach, halfWidth);
        float base = (float)player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean hit = false;
        for (EntityLivingBase target : targets) {
            float damage = (base + EnchantmentHelper.getModifierForCreature(stack,
                target.getCreatureAttribute())) * multiplier;
            if (target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)) {
                hit = true;
                item.hitEntity(stack, target, player);
                if (full && ModContent.ARMOR_FRACTURE != null) {
                    PotionEffect old = target.getActivePotionEffect(ModContent.ARMOR_FRACTURE);
                    int level = old == null ? 1 : old.getAmplifier() + 2;
                    level = Math.min(level, item.getTier().getMorningStarFractureCap());
                    int duration = target instanceof EntityPlayer ? 200 : 600;
                    target.addPotionEffect(new PotionEffect(ModContent.ARMOR_FRACTURE,
                        duration, level - 1, false, true));
                }
                if (full && ModContent.STUNNED != null && player.getRNG().nextFloat() < 0.20F)
                    target.addPotionEffect(new PotionEffect(ModContent.STUNNED, 60, 0, false, true));
            }
        }
        player.swingArm(EnumHand.MAIN_HAND);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.85F, 0.88F);
        player.resetActiveHand();
        player.resetCooldown();
    }

    private static void spawnMorningSweep(EntityPlayerMP player, Vec3d eye,
                                          Vec3d forward, Vec3d right,
                                          double reach, double halfWidth) {
        if (halfWidth <= 0.0D) return;
        int forwardSteps = Math.max(1, (int)Math.ceil(reach));
        int sideSteps = Math.max(1, (int)Math.ceil(halfWidth * 2.0D));
        for (int f = 1; f <= forwardSteps; f++) {
            double along = Math.min(reach, f);
            for (int s = 0; s <= sideSteps; s++) {
                double lateral = -halfWidth + (halfWidth * 2.0D * s / sideSteps);
                Vec3d point = eye.add(forward.scale(along)).add(right.scale(lateral));
                player.getServerWorld().spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                    point.x, player.posY + 0.75D, point.z, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void scimitar(EntityPlayerMP player, boolean offhand) {
        EnumHand hand = offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack stack = player.getHeldItem(hand);
        if (!(stack.getItem() instanceof ItemScimitar)) return;
        boolean dual = player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar;
        if (!dual && !offhand) return;
        long now = player.world.getTotalWorldTime();
        boolean auxiliary = offhand && !dual;
        Map<UUID, Long> clock = auxiliary ? LAST_AUXILIARY_SCIMITAR : LAST_SCIMITAR;
        long last = clock.getOrDefault(player.getUniqueID(), Long.MIN_VALUE);
        double speed = auxiliary
            ? offhandScimitarAttackSpeed(player, stack)
            : Math.max(0.1D, player.getEntityAttribute(
                SharedMonsterAttributes.ATTACK_SPEED).getAttributeValue());
        double dualSpeed = speed >= 1.8D ? 2.0D : speed;
        double cooldown = dual ? 20.0D / Math.max(0.1D, dualSpeed) : 20.0D / speed;
        float attackMultiplier = 1.0F;
        if (auxiliary) {
            float strength = last == Long.MIN_VALUE ? 1.0F : (float)Math.max(0.0D,
                Math.min(1.0D, (now - last + 0.5D) / cooldown));
            attackMultiplier = 0.2F + strength * strength * 0.8F;
        } else if (last != Long.MIN_VALUE && now - last + 0.5D < cooldown) {
            return;
        }
        clock.put(player.getUniqueID(), now);
        EntityLivingBase target = aimedTarget(player, reach(player));
        player.swingArm(hand);
        if (target != null) {
            double current = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            if (offhand) {
                current -= mainHandDamageBonus(player.getHeldItemMainhand());
                current += mainHandDamageBonus(stack);
            }
            float damage = ((float)current + EnchantmentHelper.getModifierForCreature(stack,
                target.getCreatureAttribute())) * attackMultiplier;
            if (target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage))
                ((ItemScimitar) stack.getItem()).hitEntity(stack, target, player);
        }
        if (dual || !offhand) player.resetCooldown();
    }

    private static double reach(EntityPlayer player) {
        return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
    }

    private static double mainHandDamageBonus(ItemStack stack) {
        double result = 0.0D;
        for (AttributeModifier modifier : stack.getAttributeModifiers(
            EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            if (modifier.getOperation() == 0) result += modifier.getAmount();
        }
        return result;
    }

    public static double offhandScimitarAttackSpeed(EntityPlayer player,
                                                     ItemStack offhand) {
        double speed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED)
            .getAttributeValue();
        speed -= mainHandAttributeBonus(player.getHeldItemMainhand(),
            SharedMonsterAttributes.ATTACK_SPEED.getName());
        speed += mainHandAttributeBonus(offhand,
            SharedMonsterAttributes.ATTACK_SPEED.getName());
        return Math.max(0.1D, speed);
    }

    private static double mainHandAttributeBonus(ItemStack stack, String attribute) {
        double result = 0.0D;
        for (AttributeModifier modifier : stack.getAttributeModifiers(
            EntityEquipmentSlot.MAINHAND).get(attribute)) {
            if (modifier.getOperation() == 0) result += modifier.getAmount();
        }
        return result;
    }

    private static EntityLivingBase aimedTarget(EntityPlayer player, double reach) {
        Vec3d eye = player.getPositionEyes(1.0F), look = player.getLookVec().normalize();
        EntityLivingBase best = null; double bestAlong = Double.MAX_VALUE;
        AxisAlignedBB box = player.getEntityBoundingBox().expand(look.x * reach, look.y * reach,
            look.z * reach).grow(1.0D);
        for (EntityLivingBase target : player.world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> e != player && !e.isDead && !player.isOnSameTeam(e))) {
            Vec3d rel = target.getEntityBoundingBox().getCenter().subtract(eye);
            double along = rel.dotProduct(look);
            if (along < 0 || along > reach) continue;
            double sideSq = rel.subtract(look.scale(along)).lengthSquared();
            double radius = 0.75D + target.width * 0.5D;
            if (sideSq <= radius * radius && along < bestAlong) { best = target; bestAlong = along; }
        }
        return best;
    }

    private static void menuGuard(EntityPlayerMP player, boolean active) {
        boolean valid = active && ShieldCombat.isBulwarkOffCooldown(player)
            && (player.getHeldItemMainhand().getItem()
            instanceof ItemSunWarBulwark && player.getHeldItemOffhand().isEmpty()
            || player.getHeldItemOffhand().getItem() instanceof ItemSunWarBulwark
                && player.getHeldItemMainhand().isEmpty());
        player.getEntityData().setBoolean("ArsenalBulwarkMenuGuard", valid);
    }

    private static void bulwarkAttack(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItemOffhand();
        if (!(stack.getItem() instanceof ItemSunWarBulwark) || !player.getHeldItemMainhand().isEmpty()
            || player.getCooledAttackStrength(0.5F) < 0.95F) return;
        EntityLivingBase target = aimedTarget(player, reach(player));
        player.swingArm(EnumHand.OFF_HAND);
        if (target != null) {
            float armor = (float)Math.max(0.0D, player.getEntityAttribute(
                SharedMonsterAttributes.ARMOR).getAttributeValue());
            float strength = player.getCooledAttackStrength(0.5F);
            float damage = (1.0F + armor) * (0.2F + strength * strength * 0.8F);
            if (target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage))
                stack.damageItem(1, player);
        }
        player.resetCooldown();
    }

    private static final class MorningState {
        final long started; long heartbeat; boolean chimed;
        MorningState(long now) { started = heartbeat = now; }
    }
}
