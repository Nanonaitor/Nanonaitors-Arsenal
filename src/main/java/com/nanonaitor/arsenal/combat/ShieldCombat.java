package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.compat.RaceWeaponAffinityCompat;
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
        return player.isHandActive() && type.isInstance(player.getActiveItemStack().getItem())
            || type == ItemSunWarBulwark.class && isBulwarkReady(player)
                && isBulwarkOffCooldown(player)
                && player.getEntityData().getBoolean("ArsenalBulwarkMenuGuard");
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

        if (player.getEntityData().getLong(TARTSY_INVULNERABLE_UNTIL)
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
            player.getCooldownTracker().setCooldown(tartsy.getItem(),
                shieldCooldown(tartsy, TARTSY_DISABLE_TICKS));
            player.resetActiveHand();
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.25F);
            return;
        }

        if (player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemBallAndChain
            && player.getHeldItemOffhand().isEmpty() && isFrontDamage(player, source)) {
            event.setCanceled(true);
            findEquipped(player, ItemSunWarBulwark.class).damageItem(1, player);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_METAL_PLACE, SoundCategory.PLAYERS, 1.125F, 0.90F);
            return;
        }
        if (player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemScimitar
            && player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar && isFrontDamage(player, source)) {
            event.setCanceled(true);
            player.getHeldItemMainhand().damageItem(1, player);
            player.getHeldItemOffhand().damageItem(1, player);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_METAL_PLACE, SoundCategory.PLAYERS, 1.125F, 0.90F);
            return;
        }

        if (isGuarding(player, ItemSunWarBulwark.class) && isBulwarkReady(player)
            && isBulwarkCombatDamage(source)) {
            event.setCanceled(true);
            findEquipped(player, ItemSunWarBulwark.class).damageItem(1, player);
            playBlock(player, 0.65F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDisabledBulwarkAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark)) return;
        if (!player.world.isRemote && event.getTarget() instanceof EntityLivingBase) {
            PENDING_ATTACKS.put(player, new PendingBulwarkAttack(
                event.getTarget().getEntityId(), player.world.getTotalWorldTime(),
                attackChargeMultiplier(player)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source.getTrueSource() instanceof EntityPlayer
            && source.getImmediateSource() == source.getTrueSource()
            && !"tartsy_dash".equals(source.damageType)) {
            EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
            if (attacker.getEntityData().getBoolean(TARTSY_CRITICAL_READY)) {
                attacker.getEntityData().setBoolean(TARTSY_CRITICAL_READY, false);
                if (!isVanillaCritical(attacker)) event.setAmount(event.getAmount() * 1.5F);
                attacker.onCriticalHit(event.getEntityLiving());
                attacker.world.playSound(null, attacker.posX, attacker.posY, attacker.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
            if (isBulwarkReady(attacker)) {
                PendingBulwarkAttack pending = PENDING_ATTACKS.remove(attacker);
                float multiplier = pending != null
                    && pending.targetId == event.getEntityLiving().getEntityId()
                    && attacker.world.getTotalWorldTime() - pending.worldTime <= 1L
                    ? pending.chargeMultiplier : attackChargeMultiplier(attacker);
                event.setAmount(armorScaledDamage(attacker) * multiplier);
            }
        }

        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (findEquipped(player, ItemSunWarBulwark.class) != ItemStack.EMPTY && !isVoid(source)) {
            event.setAmount(event.getAmount() * (float)(1.0D - BULWARK_PASSIVE_REDUCTION));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase == TickEvent.Phase.START) {
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
        net.minecraft.util.math.Vec3d look = player.getLookVec();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 0.001D) return;
        player.motionX = look.x / horizontal * 1.0125D;
        player.motionY = Math.max(player.motionY, 0.12D);
        player.motionZ = look.z / horizontal * 1.0125D;
        player.velocityChanged = true;
        player.getEntityData().setLong(TARTSY_INVULNERABLE_UNTIL,
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
                EntityLivingBase.class, area,
                e -> e != player && !e.isDead && !player.isOnSameTeam(e))) {
            if (!dash.hit.add(target.getEntityId())) continue;
            if (target.attackEntityFrom(new EntityDamageSource("tartsy_dash", player), 2.0F)) {
                if (ModContent.STUNNED != null)
                    com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,
                        com.nanonaitor.arsenal.config.ArsenalConfig.effects.tartsyDash, 20, 0);
                player.getEntityData().setBoolean(TARTSY_CRITICAL_READY, true);
                target.knockBack(player, 0.55F, player.posX - target.posX,
                    player.posZ - target.posZ);
            }
        }
    }

    private static boolean isVanillaCritical(EntityPlayer player) {
        return player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder()
            && !player.isInWater() && !player.isPotionActive(net.minecraft.init.MobEffects.BLINDNESS)
            && !player.isRiding() && !player.isSprinting();
    }

    public static void bash(EntityPlayer player) {
        if (player.world.isRemote || !isBulwarkReady(player)
            || !isGuarding(player, ItemSunWarBulwark.class)) return;
        long now = player.world.getTotalWorldTime();
        NBTTagCompound data = player.getEntityData();
        if (now < data.getLong(BASH_READY)) return;
        ItemStack bulwark = findEquipped(player, ItemSunWarBulwark.class);
        int cooldown = shieldCooldown(bulwark, BASH_COOLDOWN_TICKS);
        data.setLong(BASH_READY, now + cooldown);

        AxisAlignedBB area = player.getEntityBoundingBox().grow(4.0D);
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(EntityLivingBase.class, area,
            // MmmMmmMmmMmm's dummy reports isEntityAlive() as false while it
            // is still a valid damage target. Match the proven Ram behavior.
            target -> target != player && !target.isDead && !player.isOnSameTeam(target));
        float damage = armorScaledDamage(player) * attackChargeMultiplier(player);
        for (EntityLivingBase target : targets) {
            if (player.getDistanceSq(target) > 16.0D) continue;
            boolean hit;
            RaceWeaponAffinityCompat.beginAttack(bulwark);
            try {
                hit = target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
            } finally {
                RaceWeaponAffinityCompat.endAttack();
            }
            if (hit) {
                findEquipped(player, ItemSunWarBulwark.class).damageItem(1, player);
                target.knockBack(player, 1.4F, player.posX - target.posX, player.posZ - target.posZ);
            }
        }
        player.getCooldownTracker().setCooldown(bulwark.getItem(), cooldown);
        player.resetActiveHand();
        player.swingArm(player.getHeldItemMainhand() == bulwark ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.2F, 0.65F);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 0.9F, 0.70F);
        player.resetCooldown();
    }

    private static float armorScaledDamage(EntityPlayer player) {
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        // The live attribute includes armor items, potion modifiers, Baubles and
        // quality-system modifiers rather than only counting worn armor pieces.
        return 1.0F + (float) Math.max(0.0D, armor == null
            ? player.getTotalArmorValue() : armor.getAttributeValue());
    }

    private static int shieldCooldown(ItemStack shield, int baseTicks) {
        int ticks = baseTicks;
        if (ModContent.RECOVERY != null
            && EnchantmentHelper.getEnchantmentLevel(ModContent.RECOVERY, shield) > 0) {
            ticks = Math.max(1, ticks / 2);
        }
        if (ModContent.BREECHED != null
            && EnchantmentHelper.getEnchantmentLevel(ModContent.BREECHED, shield) > 0) {
            ticks *= 2;
        }
        return ticks;
    }

    private static float attackChargeMultiplier(EntityPlayer player) {
        float strength = player.getCooledAttackStrength(0.5F);
        return 0.2F + strength * strength * 0.8F;
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
        AttributeModifier old = speed.getModifier(OCCUPIED_ATTACK_UUID);
        if (old != null) speed.removeModifier(old);
        boolean occupiedBulwark = player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark
            && !player.getHeldItemOffhand().isEmpty()
            || player.getHeldItemOffhand().getItem() instanceof ItemSunWarBulwark
            && !player.getHeldItemMainhand().isEmpty();
        if (occupiedBulwark) speed.applyModifier(new AttributeModifier(OCCUPIED_ATTACK_UUID,
            "Occupied Sun-War Bulwark attack penalty", -0.50D, 2).setSaved(false));
    }

    private static void updateBallWindMovement(EntityPlayer player) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier old = speed.getModifier(BALL_WIND_MOVEMENT_UUID);
        if (old != null) speed.removeModifier(old);
        if (player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain
            && player.getEntityData().getBoolean("ArsenalBallWindBoost")) {
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

    private static boolean isBulwarkCombatDamage(DamageSource source) {
        if (isVoid(source) || source.isFireDamage()) return false;
        String type = source.damageType.toLowerCase(Locale.ROOT);
        if ("fall".equals(type) || "drown".equals(type) || "starve".equals(type)
            || "cactus".equals(type) || "inwall".equals(type) || "hotfloor".equals(type)) return false;
        return source.getTrueSource() != null || source.getImmediateSource() != null
            || source.isProjectile() || source.isExplosion() || source.isMagicDamage();
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
