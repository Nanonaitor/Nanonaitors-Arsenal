package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalShield;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.ItemTartsyShield;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ShieldCombat {
    private static final String BASH_READY = "ArsenalBulwarkBashReady";
    private static final String MENU_GUARD = "ArsenalBulwarkMenuGuard";
    private static final UUID MOVEMENT_UUID = UUID.fromString("f223d9ba-b0ca-4ed4-bb87-58709fd6ea1e");
    private static final UUID OCCUPIED_ATTACK_UUID = UUID.fromString("82993c2a-aee0-44d2-a09e-19ba6d18df72");
    private static final UUID BALL_WIND_MOVEMENT_UUID = UUID.fromString("6b5c8592-c187-4a84-908f-54bea8445b03");
    private static final double VANILLA_USE_FACTOR = 0.20D;
    private static final double BULWARK_PASSIVE_REDUCTION = 0.15D;
    private static final int BASH_COOLDOWN_TICKS = 60;
    private static final Map<EntityPlayer, PendingBulwarkAttack> PENDING_ATTACKS =
        new WeakHashMap<>();
    private static final Map<EntityPlayer, TartsyDash> TARTSY_DASHES = new WeakHashMap<>();
    private static final String TARTSY_INVULNERABLE_UNTIL = "ArsenalTartsyInvulnerableUntil";
    private static final String TARTSY_CRITICAL_READY = "ArsenalTartsyCriticalReady";
    private static final int TARTSY_DISABLE_TICKS = 80;

    private ShieldCombat() {}

    public static boolean isGuarding(EntityPlayer player, Class<? extends ItemArsenalShield> type) {
        if(AbilityUseRules.cooling(player,findEquipped(player,type)))return false;
        return player.isHandActive() && type.isInstance(player.getActiveItemStack().getItem())
            || type == ItemSunWarBulwark.class && isBulwarkReady(player)
                && isBulwarkOffCooldown(player)
                && com.nanonaitor.arsenal.data.TransientData.of(player).getBoolean(MENU_GUARD);
    }

    /** Guard held by the bulwark while a menu is open (the use key cannot be held there). */
    public static boolean isMenuGuarding(net.minecraft.entity.Entity player) {
        return com.nanonaitor.arsenal.data.TransientData.of(player).getBoolean(MENU_GUARD);
    }

    public static void setMenuGuard(EntityPlayer player, boolean guarding) {
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean(MENU_GUARD, guarding);
    }

    /**
     * Ball and chain guard: holding use to guard, or the whole wind-up. Never
     * while the ball is out on a throw or coming back.
     *
     * The wind-up is read from the server's swing state, not the active hand.
     * During wind-up the client sends a stop-using packet every tick (the use
     * key is not held), and the server handles it before mobs attack, so the
     * active hand is briefly off exactly when hits land.
     */
    public static boolean isBallAndChainGuarding(EntityPlayer player) {
        if (!(player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain)
            || !player.getHeldItemOffhand().isEmpty()
            || BallAndChainCombat.isThrowing(player)) return false;
        return BallAndChainCombat.isWindingUp(player)
            || player.isHandActive() && player.getActiveHand() == EnumHand.MAIN_HAND
                && player.getActiveItemStack().getItem() instanceof ItemBallAndChain;
    }

    /**
     * Damage dealt by something (a mob, player, projectile, explosion or
     * lightning strike), fire attacks included; not a status effect or the world.
     */
    private static boolean isAttackHit(DamageSource source) {
        return !isEnvironmentDamage(source)
            && (source.getTrueSource() != null || source.getImmediateSource() != null
                || source.isProjectile() || source.isExplosion()
                || "lightningBolt".equals(source.damageType));
    }

    public static boolean isBulwarkReady(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark
            && player.getHeldItemOffhand().isEmpty()
            || player.getHeldItemOffhand().getItem() instanceof ItemSunWarBulwark
            && player.getHeldItemMainhand().isEmpty();
    }

    public static boolean isBulwarkOffCooldown(EntityPlayer player) {
        ItemStack stack = findEquipped(player, ItemSunWarBulwark.class);
        return stack.isEmpty() || !player.getCooldownTracker().hasCooldown(stack.getItem());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        DamageSource source = event.getSource();

        if (com.nanonaitor.arsenal.data.TransientData.of(player).getLong(TARTSY_INVULNERABLE_UNTIL)
            >= player.world.getTotalWorldTime()) {
            event.setCanceled(true);
            return;
        }

        // Tartsy is a one-hit emergency guard. Unlike directional shields it
        // cancels the next damage source outright, then immediately drops and
        // becomes unavailable for four seconds.
        if (isGuarding(player, ItemTartsyShield.class)) {
            event.setCanceled(true);
            ItemStack tartsy = findEquipped(player, ItemTartsyShield.class);
            tartsy.damageItem(1, player);
            triggerThorns(player,tartsy,event.getSource());
            player.getCooldownTracker().setCooldown(tartsy.getItem(),
                shieldCooldown(tartsy, TARTSY_DISABLE_TICKS));
            player.resetActiveHand();
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.25F);
            return;
        }

        // The ball and chain guard stops every attack from any direction and
        // cannot be broken. Every block wears the weapon like the scimitar
        // cross-guard does and rings out a metal clang.
        if (isBallAndChainGuarding(player) && isAttackHit(source)) {
            event.setCanceled(true);
            if (!player.world.isRemote) {
                ItemStack ball = player.getHeldItemMainhand();
                triggerThorns(player, ball, source);
                ball.damageItem(GuardRules.scimitarWear(event.getAmount()), player);
                playBallGuardClang(player);
                knockBackMeleeAttacker(player, ball, source);
            }
            return;
        }

        if (isGuarding(player, ItemSunWarBulwark.class) && isBulwarkReady(player) && isBulwarkOffCooldown(player)
            && isBulwarkCombatDamage(source)) {
            event.setCanceled(true);
            findEquipped(player, ItemSunWarBulwark.class).damageItem(1, player);
            triggerThorns(player,findEquipped(player, ItemSunWarBulwark.class),source);
            com.nanonaitor.arsenal.item.ItemSunWarBulwark.recordBlock(player,
                findEquipped(player, ItemSunWarBulwark.class));
            playBlock(player, 0.65F);
        }
    }

    /**
     * Knockback on a Ball and Chain also shoves back a melee attacker whose hit the
     * guard blocks (no damage), like a shield bash with the ball.
     */
    private static void knockBackMeleeAttacker(EntityPlayer defender, ItemStack ball, DamageSource source) {
        if (!(source.getTrueSource() instanceof EntityLivingBase) || source.isProjectile()
            || source.getImmediateSource() != source.getTrueSource()) return;
        int level = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
            net.minecraft.init.Enchantments.KNOCKBACK, ball);
        if (level <= 0) return;
        EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
        attacker.knockBack(defender, guardKnockback(level),
            defender.posX - attacker.posX, defender.posZ - attacker.posZ);
    }

    /** Push strength for a Knockback level on the Ball and Chain guard (0 = none). */
    public static float guardKnockback(int level) {
        return level <= 0 ? 0.0F : 0.4F + 0.5F * level;
    }

    private static void triggerThorns(EntityPlayer defender,ItemStack shield,DamageSource source) {
        if(defender.world.isRemote || !(source.getTrueSource() instanceof EntityLivingBase))return;
        int level=net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.THORNS,shield);
        if(level>0)net.minecraft.init.Enchantments.THORNS.onUserHurt(defender,(EntityLivingBase)source.getTrueSource(),level);
    }

    /** Run after NORMAL-priority Counter Attack, rather than hiding the hit from it. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void scimitarGuard(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getAmount() <= 0) return;
        EntityPlayer player=(EntityPlayer)event.getEntityLiving();
        ItemStack main=player.getHeldItemMainhand(),off=player.getHeldItemOffhand();
        if (!player.isHandActive() || !(player.getActiveItemStack().getItem() instanceof ItemScimitar)
            || !(main.getItem() instanceof ItemScimitar) || !(off.getItem() instanceof ItemScimitar)
            || player.getCooldownTracker().hasCooldown(main.getItem()) || player.getCooldownTracker().hasCooldown(off.getItem())
            || player.getItemInUseMaxCount() < com.nanonaitor.arsenal.compat.ScimitarShieldCompat.raiseDelay()
            || event.getSource().isUnblockable() || !isFrontDamage(player,event.getSource())) return;
        event.setCanceled(true);
        com.nanonaitor.arsenal.compat.ScimitarShieldCompat.blocked(player,event.getSource(),event.getAmount());
        ScimitarCounter.blocked(player);
        if (!player.world.isRemote) {
            int wear=com.nanonaitor.arsenal.combat.GuardRules.scimitarWear(event.getAmount());
            main.damageItem(wear,player);
            off.damageItem(wear,player);
            if (main.isEmpty() || off.isEmpty()) player.resetActiveHand();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDisabledBulwarkAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark)) return;
        if (!player.world.isRemote && event.getTarget() instanceof EntityLivingBase) {
            // Attacking means no menu is open, so a menu guard cannot still apply.
            setMenuGuard(player, false);
            PENDING_ATTACKS.put(player, new PendingBulwarkAttack(
                event.getTarget().getEntityId(), player.world.getTotalWorldTime(),
                WeaponHits.chargeMultiplier(player)));
        }
    }

    /** A player's own melee swing, as opposed to Thorns, projectiles or ability damage. */
    private static boolean isDirectPlayerMelee(DamageSource source) {
        return source.getTrueSource() instanceof EntityPlayer
            && source.getImmediateSource() == source.getTrueSource()
            && "player".equals(source.damageType);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (isDirectPlayerMelee(source)) {
            EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
            if (com.nanonaitor.arsenal.data.TransientData.of(attacker).getBoolean(TARTSY_CRITICAL_READY)) {
                com.nanonaitor.arsenal.data.TransientData.of(attacker).setBoolean(TARTSY_CRITICAL_READY, false);
                if (!WeaponHits.isVanillaCritical(attacker)) event.setAmount(event.getAmount() * 1.5F);
                attacker.onCriticalHit(event.getEntityLiving());
                attacker.world.playSound(null, attacker.posX, attacker.posY, attacker.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
        applyBulwarkPassiveReduction(event);
    }

    // Establish base damage before LevelUp/SME alter it; never overwrite their critical bonus.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void bulwarkBaseDamage(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        // Only the bulwark's own swing is armor-scaled; Thorns and other
        // secondary damage the holder causes keeps its normal amount.
        if (isDirectPlayerMelee(source)) {
            EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
            if (isBulwarkReady(attacker)) {
                PendingBulwarkAttack pending = PENDING_ATTACKS.remove(attacker);
                float multiplier = pending != null
                    && pending.targetId == event.getEntityLiving().getEntityId()
                    && attacker.world.getTotalWorldTime() - pending.worldTime <= 1L
                    ? pending.chargeMultiplier : WeaponHits.chargeMultiplier(attacker);
                event.setAmount(armorScaledDamage(attacker) * multiplier);
            }
        }

    }

    private static void applyBulwarkPassiveReduction(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!findEquipped(player, ItemSunWarBulwark.class).isEmpty() && !isVoid(source)) {
            event.setAmount(event.getAmount() * (float)(1.0D - BULWARK_PASSIVE_REDUCTION));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase == TickEvent.Phase.START) {
            // A menu guard ends for good once the bulwark is no longer ready,
            // so re-equipping it later cannot revive a stale guard.
            if (!player.world.isRemote && isMenuGuarding(player)
                && (!isBulwarkReady(player) || !isBulwarkOffCooldown(player))) {
                setMenuGuard(player, false);
            }
            if (com.nanonaitor.arsenal.compat.ScimitarShieldCompat.disabled(player)
                && player.isHandActive()) {
                player.resetActiveHand();
            }
            updateBulwarkMovement(player);
            updateOccupiedAttackSpeed(player);
            updateBallWindMovement(player);
            updateTartsyDash(player);
        }
    }

    public static void tartsyBash(EntityPlayer player) {
        if (player.world.isRemote || !isGuarding(player, ItemTartsyShield.class)) return;
        ItemStack shield = findEquipped(player, ItemTartsyShield.class);
        if (shield.isEmpty() || player.getCooldownTracker().hasCooldown(shield.getItem())) return;
        BallAndChainCombat.cancelAbility(player);
        ModernBackportCombat.cancelMorning(player);
        AbilityUseRules.lockWeaponAttacks(player, 20);
        net.minecraft.util.math.Vec3d look = player.getLookVec();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 0.001D) return;
        player.motionX = look.x / horizontal * 1.0125D;
        player.motionY = Math.max(player.motionY, 0.12D);
        player.motionZ = look.z / horizontal * 1.0125D;
        player.velocityChanged = true;
        com.nanonaitor.arsenal.data.TransientData.of(player).setLong(TARTSY_INVULNERABLE_UNTIL,
            player.world.getTotalWorldTime() + 19L);
        TARTSY_DASHES.put(player, new TartsyDash(player.world.getTotalWorldTime() + 19L));
        player.getCooldownTracker().setCooldown(shield.getItem(),
            shieldCooldown(shield, TARTSY_DISABLE_TICKS));
        player.resetActiveHand();
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9F, 1.35F);
    }

    private static void updateTartsyDash(EntityPlayer player) {
        if (player.world.isRemote) return;
        TartsyDash dash = TARTSY_DASHES.get(player);
        if (dash == null) return;
        long now = player.world.getTotalWorldTime();
        if (player.isDead || now > dash.endTick) {
            TARTSY_DASHES.remove(player);
            return;
        }
        AxisAlignedBB area = player.getEntityBoundingBox().grow(0.65D, 0.35D, 0.65D);
        for (EntityLivingBase target : player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, area, e -> CombatTargetRules.canHit(player, e))) {
            if (!dash.hit.add(target.getEntityId())) continue;
            if (target.attackEntityFrom(new EntityDamageSource("tartsy_dash", player), 2.0F)) {
                if (ModContent.STUNNED != null)
                    com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,
                        com.nanonaitor.arsenal.config.ArsenalConfig.effects.tartsyDash, 20, 0);
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean(TARTSY_CRITICAL_READY, true);
                target.knockBack(player, 0.55F, player.posX - target.posX,
                    player.posZ - target.posZ);
            }
        }
    }

    public static void bash(EntityPlayer player) {
        if (player.world.isRemote || !isBulwarkReady(player) || !isBulwarkOffCooldown(player)
            || !isGuarding(player, ItemSunWarBulwark.class)) return;
        long now = player.world.getTotalWorldTime();
        NBTTagCompound data = com.nanonaitor.arsenal.data.TransientData.of(player);
        if (now < data.getLong(BASH_READY)) return;
        ItemStack bulwark = findEquipped(player, ItemSunWarBulwark.class);
        if(AbilityUseRules.cooling(player,bulwark))return;
        int cooldown = shieldCooldown(bulwark, BASH_COOLDOWN_TICKS);
        data.setLong(BASH_READY, now + cooldown);

        AxisAlignedBB area = player.getEntityBoundingBox().grow(4.0D);
        // canHit checks isDead rather than isEntityAlive(): MmmMmmMmmMmm's dummy
        // reports itself as not alive while still being a valid damage target.
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(EntityLivingBase.class, area,
            target -> CombatTargetRules.canHit(player, target));
        float damage = armorScaledDamage(player);
        for (EntityLivingBase target : targets) {
            if (player.getDistanceSq(target) > 16.0D) continue;
            boolean hit = target.attackEntityFrom(
                new EntityDamageSource("arsenal_bulwark_bash", player), damage);
            if (hit) {
                findEquipped(player, ItemSunWarBulwark.class).damageItem(1, player);
                target.knockBack(player, 1.4F, player.posX - target.posX, player.posZ - target.posZ);
            }
        }
        // The guard is unbreakable: a bash only starts its own bash cooldown
        // (BASH_READY) and leaves the bulwark raised and able to block.
        player.swingArm(player.getHeldItemMainhand() == bulwark ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.2F, 0.65F);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 0.9F, 0.70F);
        player.resetCooldown();
    }

    /** Sun-War Bulwark damage: one plus the wielder's live armor value. */
    public static float armorScaledDamage(EntityPlayer player) {
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        // The live attribute includes armor items, potion modifiers, Baubles and
        // quality-system modifiers rather than only counting worn armor pieces.
        return 1.0F + (float) Math.max(0.0D, armor == null
            ? player.getTotalArmorValue() : armor.getAttributeValue());
    }

    private static int shieldCooldown(ItemStack shield, int baseTicks) {
        int ticks = baseTicks;
        if (com.nanonaitor.arsenal.config.ContentSwitches
            .enabledEnchantmentLevel(ModContent.RECOVERY, shield) > 0) {
            ticks = Math.max(1, ticks / 2);
        }
        if (com.nanonaitor.arsenal.config.ContentSwitches
            .enabledEnchantmentLevel(ModContent.BREECHED, shield) > 0) {
            ticks *= 2;
        }
        return ticks;
    }

    private static void updateBulwarkMovement(EntityPlayer player) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier old = speed.getModifier(MOVEMENT_UUID);
        ItemStack bulwark = findEquipped(player, ItemSunWarBulwark.class);
        if (bulwark.isEmpty()) {
            if (old != null) speed.removeModifier(old);
            return;
        }
        // Active item use multiplies movement input by 0.2 in 1.12.2. A +25%
        // attribute modifier therefore produces the intended 25% final speed.
        // Carrying remains a 40% penalty even with the opposite hand occupied;
        // only the stronger guarding state still requires both hands.
        double amount = isBulwarkReady(player)
            && isGuarding(player, ItemSunWarBulwark.class)
            ? (0.25D / VANILLA_USE_FACTOR) - 1.0D : -0.40D;
        // Replacing this modifier every tick forces Minecraft to recalculate
        // movement-based FOV every tick, producing a visible zoom pulse.
        if (old != null && Math.abs(old.getAmount() - amount) < 0.000001D) return;
        if (old != null) speed.removeModifier(old);
        speed.applyModifier(new AttributeModifier(MOVEMENT_UUID,
            "Sun-War Bulwark movement", amount, 2).setSaved(false));
    }

    private static void updateOccupiedAttackSpeed(EntityPlayer player) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        boolean occupiedBulwark = player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark
            && !player.getHeldItemOffhand().isEmpty()
            || player.getHeldItemOffhand().getItem() instanceof ItemSunWarBulwark
            && !player.getHeldItemMainhand().isEmpty();
        // Only touch the attribute when the state changes; re-applying every
        // tick marks it dirty and resends attribute packets to trackers.
        boolean present = speed.getModifier(OCCUPIED_ATTACK_UUID) != null;
        if (occupiedBulwark == present) return;
        if (present) speed.removeModifier(OCCUPIED_ATTACK_UUID);
        else speed.applyModifier(new AttributeModifier(OCCUPIED_ATTACK_UUID,
            "Occupied Sun-War Bulwark attack penalty", -0.50D, 2).setSaved(false));
    }

    private static void updateBallWindMovement(EntityPlayer player) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        boolean winding = player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain
            && com.nanonaitor.arsenal.data.TransientData.of(player).getBoolean("ArsenalBallWindBoost");
        boolean present = speed.getModifier(BALL_WIND_MOVEMENT_UUID) != null;
        if (winding == present) return;
        if (present) {
            speed.removeModifier(BALL_WIND_MOVEMENT_UUID);
        } else {
            speed.applyModifier(new AttributeModifier(BALL_WIND_MOVEMENT_UUID,
                "Ball and Chain fast winding movement", -0.20D, 2).setSaved(false));
        }
    }

    private static boolean isFrontDamage(EntityPlayer player, DamageSource source) {
        net.minecraft.util.math.Vec3d pos = source.getDamageLocation();
        if (pos == null) return false;
        net.minecraft.util.math.Vec3d incoming = pos.subtract(
            new net.minecraft.util.math.Vec3d(player.posX, player.posY, player.posZ));
        incoming = new net.minecraft.util.math.Vec3d(incoming.x, 0, incoming.z);
        net.minecraft.util.math.Vec3d look = player.getLookVec();
        look = new net.minecraft.util.math.Vec3d(look.x, 0, look.z);
        return incoming.lengthSquared() < 0.0001D || look.lengthSquared() < 0.0001D
            || incoming.normalize().dotProduct(look.normalize()) > 0.0D;
    }

    private static ItemStack findEquipped(EntityPlayer player, Class<? extends ItemArsenalShield> type) {
        if (type.isInstance(player.getHeldItemOffhand().getItem())) return player.getHeldItemOffhand();
        if (type.isInstance(player.getHeldItemMainhand().getItem())) return player.getHeldItemMainhand();
        return ItemStack.EMPTY;
    }

    private static final class TartsyDash {
        final long endTick;
        final Set<Integer> hit = new HashSet<>();
        TartsyDash(long endTick) { this.endTick = endTick; }
    }

    private static void playBlock(EntityPlayer player, float pitch) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, pitch);
    }

    /** Shield-block confirmation layered with a high anvil ring, heard by everyone nearby. */
    private static void playBallGuardClang(EntityPlayer player) {
        playBlock(player, 0.9F);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.45F,
            1.55F + player.getRNG().nextFloat() * 0.15F);
    }

    /** The Sun-War Bulwark stops every attack and, as before, magic damage too. */
    private static boolean isBulwarkCombatDamage(DamageSource source) {
        return isAttackHit(source) || !isEnvironmentDamage(source) && source.isMagicDamage();
    }

    /**
     * World and status damage that no Arsenal guard stops. Fire only counts
     * here when nothing dealt it (burning, lava, fire blocks): fireballs and
     * fire breath have an attacker and are blocked like any other attack.
     */
    private static boolean isEnvironmentDamage(DamageSource source) {
        if (isVoid(source)) return true;
        String type = source.damageType.toLowerCase(Locale.ROOT);
        if ("fall".equals(type) || "drown".equals(type) || "starve".equals(type)
            || "cactus".equals(type) || "inwall".equals(type) || "hotfloor".equals(type)
            || "flyintowall".equals(type) || "cramming".equals(type)) return true;
        return source.isFireDamage() && source.getTrueSource() == null
            && source.getImmediateSource() == null;
    }

    private static boolean isVoid(DamageSource source) {
        return source == DamageSource.OUT_OF_WORLD || "outOfWorld".equals(source.damageType);
    }

    private static final class PendingBulwarkAttack {
        private final int targetId;
        private final long worldTime;
        private final float chargeMultiplier;

        private PendingBulwarkAttack(int targetId, long worldTime,
                                     float chargeMultiplier) {
            this.targetId = targetId;
            this.worldTime = worldTime;
            this.chargeMultiplier = chargeMultiplier;
        }
    }
}
