package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.compat.ReskillableCompat;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.network.BallAndChainReleaseAnimationMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.registry.ModContent;
import com.nanonaitor.arsenal.registry.ModSounds;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class BallAndChainCombat {
    public static final int RELEASE_ANIMATION_TICKS = 16;
    public static final double WINDUP_REACH = 3.0D;
    /**
     * Thrown-ball hit radius around the ball's centre, close to its drawn size
     * (0.24-0.32 blocks), so a hit happens where the ball visibly touches.
     */

    /** Radius of the ball as drawn for a throw of this charge (matches the client renderer). */
    public static double visualBallRadius(int charge) {
        return 0.625D * (0.24D + Math.max(1, Math.min(3, charge)) * 0.025D) * 1.3D;
    }

    /** Damage hits use a radius 30% larger than the drawn ball. */
    public static double hitRadius(int charge) {
        return visualBallRadius(charge) * 1.3D;
    }

    /** Blocks stop the ball a little inside its drawn size, so near misses fly on. */
    public static double crashRadius(int charge) {
        return visualBallRadius(charge) * 0.8D;
    }
    private static final float[] RELEASE_DAMAGE_MULTIPLIER = {0.0F, 1.0F, 1.5F, 2.0F};
    // cancelAbility can run on the client thread (item use), so keep sides apart.
    private static final SidedPlayerMap<SwingState> SWINGS = new SidedPlayerMap<>();
    private static final SidedPlayerMap<ThrowState> THROWS = new SidedPlayerMap<>();

    /** How close (in blocks, plus half the projectile's width) the ball must pass to smash a projectile. */
    private static final double PROJECTILE_SMASH_RADIUS = 0.9D;

    private BallAndChainCombat() {}

    /** Server: winding up (rotating) and not yet released. */
    public static boolean isWindingUp(EntityPlayer player) {
        return SWINGS.containsKey(player) && !THROWS.containsKey(player);
    }

    /** Server: the ball is out on a throw or on its way back. */
    public static boolean isThrowing(EntityPlayer player) {
        return THROWS.containsKey(player);
    }

    public static void cancelAbility(EntityPlayer player){
        SWINGS.remove(player);THROWS.remove(player);
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive",false);
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallWindBoost",false);
        if(player.isHandActive()&&player.getActiveItemStack().getItem() instanceof ItemBallAndChain)player.resetActiveHand();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelVanillaAttack(AttackEntityEvent event) {
        if (com.nanonaitor.arsenal.combat.AttackProbe.isAreaProbe(event)) return; // e.g. Callable Horses' area check
        if (event.getEntityPlayer().getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            event.setCanceled(true);
        }
    }

    public static void updateSwinging(EntityPlayerMP player, boolean swinging) {
        if (shieldStopsBall(player)) { cancelAbility(player); return; }
        if(AbilityUseRules.cooling(player,player.getHeldItemMainhand())){cancelAbility(player);return;}
        if (usingRealShield(player)) {SWINGS.remove(player);return;}
        if (!swinging) {
            release(player);
            return;
        }
        if (THROWS.containsKey(player)) {
            return;
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (!isValidWielder(player, stack)) {
            SWINGS.remove(player);
            return;
        }
        long now = player.world.getTotalWorldTime();
        SwingState state = SWINGS.get(player);
        if (state == null) {
            state = new SwingState(now);
            SWINGS.put(player, state);
        }
        state.lastHeartbeatTick = now;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
            || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if(AbilityUseRules.cooling(player,player.getHeldItemMainhand())){cancelAbility(player);return;}
        if (usingRealShield(player)) {SWINGS.remove(player);THROWS.remove(player);return;}
        if (updateThrow(player)) {
            return;
        }
        SwingState state = SWINGS.get(player);
        if (state == null) {
            return;
        }
        ItemStack weapon = player.getHeldItemMainhand();
        if (!isValidWielder(player, weapon)) {
            SWINGS.remove(player);
            stopBallUse(player);
            return;
        }
        long now = player.world.getTotalWorldTime();
        if (now - state.lastHeartbeatTick > 3L) {
            release(player);
            return;
        }
        if (!player.isHandActive()) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        if (now % 2L == 0L) ModNetwork.CHANNEL.sendToAllAround(
            new com.nanonaitor.arsenal.network.BallAndChainWindupMessage(player.getEntityId()),
            new NetworkRegistry.TargetPoint(player.dimension,player.posX,player.posY,player.posZ,64.0D));
        int interval = ChainWeaponStats.swingIntervalTicks(player, weapon);
        if (state.interval != 0 && interval != state.interval && now < state.nextSwingTick) {
            // The spin sped up or slowed down mid-turn: keep the share of the turn
            // still to go, at the new speed, instead of restarting the count.
            state.nextSwingTick = now + Math.max(1L,
                Math.round((state.nextSwingTick - now) * interval / (double) state.interval));
        }
        state.interval = interval;
        if (now >= state.nextSwingTick) {
            state.nextSwingTick = now + interval;
            int maxCharges = maxCharges(((ItemBallAndChain) weapon.getItem()).getTier());
            int previousCharge = state.charge;
            state.charge = BallChargeRules.nextCharge(state.charge, maxCharges,
                com.nanonaitor.arsenal.compat.SilverSetBonusCompat.hasMatchingMetalSet(
                    player, ((ItemBallAndChain) weapon.getItem()).getTier()));
            performWindupSweep(player, weapon, state.charge);
            if (previousCharge < maxCharges && state.charge == maxCharges) {
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(),
                    0.55F, 1.55F);
            }
        }
    }

    private static boolean updateThrow(EntityPlayerMP player) {
        ThrowState state = THROWS.get(player);
        if (state == null) {
            return false;
        }
        ItemStack current = player.getHeldItemMainhand();
        if (current != state.weapon || !(current.getItem() instanceof ItemBallAndChain)
            || ((ItemBallAndChain) current.getItem()).getTier() != state.tier
            ) {
            THROWS.remove(player);
            stopBallUse(player);
            return false;
        }
        long age = player.world.getTotalWorldTime() - state.startTick;
        // Hits follow the ball as drawn: each tick checks only the stretch the
        // ball travelled this tick, outbound first, then on the way back.
        Vec3d from = state.lastBallPosition;
        Vec3d to = ballPosition(state, age);
        state.lastBallPosition = to;
        hitAlongPath(player, state, from, to, age * 2L > state.durationTicks);
        if (state.crashAt != null && !state.crashed && age * 2L >= state.durationTicks) {
            state.crashed = true;
            crashFeedback(player, state);
        }
        smashProjectiles(player, from, to);
        if (age >= state.durationTicks || player.isDead) {
            THROWS.remove(player);
            stopBallUse(player);
            return false;
        }
        if (!player.isHandActive() && player.getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        return true;
    }

    /** A conventional shield in use; a raised Defender lets the ball keep going. */
    /** The ball hits a block: heavy thud, a louder chain clash, and a burst of dust and sparks. */
    private static void crashFeedback(EntityPlayerMP player, ThrowState state) {
        Vec3d at = state.crashAt;
        player.world.playSound(null, at.x, at.y, at.z,
            SoundEvents.ENTITY_IRONGOLEM_ATTACK, player.getSoundCategory(),
            0.8F, 0.75F + state.charge * 0.08F);
        player.world.playSound(null, at.x, at.y, at.z,
            ModSounds.BALL_CHAIN_CRASH, player.getSoundCategory(),
            1.0F, 0.85F + player.getRNG().nextFloat() * 0.1F);
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(
            at.add(state.direction.scale(0.35D)));
        net.minecraft.block.state.IBlockState block = player.world.getBlockState(pos);
        net.minecraft.world.WorldServer world = player.getServerWorld();
        // Chips burst off the block face; sparks and a puff come off the ball itself.
        Vec3d face = at.add(state.direction.scale(0.3D));
        if (block.getMaterial() != net.minecraft.block.material.Material.AIR) {
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.BLOCK_DUST, face.x, face.y, face.z,
                30 + state.charge * 12, 0.3D, 0.3D, 0.3D, 0.15D,
                net.minecraft.block.Block.getStateId(block));
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.BLOCK_CRACK, face.x, face.y, face.z,
                16 + state.charge * 6, 0.2D, 0.2D, 0.2D, 0.25D,
                net.minecraft.block.Block.getStateId(block));
        }
        world.spawnParticle(net.minecraft.util.EnumParticleTypes.CRIT, at.x, at.y, at.z,
            14 + state.charge * 6, 0.15D, 0.15D, 0.15D, 0.55D);
        world.spawnParticle(net.minecraft.util.EnumParticleTypes.FIREWORKS_SPARK, at.x, at.y, at.z,
            6 + state.charge * 3, 0.1D, 0.1D, 0.1D, 0.12D);
        world.spawnParticle(net.minecraft.util.EnumParticleTypes.EXPLOSION_NORMAL, at.x, at.y, at.z,
            2 + state.charge, 0.1D, 0.1D, 0.1D, 0.02D);
    }

    private static boolean usingRealShield(EntityPlayer player) {
        return AbilityUseRules.activeConventionalShield(player);
    }

    /** Shield use (or a shield action's attack lock) stops the ball, except while guarding with a Defender. */
    private static boolean shieldStopsBall(EntityPlayer player) {
        return AbilityUseRules.suppressedUnlessDefending(player);
    }

    /** Ends the ball's own use action only, never a Defender being held up. */
    private static void stopBallUse(EntityPlayer player) {
        if (player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemBallAndChain) {
            player.resetActiveHand();
        }
    }

    private static boolean isValidWielder(EntityPlayerMP player, ItemStack stack) {
        return stack.getItem() instanceof ItemBallAndChain
            && player.isEntityAlive()
            && ReskillableCompat.canUse(player, stack)
            && !player.isSpectator();
    }

    private static void performWindupSweep(EntityPlayerMP player, ItemStack weapon,
                                           int charge) {
        Vec3d start = new Vec3d(player.posX, player.posY + player.height * 0.5D,
            player.posZ);
        Vec3d forward = horizontalLook(player);
        double reach = ChainWeaponStats.ballWindupReach(player, weapon);
        Vec3d end = stopAtSolidBlock(player, start,
            start.add(forward.scale(reach)), crashRadius(1));
        AxisAlignedBB search = sweptBox(start, end, 0.7D, 1.0D);
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class, search,
            target -> isValidTarget(player, target));
        targets.sort(Comparator.comparingDouble(target -> target.getDistanceSq(start.x,
            start.y, start.z)));
        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean hit = false;
        for (EntityLivingBase target : targets) {
            if (intersectsLine(target, start, end, 0.7D, 1.0D)) {
                float damage = baseDamage * 0.5F + EnchantmentHelper.getModifierForCreature(
                    weapon, target.getCreatureAttribute());
                hit |= applyHit(player, target, weapon, damage, 0.3F,
                    forward, false);
            }
        }
        playAttackSound(player, hit, 0.82F + charge * 0.07F);
    }

    private static void release(EntityPlayerMP player) {
        SwingState swing = SWINGS.remove(player);
        ItemStack weapon = player.getHeldItemMainhand();
        if (swing == null || THROWS.containsKey(player)) {
            return;
        }
        if (!isValidWielder(player, weapon) || swing.charge <= 0) {
            stopBallUse(player);
            return;
        }
        ItemBallAndChain item = (ItemBallAndChain) weapon.getItem();
        int charge = Math.min(maxCharges(item.getTier()), swing.charge);
        int effectiveCharge = item.getTier() == WeaponTier.GOLD && charge >= 2 ? 3 : charge;
        Vec3d start = new Vec3d(player.posX, player.posY + player.getEyeHeight() * 0.75D,
            player.posZ);
        Vec3d direction = player.getLookVec().normalize();
        double throwReach = ChainWeaponStats.ballThrowReach(player, weapon,
            effectiveCharge);
        double longChain = Math.min(throwReach - 0.01D,
            ChainWeaponStats.longChainBonus(player, weapon));
        Vec3d horizontal = new Vec3d(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSquared() > 0.0001D) horizontal = horizontal.normalize();
        Vec3d intendedEnd = start.add(direction.scale(throwReach - longChain))
            .add(horizontal.scale(longChain));
        Vec3d end = stopAtSolidBlock(player, start, intendedEnd, crashRadius(effectiveCharge));
        float multiplier = RELEASE_DAMAGE_MULTIPLIER[effectiveCharge];
        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * multiplier;
        int durationTicks = ChainWeaponStats.ballReleaseAnimationTicks(player, weapon);
        // Keep the ball's speed when a block cuts the throw short (it used to slow down).
        double full = start.distanceTo(intendedEnd);
        if (full > 0.01D && end.distanceTo(intendedEnd) > 0.05D) {
            durationTicks = Math.max(4, (int) Math.round(durationTicks * start.distanceTo(end) / full));
        }
        ThrowState state = new ThrowState(player.world.getTotalWorldTime(),
            durationTicks, weapon, item.getTier(), effectiveCharge, start, end,
            direction, baseDamage);
        THROWS.put(player, state);
        player.setActiveHand(EnumHand.MAIN_HAND);
        sendReleaseAnimation(player, effectiveCharge, (float) start.distanceTo(end),
            durationTicks);
        playAttackSound(player, false, 0.72F + state.charge * 0.10F);
        if (end.distanceTo(intendedEnd) > 0.05D) {
            // Played when the ball arrives (see updateThrow), so sound and sight match.
            state.crashAt = end;
        }
    }

    /** Damages targets the ball touches between two of its tick positions, once per direction. */
    private static void hitAlongPath(EntityPlayerMP player, ThrowState state, Vec3d from, Vec3d to,
                                     boolean returning) {
        java.util.Set<Integer> already = returning ? state.hitReturning : state.hitOutbound;
        Vec3d direction = returning ? state.direction.scale(-1.0D) : state.direction;
        AxisAlignedBB search = sweptBox(from, to, hitRadius(state.charge), hitRadius(state.charge));
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class, search,
            target -> isValidTarget(player, target) && !already.contains(target.getEntityId()));
        targets.sort(Comparator.comparingDouble(target -> target.getDistanceSq(from.x,
            from.y, from.z)));
        boolean hit = false;
        for (EntityLivingBase target : targets) {
            if (!intersectsLine(target, from, to, hitRadius(state.charge), hitRadius(state.charge))) {
                continue;
            }
            already.add(target.getEntityId());
            float damage = state.baseDamage + EnchantmentHelper.getModifierForCreature(
                state.weapon, target.getCreatureAttribute());
            if (isFullCharge(state)) {
                damage = compensateForArmor(target, damage,
                    getArmorPiercing(state.tier));
            }
            float knockback = 0.75F + state.charge * 0.55F;
            if (applyHit(player, target, state.weapon, damage, knockback,
                direction, true)) {
                hit = true;
                if (!returning && isFullCharge(state)) {
                    fractureArmor(target, state.tier);
                    armorPierceFeedback(player, target);
                }
            }
        }
        if (hit) playAttackSound(player, true, (returning ? 0.66F : 0.72F) + state.charge * 0.10F);
        if (hit && !returning && !state.shook && isFullCharge(state)) {
            // A full-charge hit lands heavily: one short jolt of the thrower's camera.
            state.shook = true;
            ModNetwork.CHANNEL.sendTo(new com.nanonaitor.arsenal.network.ArsenalFxMessage(
                com.nanonaitor.arsenal.network.ArsenalFxMessage.SHAKE, player.getEntityId(), 60), player);
        }
    }

    private static boolean applyHit(EntityPlayerMP player, EntityLivingBase target,
                                    ItemStack weapon, float damage, float knockback,
                                    Vec3d direction, boolean ignoreCurrentIFrames) {
        int previousResistance = target.hurtResistantTime;
        if (ignoreCurrentIFrames) {
            target.hurtResistantTime = 0;
        }
        boolean bypassAdaptation = weapon.getItem() instanceof ItemBallAndChain
            && ((((ItemBallAndChain) weapon.getItem()).getTier() == WeaponTier.LIVING)
                || (((ItemBallAndChain) weapon.getItem()).getTier() == WeaponTier.SENTIENT))
            && ArsenalCompatManager.isSrpParasite(target);
        Potion antimall = bypassAdaptation ? ArsenalCompatManager.potion("antimall") : null;
        PotionEffect previousAntimall = antimall == null ? null
            : target.getActivePotionEffect(antimall);
        if (antimall != null && previousAntimall == null) {
            target.addPotionEffect(new PotionEffect(antimall, 2, 0, false, false));
        }
        boolean damaged = false;
        try {
            damaged = target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
        } finally {
            // Do not leave temporary adaptation/i-frame changes behind if another
            // mod throws from its damage hook. Do not swallow or retry damage.
            if (antimall != null && previousAntimall == null) target.removePotionEffect(antimall);
            if (!damaged && ignoreCurrentIFrames) target.hurtResistantTime = previousResistance;
        }
        if (!damaged) return false;
        WeaponHits.applyFireAspect(player, target);
        float totalKnockback = knockback
            + EnchantmentHelper.getKnockbackModifier(player) * 0.35F;
        if (totalKnockback > 0.0F) {
            target.knockBack(player, totalKnockback, -direction.x, -direction.z);
            target.motionY += direction.y * totalKnockback * 0.35D;
            target.velocityChanged = true;
        }
        WeaponHits.applyHitEnchantments(player, target);
        weapon.damageItem(1, player);
        player.addExhaustion(0.1F);
        return true;
    }

    /** Where the ball is on its way out and back; matches the client's sine-shaped release animation. */
    private static Vec3d ballPosition(ThrowState state, long age) {
        double progress = Math.max(0.0D, Math.min(1.0D, age / (double) Math.max(1, state.durationTicks)));
        double out = Math.sin(progress * Math.PI);
        return state.start.add(state.end.subtract(state.start).scale(out));
    }

    /**
     * Destroys projectiles the thrown ball runs into, outbound or returning.
     * Each tick compares the ball's path this tick with each projectile's path
     * this tick, so fast arrows cannot slip between two ticks.
     */
    private static void smashProjectiles(EntityPlayerMP player, Vec3d from, Vec3d to) {
        // Grown generously: an arrow can move about 3 blocks in one tick.
        AxisAlignedBB search = sweptBox(from, to, 4.0D, 4.0D);
        for (net.minecraft.entity.Entity projectile : player.world.getEntitiesInAABBexcluding(
                player, search, entity -> isSmashableProjectile(player, entity))) {
            double centerY = projectile.height * 0.5D;
            Vec3d previous = new Vec3d(projectile.prevPosX, projectile.prevPosY + centerY, projectile.prevPosZ);
            Vec3d current = new Vec3d(projectile.posX, projectile.posY + centerY, projectile.posZ);
            double reach = PROJECTILE_SMASH_RADIUS + projectile.width * 0.5D;
            if (SegmentMath.distanceSq(from, to, previous, current) > reach * reach) continue;
            projectile.setDead();
            player.getServerWorld().spawnParticle(net.minecraft.util.EnumParticleTypes.CRIT,
                current.x, current.y, current.z, 10, 0.15D, 0.15D, 0.15D, 0.25D);
            // The same crack as an armour-piercing hit, a little lighter.
            player.world.playSound(null, current.x, current.y, current.z,
                com.nanonaitor.arsenal.registry.ModSounds.BALL_CHAIN_ARMOR_PIERCE, player.getSoundCategory(),
                0.8F, 1.1F + player.getRNG().nextFloat() * 0.15F);
        }
    }

    /** A moving projectile not fired by the wielder (stuck arrows have no motion). */
    private static boolean isSmashableProjectile(EntityPlayer player, net.minecraft.entity.Entity entity) {
        if (entity.isDead || !(entity instanceof net.minecraft.entity.IProjectile
            || entity instanceof net.minecraft.entity.projectile.EntityFireball
            || entity instanceof net.minecraft.entity.projectile.EntityShulkerBullet)) return false;
        double speedSq = entity.motionX * entity.motionX + entity.motionY * entity.motionY
            + entity.motionZ * entity.motionZ;
        return speedSq > 0.0025D && projectileOwner(entity) != player;
    }

    private static net.minecraft.entity.Entity projectileOwner(net.minecraft.entity.Entity projectile) {
        if (projectile instanceof net.minecraft.entity.projectile.EntityArrow)
            return ((net.minecraft.entity.projectile.EntityArrow) projectile).shootingEntity;
        if (projectile instanceof net.minecraft.entity.projectile.EntityThrowable)
            return ((net.minecraft.entity.projectile.EntityThrowable) projectile).getThrower();
        if (projectile instanceof net.minecraft.entity.projectile.EntityFireball)
            return ((net.minecraft.entity.projectile.EntityFireball) projectile).shootingEntity;
        if (projectile instanceof net.minecraft.entity.projectile.EntityLlamaSpit)
            return ((net.minecraft.entity.projectile.EntityLlamaSpit) projectile).owner;
        return null;
    }

    private static boolean isValidTarget(EntityPlayerMP player,
                                         EntityLivingBase target) {
        return CombatTargetRules.canHit(player, target);
    }

    private static Vec3d horizontalLook(EntityPlayer player) {
        double yaw = Math.toRadians(player.rotationYaw);
        return new Vec3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    private static Vec3d stopAtSolidBlock(EntityPlayerMP player, Vec3d start,
                                          Vec3d intendedEnd, double radius) {
        Vec3d travel = intendedEnd.subtract(start);
        double length = travel.lengthVector();
        if (length <= 0.0001D) return intendedEnd;
        Vec3d direction = travel.scale(1.0D / length);
        Vec3d side = new Vec3d(-direction.z, 0.0D, direction.x);
        if (side.lengthSquared() > 0.0001D) side = side.normalize().scale(radius);
        // The ball's outline, slightly inside the drawn size: centre, top, bottom, sides.
        Vec3d[] offsets = {
            new Vec3d(0.0D, 0.0D, 0.0D),
            new Vec3d(0.0D, -radius, 0.0D),
            new Vec3d(0.0D, radius, 0.0D),
            side,
            side.scale(-1.0D)
        };
        double closest = length;
        for (Vec3d offset : offsets) {
            Vec3d rayStart = start.add(offset);
            RayTraceResult hit = player.world.rayTraceBlocks(rayStart,
                intendedEnd.add(offset), false, true, false);
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
                closest = Math.min(closest, rayStart.distanceTo(hit.hitVec));
            }
        }
        // Stop with the ball's surface at the block, not its centre inside it.
        return closest < length ? start.add(direction.scale(Math.max(0.0D, closest - radius))) : intendedEnd;
    }

    private static AxisAlignedBB sweptBox(Vec3d start, Vec3d end,
                                          double horizontal, double vertical) {
        return new AxisAlignedBB(start.x, start.y, start.z, end.x, end.y, end.z)
            .grow(horizontal, vertical, horizontal);
    }

    private static boolean intersectsLine(EntityLivingBase target, Vec3d start,
                                          Vec3d end, double horizontal,
                                          double vertical) {
        AxisAlignedBB box = target.getEntityBoundingBox().grow(
            horizontal, vertical, horizontal);
        return box.contains(start) || box.contains(end)
            || box.calculateIntercept(start, end) != null;
    }

    private static void sendReleaseAnimation(EntityPlayerMP player, int charge,
                                             float distance, int durationTicks) {
        BallAndChainReleaseAnimationMessage message =
            new BallAndChainReleaseAnimationMessage(player.getEntityId(), charge,
                distance, player.rotationYaw, player.rotationPitch, durationTicks);
        ModNetwork.CHANNEL.sendToAllAround(message, new NetworkRegistry.TargetPoint(
            player.dimension, player.posX, player.posY, player.posZ, 64.0D));
    }

    private static void playAttackSound(EntityPlayerMP player, boolean hit, float pitch) {
        // Called only by a wind-up damage sweep or an outbound/return damage pass.
        // Separate IDs keep legitimate audio out of the client melee-spam filter.
        SoundEvent sound = hit ? ModSounds.BALL_CHAIN_HIT : ModSounds.BALL_CHAIN_SWING;
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            sound, player.getSoundCategory(), hit ? 1.0F : 0.65F, pitch);
        // The chain itself rattles with every swing, a little under the swing's level.
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            ModSounds.BALL_CHAIN_RATTLE, player.getSoundCategory(), 0.55F,
            0.9F + player.getRNG().nextFloat() * 0.2F);
    }

    private static float getArmorPiercing(WeaponTier tier) {
        return tier.getArmorPiercePercent() / 100.0F;
    }

    private static int maxCharges(WeaponTier tier) {
        return tier == WeaponTier.GOLD ? 2 : 3;
    }

    private static boolean isFullCharge(ThrowState state) {
        return state.charge >= 3;
    }

    private static float compensateForArmor(EntityLivingBase target, float damage,
                                             float piercing) {
        float armor = target.getTotalArmorValue();
        float toughness = (float) target.getEntityAttribute(
            SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
        float armoredDamage = CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
        float desiredDamage = armoredDamage + (damage - armoredDamage) * piercing;
        if (desiredDamage <= armoredDamage + 0.001F) {
            return damage;
        }
        float low = damage;
        float high = Math.max(damage * 10.0F, damage + armor * 5.0F + 10.0F);
        for (int iteration = 0; iteration < 16; iteration++) {
            float middle = (low + high) * 0.5F;
            if (CombatRules.getDamageAfterAbsorb(middle, armor, toughness)
                < desiredDamage) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    /** A full-charge hit: an armour crack sound and the fracture icon over the target. */
    private static void armorPierceFeedback(EntityPlayerMP player, EntityLivingBase target) {
        target.world.playSound(null, target.posX, target.posY + target.height * 0.5D, target.posZ,
            com.nanonaitor.arsenal.registry.ModSounds.BALL_CHAIN_ARMOR_PIERCE, player.getSoundCategory(),
            1.0F, 0.9F + player.getRNG().nextFloat() * 0.2F);
        ModNetwork.CHANNEL.sendToAllTracking(new com.nanonaitor.arsenal.network.ArsenalFxMessage(
            com.nanonaitor.arsenal.network.ArsenalFxMessage.ARMOR_BREAK, target.getEntityId(), 5), target);
    }

    private static void fractureArmor(EntityLivingBase target, WeaponTier tier) {
        if (target instanceof EntityPlayer) {
            if (ModContent.ARMOR_FRACTURE == null) {
                return;
            }
            int piercing = tier.getArmorPiercePercent();
            int amplifier = piercing >= 100 ? 4 : piercing >= 75 ? 2
                : piercing >= 50 ? 1 : 0;
            com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,
                com.nanonaitor.arsenal.config.ArsenalConfig.ballAndChain.playerFracture, 200, amplifier);
            return;
        }
        IAttributeInstance armor = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armor == null) {
            return;
        }
        double currentArmor = Math.max(0.0D, armor.getAttributeValue());
        double reduction = Math.min(currentArmor,
            Math.max(2.0D, currentArmor * 0.10D));
        if (reduction > 0.0D) {
            armor.applyModifier(new AttributeModifier(UUID.randomUUID(),
                "Ball and Chain permanent armor fracture", -reduction, 0)
                .setSaved(true));
        }
    }

    private static final class SwingState {
        private long lastHeartbeatTick;
        private long nextSwingTick;
        private int charge;
        private int interval;

        private SwingState(long startTick) {
            this.lastHeartbeatTick = startTick;
            this.nextSwingTick = startTick;
        }
    }

    private static final class ThrowState {
        private final long startTick;
        private final int durationTicks;
        private final ItemStack weapon;
        private final WeaponTier tier;
        private final int charge;
        private final Vec3d start;
        private final Vec3d end;
        private final Vec3d direction;
        private final float baseDamage;
        private Vec3d lastBallPosition;
        /** Where the ball meets a block (null when the path is clear); played once on arrival. */
        private Vec3d crashAt;
        private boolean crashed;
        /** The full-charge impact jolt plays once per throw. */
        private boolean shook;
        private final java.util.Set<Integer> hitOutbound = new java.util.HashSet<>();
        private final java.util.Set<Integer> hitReturning = new java.util.HashSet<>();

        private ThrowState(long startTick, int durationTicks, ItemStack weapon, WeaponTier tier,
                           int charge, Vec3d start, Vec3d end, Vec3d direction,
                           float baseDamage) {
            this.startTick = startTick;
            this.durationTicks = durationTicks;
            this.weapon = weapon;
            this.tier = tier;
            this.charge = charge;
            this.start = start;
            this.end = end;
            this.direction = direction;
            this.baseDamage = baseDamage;
            this.lastBallPosition = start;
        }
    }
}
