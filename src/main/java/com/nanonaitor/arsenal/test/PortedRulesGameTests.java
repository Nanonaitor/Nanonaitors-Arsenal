package com.nanonaitor.arsenal.test;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.combat.*;
import com.nanonaitor.arsenal.compat.WorldEquipmentCompat;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Rules ported from Arsenal 1.12.2 2.0.21-2.0.26. */
@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortedRulesGameTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h, String name) {
        var p = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        p.setYRot(0); p.setXRot(0); p.setYHeadRot(0);
        return p;
    }

    private static net.minecraft.world.entity.monster.Zombie dummy(GameTestHelper h, int z) {
        var target = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, z));
        target.setNoAi(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        return target;
    }

    private static ItemStack weapon(WeaponKind kind, WeaponTier tier) {
        return new ItemStack(ModItems.get(kind, tier).get());
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void scimitarBlockCounterEmpowersNextBash(GameTestHelper h) {
        var p = player(h, "CounterTest");
        var main = weapon(WeaponKind.SCIMITAR, WeaponTier.IRON);
        var off = weapon(WeaponKind.SCIMITAR, WeaponTier.IRON);
        p.setItemSlot(EquipmentSlot.MAINHAND, main); p.setItemSlot(EquipmentSlot.OFFHAND, off);
        var attacker = dummy(h, 3);
        var target = dummy(h, 2);
        h.assertTrue(!ScimitarCounter.isCharged(p), "No counter before a block");
        p.startUsingItem(InteractionHand.MAIN_HAND);
        h.assertTrue(ParityRules.guarding(p), "Dual scimitars guard");
        boolean blocked = CombatEvents.onLivingAttack(new LivingAttackEvent(p, p.damageSources().mobAttack(attacker), 4));
        h.assertTrue(blocked && ScimitarCounter.isCharged(p), "A blocked hit readies the counter");
        float combined = (float)(ParityRules.attribute(p, main, Attributes.ATTACK_DAMAGE)
            + ParityRules.attribute(p, off, Attributes.ATTACK_DAMAGE));
        CombatEvents.handleControl(p, ModNetwork.SCIMITAR_BASH, true);
        float expected = combined * ArsenalConfig.SCIMITAR_COUNTER_MULTIPLIER.get().floatValue();
        h.assertTrue(Math.abs(target.getHealth() - (100 - expected)) < .02F,
            "Bash deals combined dual-wield damage x1.3, got " + (100 - target.getHealth()) + " expected " + expected);
        h.assertTrue(!ScimitarCounter.isCharged(p), "The counter is used up by one bash");
        h.assertTrue(Math.abs(ScimitarCounter.counterDamage(10, 1.3D) - 13) < .001F, "x1.3 multiplier");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void scimitarCounterFadesAndIgnoresOtherWeapons(GameTestHelper h) {
        var p = player(h, "CounterFade");
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        ScimitarCounter.blocked(p);
        h.assertTrue(!ScimitarCounter.isCharged(p), "Blocking without a scimitar gives no counter");
        p.setItemSlot(EquipmentSlot.OFFHAND, weapon(WeaponKind.SCIMITAR, WeaponTier.WOOD));
        ScimitarCounter.blocked(p);
        h.assertTrue(ScimitarCounter.isCharged(p), "A scimitar in either hand counts");
        var target = dummy(h, 2);
        TierEffects.hurtWithStack(p.getMainHandItem(), target, p.damageSources().playerAttack(p), 5);
        h.assertTrue(Math.abs(target.getHealth() - 95) < .01F && ScimitarCounter.isCharged(p),
            "A sword hit is not a scimitar hit and keeps the counter");
        long window = ArsenalConfig.SCIMITAR_COUNTER_SECONDS.get() * 20L;
        h.assertTrue(window == 100L, "Five second default window");
        p.getPersistentData().putLong("ArsenalScimitarCounterUntil", h.getLevel().getGameTime() - 1);
        h.assertTrue(!ScimitarCounter.isCharged(p), "The counter fades after its window");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void scimitarNoLongerWeakensByDefault(GameTestHelper h) {
        var p = player(h, "NoWeakness");
        var scimitar = weapon(WeaponKind.SCIMITAR, WeaponTier.DIAMOND);
        p.setItemSlot(EquipmentSlot.MAINHAND, scimitar);
        var target = dummy(h, 2);
        h.assertTrue(ArsenalConfig.EFFECTS.get("scimitarHitExtra").get().isEmpty(), "scimitarHitExtra is empty by default");
        h.assertTrue(!ArsenalConfig.EFFECTS.containsKey("scimitarHit"), "Old scimitarHit list renamed");
        TierEffects.hurtWithStack(scimitar, target, p.damageSources().playerAttack(p), 2);
        h.assertTrue(target.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS) == null, "No Weakness by default");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void ballGuardNeedsCrouchAndShovesWithKnockback(GameTestHelper h) {
        var p = player(h, "BallGuard");
        var ball = weapon(WeaponKind.BALL_AND_CHAIN, WeaponTier.IRON);
        ball.enchant(Enchantments.KNOCKBACK, 2);
        p.setItemSlot(EquipmentSlot.MAINHAND, ball);
        p.setShiftKeyDown(false);
        ball.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(!p.isUsingItem(), "Standing right-click does not guard");
        p.setShiftKeyDown(true);
        ball.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(p.isUsingItem() && CombatEvents.ballGuarding(p), "Crouch + right-click guards");
        var attacker = dummy(h, 2);
        Vec3 before = attacker.getDeltaMovement();
        boolean blocked = CombatEvents.onLivingAttack(new LivingAttackEvent(p, p.damageSources().mobAttack(attacker), 4));
        h.assertTrue(blocked, "The guard blocks the melee hit");
        h.assertTrue(attacker.getHealth() == 100, "The shove deals no damage");
        h.assertTrue(attacker.getDeltaMovement().z > before.z + 0.1D, "Knockback shoves the attacker away");
        h.assertTrue(CombatEvents.guardKnockback(0) == 0 && Math.abs(CombatEvents.guardKnockback(2) - 1.4F) < .001F,
            "Shove scales with the Knockback level");
        p.setShiftKeyDown(false);
        CombatEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
            net.minecraftforge.event.TickEvent.Phase.END, p));
        h.assertTrue(!p.isUsingItem(), "Standing up lowers the guard");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void ballThrowGeometry(GameTestHelper h) {
        for (int charge = 1; charge <= 3; charge++) {
            double visual = BallPath.visualBallRadius(charge);
            h.assertTrue(Math.abs(BallPath.hitRadius(charge) - visual * 1.3D) < 1e-9, "Hit radius is 1.3x the drawn ball");
            h.assertTrue(Math.abs(BallPath.crashRadius(charge) - visual * 0.8D) < 1e-9, "Crash radius is 0.8x the drawn ball");
            h.assertTrue(Math.abs(visual - BallPath.throwModelScale(charge) * 5.0D / 16.0D) < 1e-9, "Radius matches the drawn head");
        }
        h.assertTrue(BallPath.throwDuration(20, 6, 12) == 10, "A throw cut to half keeps its speed (half the time)");
        h.assertTrue(BallPath.throwDuration(20, 12, 12) == 20, "A clear throw keeps its time");
        h.assertTrue(BallPath.throwDuration(20, 0.2, 12) == 4, "Very short throws still take a few ticks");
        // A wall five blocks ahead: the ball stops with its surface at the wall face.
        for (int y = 0; y <= 3; y++) for (int x = -1; x <= 3; x++)
            h.setBlock(new BlockPos(x, y, 6), net.minecraft.world.level.block.Blocks.STONE);
        BlockPos base = h.absolutePos(new BlockPos(1, 1, 1));
        Vec3 origin = new Vec3(base.getX() + .5, base.getY() + 1.25, base.getZ() + .5);
        double radius = BallPath.crashRadius(2);
        double wallFace = h.absolutePos(new BlockPos(1, 1, 6)).getZ() - origin.z;
        double stop = BallPath.stopDistance(h.getLevel(), origin, new Vec3(0, 0, 1), 12, radius);
        h.assertTrue(Math.abs(stop - (wallFace - radius)) < .01D, "Stops with the ball surface at the wall: " + stop);
        h.assertTrue(BallPath.cutShort(stop, 12), "The wall cuts the throw short");
        h.assertTrue(BallPath.stopDistance(h.getLevel(), origin, new Vec3(0, 0, 1), 3, radius) == 3, "Clear path keeps its reach");
        h.assertTrue(Math.abs(BallPath.travel(4, 0.5D) - 4) < 1e-9 && BallPath.travel(4, 1.0D) < 1e-9, "Out and back");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void shieldTypeOffhandOwnsRightClick(GameTestHelper h) {
        var p = player(h, "ShieldPriority");
        var staff = weapon(WeaponKind.BLADE_STAFF, WeaponTier.IRON);
        p.setItemSlot(EquipmentSlot.MAINHAND, staff);
        for (ItemStack shield : new ItemStack[] {new ItemStack(ModItems.TARTSY_SHIELD.get()),
                new ItemStack(ModItems.SUN_WAR.get()), new ItemStack(Items.SHIELD)}) {
            p.setItemSlot(EquipmentSlot.OFFHAND, shield);
            h.assertTrue(ArsenalWeaponItem.offhandGuard(p), "Shield-type offhand detected: " + shield);
        }
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.TARTSY_SHIELD.get()));
        var ball = weapon(WeaponKind.BALL_AND_CHAIN, WeaponTier.IRON);
        p.setItemSlot(EquipmentSlot.MAINHAND, ball);
        p.setShiftKeyDown(true);
        var result = ball.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.PASS && !p.isUsingItem(),
            "The main-hand weapon passes the right-click to the shield");
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STICK));
        h.assertTrue(!ArsenalWeaponItem.offhandGuard(p), "Ordinary items are not guards");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void ballThrowEndKeepsOtherRaisedItem(GameTestHelper h) {
        var p = player(h, "DefenderStandIn");
        var ball = weapon(WeaponKind.BALL_AND_CHAIN, WeaponTier.IRON);
        p.setItemSlot(EquipmentSlot.MAINHAND, ball);
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BOW));
        p.getInventory().add(new ItemStack(Items.ARROW));
        CombatEvents.handleControl(p, ModNetwork.BALL_CHAIN, true);
        var tick = new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, p);
        CombatEvents.onPlayerTick(tick);
        // Another item is raised in the off-hand during the attack, as the client's
        // Defender hand-off does: release the weapon's use action, then raise it.
        h.assertTrue(p.isUsingItem() && p.getUsedItemHand() == InteractionHand.MAIN_HAND, "The wind-up uses the ball");
        p.releaseUsingItem();
        p.startUsingItem(InteractionHand.OFF_HAND);
        h.assertTrue(p.isUsingItem() && p.getUsedItemHand() == InteractionHand.OFF_HAND, "Off-hand item raised");
        CombatEvents.handleControl(p, ModNetwork.BALL_CHAIN, false);
        h.assertTrue(ball.getOrCreateTag().getBoolean("ArsenalBallThrown"), "The ball is thrown");
        h.assertTrue(p.isUsingItem() && p.getUsedItemHand() == InteractionHand.OFF_HAND, "Throwing keeps it raised");
        h.onEachTick(() -> CombatEvents.onPlayerTick(tick));
        h.runAfterDelay(60, () -> {
            h.assertTrue(!ball.getOrCreateTag().getBoolean("ArsenalBallThrown"), "The throw finished");
            h.assertTrue(p.isUsingItem() && p.getUsedItemHand() == InteractionHand.OFF_HAND,
                "The end of the throw keeps the raised off-hand item up (using=" + p.isUsingItem()
                    + " hand=" + p.getUsedItemHand() + " item=" + p.getUseItem() + ")");
            h.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void worldEquipmentKeepsDesignedGear(GameTestHelper h) {
        ItemStack plain = new ItemStack(Items.IRON_SWORD);
        h.assertTrue(!WorldEquipmentCompat.isSpecialStack(plain), "Plain sword can be swapped");
        plain.enchant(Enchantments.SHARPNESS, 1); plain.setDamageValue(5);
        h.assertTrue(!WorldEquipmentCompat.isSpecialStack(plain), "Enchanted, worn sword can be swapped");
        ItemStack named = new ItemStack(Items.IRON_SWORD);
        named.setHoverName(Component.literal("Stone Breaker"));
        h.assertTrue(WorldEquipmentCompat.isSpecialStack(named), "Named sword is kept");
        ItemStack unbreakable = new ItemStack(Items.IRON_SWORD);
        unbreakable.getOrCreateTag().putBoolean("Unbreakable", true);
        h.assertTrue(WorldEquipmentCompat.isSpecialStack(unbreakable), "Unbreakable sword is kept");
        ItemStack attributes = new ItemStack(Items.IRON_SWORD);
        attributes.addAttributeModifier(Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
            "legendary", 10, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
        h.assertTrue(WorldEquipmentCompat.isSpecialStack(attributes), "Attribute gear is kept");
        // Rolls at chance 1 directly: changing the config value here raced Forge's config file
        // watcher, which could reload the old value mid-test or keep the test value afterwards.
        var namedMob = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        namedMob.setCustomName(Component.literal("Bob"));
        namedMob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        WorldEquipmentCompat.equip(namedMob, 1.0D);
        h.assertTrue(namedMob.getMainHandItem().is(Items.IRON_SWORD), "Named mobs keep their weapon");
        var mob = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        WorldEquipmentCompat.equip(mob, 1.0D);
        h.assertTrue(mob.getMainHandItem().getItem() instanceof ArsenalWeaponItem, "Plain mob gear still becomes Arsenal");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void clawsRingEveryThirdHit(GameTestHelper h) {
        h.assertTrue(!ClawSounds.ringsOn(1) && !ClawSounds.ringsOn(2) && ClawSounds.ringsOn(3) && ClawSounds.ringsOn(6),
            "Ring on every third hit");
        var p = player(h, "ClawCombo");
        long a = ClawSounds.slash(p), b = ClawSounds.slash(p), c = ClawSounds.slash(p);
        h.assertTrue(a == 1 && b == 2 && c == 3, "Hits in one combo count up");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void bladeStaffUsesPoleRecipeShape(GameTestHelper h) {
        var recipe = h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(ArsenalMod.MOD_ID, "blade_staff_iron"));
        h.assertTrue(recipe.isPresent(), "Blade Staff recipe loads through its conditional form");
        boolean spartan = net.minecraftforge.fml.ModList.get().isLoaded("spartanweaponry");
        var handle = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            new net.minecraft.resources.ResourceLocation(spartan ? "spartanweaponry" : "minecraft", spartan ? "pole" : "stick"));
        h.assertTrue(recipe.get().getIngredients().stream().anyMatch(i -> i.test(new ItemStack(handle))),
            "Uses Spartan's Pole when installed, otherwise a stick");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void segmentDistanceMath(GameTestHelper h) {
        Vec3 o = Vec3.ZERO;
        h.assertTrue(Math.abs(CombatEvents.segmentDistanceSqr(o, new Vec3(0, 0, 4), new Vec3(-2, 0, 2), new Vec3(2, 0, 2))) < 1e-9,
            "Crossing segments touch");
        h.assertTrue(Math.abs(CombatEvents.segmentDistanceSqr(o, new Vec3(0, 0, 4), new Vec3(1, 0, 0), new Vec3(1, 0, 4)) - 1) < 1e-9,
            "Parallel segments one block apart");
        h.assertTrue(Math.abs(CombatEvents.segmentDistanceSqr(o, new Vec3(0, 0, 4), new Vec3(0, 3, 6), new Vec3(0, 3, 6)) - 13) < 1e-9,
            "Point beyond the end measures to the end");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge", timeoutTicks = 120)
    public static void thrownBallSmashesProjectiles(GameTestHelper h) {
        var p = player(h, "BallSmash");
        p.setItemSlot(EquipmentSlot.MAINHAND, weapon(WeaponKind.BALL_AND_CHAIN, WeaponTier.IRON));
        // Two slow arrows hanging in the throw's path: one from a mob, one the wielder's own.
        var foreign = h.spawn(EntityType.ARROW, new Vec3(1.5, 2.0, 3.5));
        var own = h.spawn(EntityType.ARROW, new Vec3(1.7, 2.0, 3.5));
        own.setOwner(p);
        for (var arrow : new net.minecraft.world.entity.projectile.Arrow[] {foreign, own}) {
            arrow.setNoGravity(true);
            arrow.setDeltaMovement(0.0D, 0.0D, 0.06D);
        }
        var tick = new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, p);
        CombatEvents.handleControl(p, ModNetwork.BALL_CHAIN, true);
        CombatEvents.onPlayerTick(tick);
        CombatEvents.handleControl(p, ModNetwork.BALL_CHAIN, false);
        h.onEachTick(() -> CombatEvents.onPlayerTick(tick));
        h.runAfterDelay(60, () -> {
            h.assertTrue(!foreign.isAlive(), "The thrown ball smashes a projectile in its path");
            h.assertTrue(own.isAlive(), "The wielder's own projectiles are left alone");
            h.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void windUpGuardsWithEmptyOffhand(GameTestHelper h) {
        var p = player(h, "WindUpGuard");
        p.setItemSlot(EquipmentSlot.MAINHAND, weapon(WeaponKind.BALL_AND_CHAIN, WeaponTier.IRON));
        var attacker = dummy(h, 2);
        var tick = new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, p);
        CombatEvents.handleControl(p, ModNetwork.BALL_CHAIN, true);
        CombatEvents.onPlayerTick(tick);
        h.assertTrue(CombatEvents.ballWindupGuards(p), "Winding up with an empty offhand guards");
        h.assertTrue(CombatEvents.onLivingAttack(new LivingAttackEvent(p, p.damageSources().mobAttack(attacker), 4)),
            "A hit from the front is blocked during the wind-up");
        var behind = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        h.assertTrue(!CombatEvents.onLivingAttack(new LivingAttackEvent(p, p.damageSources().mobAttack(behind), 4)),
            "A hit from behind is not");
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
        h.assertTrue(!CombatEvents.ballWindupGuards(p), "An occupied offhand gives no wind-up guard");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge", timeoutTicks = 60)
    public static void electricChainLightningHops(GameTestHelper h) {
        var p = player(h, "ChainLightning");
        var blade = weapon(WeaponKind.SCIMITAR, WeaponTier.ELECTRIC_DRAGONBONE);
        p.setItemSlot(EquipmentSlot.MAINHAND, blade);
        var first = dummy(h, 2);
        var second = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 2));
        var third = h.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 2));
        for (var z : new net.minecraft.world.entity.monster.Zombie[] {second, third}) {
            z.setNoAi(true);
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            z.setHealth(100);
        }
        var hops = ChainLightning.strike(p, first);
        h.assertTrue(hops.size() == 3 && hops.get(0) == first && hops.get(1) == second && hops.get(2) == third,
            "Lightning hops from the target to the nearest enemies in turn: " + hops.size());
        h.assertTrue(Math.abs(first.getHealth() - 95) < .01F && Math.abs(second.getHealth() - 96) < .01F
            && Math.abs(third.getHealth() - 97) < .01F, "5, 4, 3 damage per hop, through armor: "
                + first.getHealth() + " " + second.getHealth() + " " + third.getHealth());
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "The wielder is never struck");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void dragonforgeKeepsWeaponData(GameTestHelper h) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("iceandfire")) { h.succeed(); return; }
        var core = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
            new net.minecraft.resources.ResourceLocation("iceandfire", "dragonforge_fire_core"));
        h.setBlock(new BlockPos(1, 1, 1), core);
        var forge = (net.minecraft.world.Container)h.getBlockEntity(new BlockPos(1, 1, 1));
        var claws = weapon(WeaponKind.CLAWS, WeaponTier.DRAGONBONE);
        claws.setHoverName(Component.literal("Old Friend"));
        claws.enchant(Enchantments.SHARPNESS, 3);
        claws.setDamageValue(claws.getMaxDamage() / 2);
        forge.setItem(0, claws);
        forge.setItem(1, new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            new net.minecraft.resources.ResourceLocation("iceandfire", "fire_dragon_blood"))));
        try {
            forge.getClass().getMethod("smeltItem").invoke(forge);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        ItemStack out = forge.getItem(2);
        h.assertTrue(out.getItem() == ModItems.get(WeaponKind.CLAWS, WeaponTier.FLAMED_DRAGONBONE).get(),
            "The Dragonforge upgrades the claws: " + out);
        h.assertTrue(out.getHoverName().getString().equals("Old Friend"), "Name kept");
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out) == 3,
            "Enchantments kept");
        h.assertTrue(Math.abs((float)out.getDamageValue() / out.getMaxDamage() - 0.5F) < 0.01F, "Proportional wear kept");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "forge")
    public static void customMaterialsRegisterAndCraft(GameTestHelper h) {
        var custom = WeaponTier.activeValues().stream().filter(WeaponTier::custom).toList();
        if (custom.isEmpty()) { h.succeed(); return; } // only when a materials file lists some
        for (WeaponTier tier : custom) {
            for (WeaponKind kind : WeaponKind.values()) {
                var item = (ArsenalWeaponItem)ModItems.get(kind, tier).get();
                h.assertTrue(item.tier() == tier && item.kind() == kind, "Registered " + kind + " " + tier.id);
                if (kind == WeaponKind.LINKED_CLAWS) continue;
                var stack = new ItemStack(item);
                h.assertTrue(stack.getHoverName().getString().contains(tier.displayName()), "Named after the material: " + stack.getHoverName().getString());
                h.assertTrue(stack.getMaxDamage() == tier.material.getUses(), "Durability from the entry");
                var recipe = h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(
                    ArsenalMod.MOD_ID, "custom/" + kind.id + "_" + tier.id));
                h.assertTrue(recipe.isPresent(), "Generated recipe for " + kind.id + "_" + tier.id);
                h.assertTrue(recipe.get().getResultItem(h.getLevel().registryAccess()).getItem() == item, "Recipe makes the custom weapon");
                h.assertTrue(recipe.get().getIngredients().stream().noneMatch(i -> i.test(new ItemStack(Items.IRON_INGOT))
                    || i.test(new ItemStack(Items.IRON_BLOCK))), "Iron is swapped for the custom ingredient");
            }
        }
        h.succeed();
    }

    private PortedRulesGameTests() {}
}
