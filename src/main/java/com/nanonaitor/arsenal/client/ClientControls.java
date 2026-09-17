package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.ArsenalShieldItem;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.combat.ChainWeaponStats;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class ClientControls {
    private static boolean shieldAttackLatched;
    private static boolean scimitarBashLatched;
    private static boolean ballWasDown, ballWindBoostWasDown, flailWasDown, morningStarCharging;
    private static long lastFlailHeartbeat = Long.MIN_VALUE, lastHeartbeat = Long.MIN_VALUE;
    private static boolean ramLocked;
    private static float lockedYaw, lockedPitch;
    private static long flailVisualUntil = Long.MIN_VALUE, ballStarted = Long.MIN_VALUE,
        ballReleaseStarted = Long.MIN_VALUE, nextBallSwing = Long.MIN_VALUE;
    private static long lastMainClawAttack = Long.MIN_VALUE, lastOffhandClawAttack = Long.MIN_VALUE;
    private static long morningStarStarted = Long.MIN_VALUE, morningStarSwingStarted = Long.MIN_VALUE,
        lastMorningStarHeartbeat = Long.MIN_VALUE;
    private static boolean morningStarFullSwing;
    private static boolean mainClawWasDown, offhandClawWasDown;
    private static boolean clawEntityInteraction;
    private static boolean menuBulwarkGuard, scimitarNextOffhand;
    private static boolean offhandScimitarWasDown, bladeStaffWasDown;
    private static long lastScimitarAttack = Long.MIN_VALUE, lastBulwarkAttack = Long.MIN_VALUE,
        bladeStaffReflectUntil = Long.MIN_VALUE, lastBladeStaffAutoAttack = Long.MIN_VALUE;
    private static int releasedCharge, ballCharge, ballReleaseDuration = 16;
    private static double releasedDistance;
    private static Vec3 releasedDirection = Vec3.ZERO;
    private static ItemStack activeFlailSprite = ItemStack.EMPTY, activeBallSprite = ItemStack.EMPTY;

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ClientTooltip::requirements);
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(
            (net.minecraftforge.client.event.RegisterItemDecorationsEvent event) -> event.register(
                com.nanonaitor.arsenal.registry.ModItems.SUN_WAR.get(), (graphics,font,stack,x,y) -> {
                    int strain=stack.hasTag()?stack.getTag().getInt("ArsenalGuardStrain"):0;
                    if(strain>0) {
                        graphics.fill(x+2,y+10,x+15,y+12,0xFF000000);
                        graphics.fill(x+2,y+10,x+2+Math.max(1,Math.round(13F*strain/25F)),y+11,0xFFFFD23F);
                    }
                    return false;
                }));
        // Register every extensible enum before any humanoid switch table is built.
        WeaponClientExtensions.bootstrap();
        ShieldClientExtensions.bootstrap();
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(
            (net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(ClientModels::register));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> { if(e.phase==TickEvent.Phase.END) tick(e); });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> { if(interaction(e)) e.setCanceled(true); });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ClientWeaponRenderer::render);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ScimitarClientExtensions::beforeRender);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ClientWeaponRenderer::renderWorld);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
            net.minecraftforge.eventbus.api.EventPriority.LOWEST,true,
            net.minecraftforge.client.event.RenderHandEvent.class,ClientWeaponRenderer::renderFirstPerson);
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        ClientSmokeTest.tick();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) { clearFlailSprite(); clearBallSprite(); ballWasDown = false; flailWasDown = false;
            scimitarBashLatched = false;
            shieldAttackLatched = false;
            if (morningStarCharging) ModNetwork.send(ModNetwork.MORNING_STAR, false);
            ramLocked = false; mainClawWasDown = false; offhandClawWasDown = false;
            ballWindBoostWasDown = false;
            morningStarCharging = false; menuBulwarkGuard = false;
            offhandScimitarWasDown = false; bladeStaffReflectUntil = Long.MIN_VALUE; return; }
        boolean attack = minecraft.screen == null && minecraft.options.keyAttack.isDown();
        updateScimitarGuardModels(player);
        if (!attack) scimitarBashLatched = false;
        // Minecraft can consume attack clicks while using an item before firing
        // InteractionKeyMappingTriggered. Poll the bound key too, once per press.
        if (attack && (scimitarBashLatched
            || com.nanonaitor.arsenal.combat.ParityRules.guarding(player))) {
            requestScimitarBash(player);
            if (minecraft.gameMode != null) minecraft.gameMode.stopDestroyBlock();
            return;
        }
        if (com.nanonaitor.arsenal.combat.ParityRules.pair(player) && minecraft.gameMode != null)
            minecraft.gameMode.stopDestroyBlock();
        tickMenuBulwarkGuard(minecraft, player);
        long now = player.level().getGameTime();
        tickBladeStaffReflection(player, now);
        if (!attack) shieldAttackLatched = false;
        boolean shieldUsing = player.isUsingItem()
            && (player.getUseItem().getItem() instanceof net.minecraft.world.item.ShieldItem
                || player.getUseItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK));
        if (shieldUsing || shieldAttackLatched) {
            if (ballWasDown || morningStarCharging || ramLocked || flailWasDown)
                ModNetwork.send(ModNetwork.CANCEL_WEAPON_INPUTS, true);
            ballWasDown = false; ballWindBoostWasDown = false;
            morningStarCharging = false; ramLocked = false; flailWasDown = false;
            mainClawWasDown = false; offhandClawWasDown = false;
            clearFlailSprite();
            ballReleaseStarted=Long.MIN_VALUE;
            clearBallSprite();
            if (attack && shieldUsing && player.getUseItem().getItem() instanceof ArsenalShieldItem) {
                ArsenalShieldItem shield = (ArsenalShieldItem) player.getUseItem().getItem();
                ModNetwork.send(shield.shieldType() == ArsenalShieldItem.Type.TARTSY
                    ? ModNetwork.TARTSY_BASH : ModNetwork.BULWARK_BASH, true);
                shieldAttackLatched = true;
            }
            return;
        }
        tickOffhandWeapons(minecraft, player, attack);
        if(!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getMainHandItem(),
            attack || minecraft.options.keyUse.isDown())) {
            if(ballWasDown || flailWasDown || morningStarCharging || ramLocked)ModNetwork.send(ModNetwork.CANCEL_WEAPON_INPUTS,true);
            ballWasDown=ballWindBoostWasDown=flailWasDown=morningStarCharging=ramLocked=false;
            ballReleaseStarted=bladeStaffReflectUntil=Long.MIN_VALUE;
            clearBallSprite();clearFlailSprite();return;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)) {
            if (ballWasDown) ModNetwork.send(ModNetwork.BALL_CHAIN, false);
            if (ballWindBoostWasDown) ModNetwork.send(ModNetwork.BALL_WIND_BOOST, false);
            if (flailWasDown) ModNetwork.send(ModNetwork.FLAIL, false);
            if (ramLocked) ModNetwork.send(ModNetwork.RAM, false);
            if (morningStarCharging) ModNetwork.send(ModNetwork.MORNING_STAR, false);
            clearFlailSprite(); clearBallSprite();
            ballWasDown = false; ballWindBoostWasDown = false; flailWasDown = false; ramLocked = false;
            mainClawWasDown = false; offhandClawWasDown = false; morningStarCharging = false; return;
        }
        // Blade Staff uses ordinary attack input, not Arsenal auto-swing.
        if (weapon.kind() == WeaponKind.CLAWS) {
            tickClawAutoAttacks(minecraft, player, weapon, now);
        } else {
            mainClawWasDown = false;
            offhandClawWasDown = false;
        }
        boolean emptyOffhand = player.getOffhandItem().isEmpty();
        if (weapon.kind() == WeaponKind.MORNING_STAR) {
            if (attack && !morningStarCharging && player.getAttackStrengthScale(0.5F) >= 1.0F) {
                morningStarCharging = true;
                morningStarStarted = now;
                lastMorningStarHeartbeat = now;
                player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                ModNetwork.send(ModNetwork.MORNING_STAR, true);
            }
            if (morningStarCharging && attack
                && (now < lastMorningStarHeartbeat || now - lastMorningStarHeartbeat >= 2L)) {
                lastMorningStarHeartbeat = now;
                ModNetwork.send(ModNetwork.MORNING_STAR, true);
            }
            if (morningStarCharging && !attack) {
                morningStarFullSwing = morningStarChargeProgress(now) >= 1.0F;
                morningStarSwingStarted = now;
                morningStarCharging = false;
                player.stopUsingItem();
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                // The custom release bypasses Minecraft's normal local attack path,
                // so consume the client cooldown here as well as on the server.
                player.resetAttackStrengthTicker();
                ModNetwork.send(ModNetwork.MORNING_STAR, false);
            }
        } else if (morningStarCharging) {
            morningStarCharging = false;
            player.stopUsingItem();
            ModNetwork.send(ModNetwork.MORNING_STAR, false);
        }
        boolean flailDown = weapon.kind() == WeaponKind.FLAIL && attack
            && !isBlockingConventionalShield(player);
        updateFlailSprite(player.getMainHandItem(), flailDown);
        if (flailDown && !player.isUsingItem()) player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (flailDown && (lastFlailHeartbeat == Long.MIN_VALUE || now < lastFlailHeartbeat
            || now - lastFlailHeartbeat >= 2)) {
            lastFlailHeartbeat = now;
            flailVisualUntil = now + 3;
            ModNetwork.send(ModNetwork.FLAIL, true);
        }
        if (!flailDown && flailWasDown) {
            ModNetwork.send(ModNetwork.FLAIL, false);
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem active
                && active.kind() == WeaponKind.FLAIL) player.stopUsingItem();
        }
        flailWasDown = flailDown;
        if (weapon.kind() == WeaponKind.BALL_AND_CHAIN) {
            boolean releasing = within(now, ballReleaseStarted, ballReleaseDuration);
            boolean down = attack && !releasing && (!player.isUsingItem() || ballWasDown);
            boolean windBoost = down && emptyOffhand && minecraft.options.keyUse.isDown();
            if (windBoost != ballWindBoostWasDown) {
                ModNetwork.send(ModNetwork.BALL_WIND_BOOST, windBoost);
                if (windBoost) nextBallSwing = Math.min(nextBallSwing, now
                    + ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem(), true));
                ballWindBoostWasDown = windBoost;
            }
            if (down && !ballWasDown) {
                ballStarted = now;
                nextBallSwing = now;
                ballCharge = 0;
            }
            if (down && now >= nextBallSwing) {
                int maxCharges = com.nanonaitor.arsenal.combat.TierEffects.maxBallCharges(player,weapon.tier());
                ballCharge = Math.min(maxCharges, ballCharge + 1);
                nextBallSwing = now + ChainWeaponStats.swingIntervalTicks(player,
                    player.getMainHandItem(), windBoost);
            }
            if (down && (lastHeartbeat == Long.MIN_VALUE || now < lastHeartbeat || now - lastHeartbeat >= 2)) {
                lastHeartbeat = now; ModNetwork.send(ModNetwork.BALL_CHAIN, true);
            }
            if (!down && ballWasDown) {
                int maxCharges = com.nanonaitor.arsenal.combat.TierEffects.maxBallCharges(player,weapon.tier());
                releasedCharge = Math.max(1, Math.min(maxCharges, ballCharge));
                int effectiveCharge = com.nanonaitor.arsenal.combat.TierEffects.effectiveCharge(player,weapon.tier(),releasedCharge);
                Vec3 path = horizontalLongChainPath(player, player.getMainHandItem(),
                    ChainWeaponStats.ballThrowReach(player, player.getMainHandItem(), effectiveCharge));
                releasedDirection = path.normalize();
                releasedDistance = visibleThrowDistance(player, releasedDirection, path.length());
                ballReleaseDuration = ChainWeaponStats.ballReleaseAnimationTicks(player,
                    player.getMainHandItem());
                ballReleaseStarted = now;
                ModNetwork.send(ModNetwork.BALL_CHAIN, false);
            }
            updateBallSprite(player.getMainHandItem(), down
                || within(now, ballReleaseStarted, ballReleaseDuration));
            ballWasDown = down;
        } else if (ballWasDown) {
            ModNetwork.send(ModNetwork.BALL_CHAIN, false);
            if (ballWindBoostWasDown) ModNetwork.send(ModNetwork.BALL_WIND_BOOST, false);
            ballWasDown = false; ballWindBoostWasDown = false; clearBallSprite();
        } else {
            if (ballWindBoostWasDown) ModNetwork.send(ModNetwork.BALL_WIND_BOOST, false);
            ballWindBoostWasDown = false; clearBallSprite();
        }
        if (weapon.kind() == WeaponKind.BATTERING_RAM) {
            boolean charging = minecraft.screen == null && minecraft.options.keyUse.isDown()
                && emptyOffhand && (player.isCreative() || player.getFoodData().getFoodLevel() > 6);
            if (charging) {
                if (!ramLocked) {
                    ramLocked = true;
                    lockedYaw = player.getYRot();
                    lockedPitch = player.getXRot();
                    player.resetAttackStrengthTicker();
                }
                // Keep one continuous use state for the custom two-handed pose. The
                // server also owns this state, but starting it locally avoids a short
                // carry/charge flicker while its heartbeat packet makes the round trip.
                if (!player.isUsingItem()) player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                player.setYRot(lockedYaw); player.yRotO = lockedYaw; player.setXRot(lockedPitch); player.xRotO = lockedPitch;
                if (lastHeartbeat == Long.MIN_VALUE || now < lastHeartbeat || now - lastHeartbeat >= 2) {
                    lastHeartbeat = now; ModNetwork.send(ModNetwork.RAM, true);
                }
            } else {
                if (ramLocked) ModNetwork.send(ModNetwork.RAM, false);
                if (ramLocked && player.isUsingItem()
                    && player.getUseItem().getItem() instanceof ArsenalWeaponItem active
                    && active.kind() == WeaponKind.BATTERING_RAM) player.stopUsingItem();
                ramLocked = false;
            }
        } else {
            if (ramLocked) ModNetwork.send(ModNetwork.RAM, false);
            ramLocked = false;
        }
    }
    private static boolean interaction(InputEvent.InteractionKeyMappingTriggered event) {
        var current = Minecraft.getInstance().player;
        if(current != null && event.isUseItem()
            && current.getMainHandItem().getItem() instanceof ArsenalWeaponItem
            && (current.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
                || current.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK))) {
            if (!current.isUsingItem() || current.getUsedItemHand()!=InteractionHand.OFF_HAND) {
                ModNetwork.send(ModNetwork.CANCEL_WEAPON_INPUTS,true);
                var mode=Minecraft.getInstance().gameMode;
                if(mode!=null) mode.useItem(current,InteractionHand.OFF_HAND);
            }
            event.setSwingHand(false); return true;
        }
        if(current!=null && event.isUseItem() && isScimitar(current.getOffhandItem())
            && !isScimitar(current.getMainHandItem())
            && current.getMainHandItem().getItem() instanceof ArsenalWeaponItem) {
            event.setSwingHand(false); return true;
        }
        if (current != null && event.isAttack()
            && (scimitarBashLatched || com.nanonaitor.arsenal.combat.ParityRules.guarding(current))) {
            requestScimitarBash(current);
            event.setSwingHand(false); return true;
        }
        if (event.isAttack() && current != null && (shieldAttackLatched
            || (current.isUsingItem() && current.getUseItem().getItem() instanceof ArsenalShieldItem))) {
            event.setSwingHand(false);
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && minecraft.screen == null && event.isUseItem()
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem staff
            && com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getMainHandItem(),false)
            && staff.kind() == WeaponKind.BLADE_STAFF && player.getOffhandItem().isEmpty()
            && !player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())
            && !bladeStaffReflecting(player.level().getGameTime())) {
            bladeStaffReflectUntil = player.level().getGameTime() + 20L;
            player.startUsingItem(InteractionHand.MAIN_HAND);
            ModNetwork.send(ModNetwork.BLADE_STAFF_REFLECT, true);
            event.setSwingHand(false);
            return true;
        }
        if (player != null && minecraft.screen == null && event.isUseItem()
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem ball
            && ball.kind() == WeaponKind.BALL_AND_CHAIN
            && (ballWasDown || ballRelease(player.level().getGameTime()))) {
            event.setSwingHand(false);
            return true;
        }
        if (player != null && minecraft.screen == null && event.isAttack()) {
            ItemStack main = player.getMainHandItem(), off = player.getOffhandItem();
            boolean offhandBulwark = main.isEmpty() && off.getItem() instanceof ArsenalShieldItem shield
                && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR;
            boolean dualScimitars = isScimitar(main) && isScimitar(off);
            boolean tartsyGuard = player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalShieldItem tartsy
                && tartsy.shieldType() == ArsenalShieldItem.Type.TARTSY;
            boolean bladeReflect = bladeStaffReflecting(player.level().getGameTime());
            // A lone off-hand Scimitar is driven by right click and must not
            // consume the main hand's left click. This lets a Blade Staff (and
            // other ordinary main-hand weapons) retain normal melee attacks.
            if (offhandBulwark || dualScimitars || tartsyGuard || bladeReflect) {
                event.setSwingHand(false);
                return true;
            }
        }
        if (event.isAttack() && player != null && minecraft.screen == null
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem held
            && (held.kind() == WeaponKind.FLAIL || (held.kind() == WeaponKind.BATTERING_RAM && ramLocked)
                || held.kind() == WeaponKind.MORNING_STAR || held.kind() == WeaponKind.BALL_AND_CHAIN)) {
            // These held attacks are driven continuously from tick(). Suppress vanilla's
            // competing hand swing and block-mining animation; the Ram crushes blocks
            // through its forward path logic rather than striking each block normally.
            event.setSwingHand(false);
            return true;
        }
        if ((!event.isAttack() && !event.isUseItem()) || player == null || minecraft.screen != null
            || !(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.CLAWS) return false;
        boolean paired = hasMatchingLinkedClaw(player, weapon);
        // Left click always belongs to the main claw. Right click belongs to the
        // linked claw only when the matching generated partner is equipped; any
        // other offhand item keeps its normal use behavior.
        if (event.isUseItem() && !paired) return false;
        if (event.isUseItem() && tryClawEntityInteraction(minecraft, player)) {
            clawEntityInteraction = true;
            event.setSwingHand(false);
            return true;
        }
        // Both physical buttons are handled once per client tick below. Modern
        // Minecraft repeatedly emits use interactions while right click is held;
        // resolving attacks here made the linked claw strike and play whiff audio
        // every tick instead of respecting its weapon cooldown.
        event.setSwingHand(false);
        return true;
    }

    private static boolean tryClawEntityInteraction(Minecraft minecraft, LocalPlayer player) {
        if (!(minecraft.hitResult instanceof EntityHitResult hit) || minecraft.gameMode == null) {
            return false;
        }
        // Ask the actual entity interaction API first, so villagers and modded
        // mounts work without a hardcoded entity-class list. PASS remains a
        // linked-claw attack; any consumed result belongs to the entity.
        return minecraft.gameMode.interactAt(player, hit.getEntity(), hit,
            InteractionHand.MAIN_HAND).consumesAction();
    }

    private static void tickMenuBulwarkGuard(Minecraft minecraft, LocalPlayer player) {
        boolean menuOpen = minecraft.screen instanceof AbstractContainerScreen<?>;
        InteractionHand hand = bulwarkHandWithFreeOpposite(player);
        boolean shouldGuard = menuOpen && hand != null
            && !player.getCooldowns().isOnCooldown(player.getItemInHand(hand).getItem());
        if (shouldGuard == menuBulwarkGuard) return;
        menuBulwarkGuard = shouldGuard;
        if (shouldGuard) {
            player.startUsingItem(hand);
            ModNetwork.send(ModNetwork.BULWARK_MENU_GUARD, true);
        } else {
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalShieldItem shield
                && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR) player.stopUsingItem();
            ModNetwork.send(ModNetwork.BULWARK_MENU_GUARD, false);
        }
    }

    private static void requestScimitarBash(LocalPlayer player) {
        if (!scimitarBashLatched && com.nanonaitor.arsenal.combat.ParityRules.guarding(player)) {
            scimitarBashLatched = true;
            ModNetwork.send(ModNetwork.SCIMITAR_BASH,true);
        }
    }

    private static void tickOffhandWeapons(Minecraft minecraft, LocalPlayer player, boolean attack) {
        ItemStack main = player.getMainHandItem(), off = player.getOffhandItem();
        boolean ballWithOffhandScimitar = main.getItem() instanceof ArsenalWeaponItem ball
            && (ball.kind() == WeaponKind.BALL_AND_CHAIN || ball.kind() == WeaponKind.MORNING_STAR
                || ball.kind() == WeaponKind.FLAIL) && isScimitar(off);
        if (minecraft.screen != null || player.isUsingItem() && !ballWithOffhandScimitar) {
            offhandScimitarWasDown = minecraft.options.keyUse.isDown();
            return;
        }
        long now = player.level().getGameTime();

        if (main.isEmpty() && off.getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR && attack
            && elapsed(now, lastBulwarkAttack, 80.0D)) {
            player.swing(InteractionHand.OFF_HAND, true);
            ModNetwork.send(ModNetwork.BULWARK_ATTACK, true);
            lastBulwarkAttack = now;
            player.resetAttackStrengthTicker();
        }

        boolean dual = isScimitar(main) && isScimitar(off);
        if(dual && attack && (!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,main,true)
            || !com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,off,true)))return;
        if (dual && (minecraft.options.keyUse.isDown()
            || com.nanonaitor.arsenal.combat.ParityRules.disabled(player))) return;
        // A lone off-hand Scimitar is a complete weapon regardless of what the
        // main hand contains. Dual Scimitars retain their shared guard/alternation.
        boolean offhandOnly = !dual && isScimitar(off);
        boolean offhandButton = minecraft.options.keyUse.isDown();
        boolean request = dual ? attack : offhandOnly && offhandButton && !offhandScimitarWasDown;
        // A lone off-hand Scimitar owns its cooldown. It must remain usable
        // immediately before or after an unrelated main-hand attack.
        boolean ready = dual || offhandOnly;
        if (request && ready) {
            // The server adds the second Scimitar's speed to the shared attack
            // speed attribute. Alternate once per resulting combined cooldown.
            double cooldown = com.nanonaitor.arsenal.combat.ParityRules.interval(player,offhandOnly);
            if (elapsed(now, lastScimitarAttack, cooldown)) {
                boolean offhandAttack = offhandOnly || scimitarNextOffhand;
                if(!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,
                    offhandAttack?off:main,true)) { if(dual)scimitarNextOffhand=!scimitarNextOffhand; return; }
                player.swinging=false;
                player.swing(offhandAttack ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, true);
                ModNetwork.send(ModNetwork.SCIMITAR_ATTACK, offhandAttack);
                lastScimitarAttack = now;
                if (dual) scimitarNextOffhand = !scimitarNextOffhand;
                if (dual) player.resetAttackStrengthTicker();
            }
        }
        offhandScimitarWasDown = offhandButton;
    }

    private static void tickBladeStaffReflection(LocalPlayer player, long now) {
        if (!bladeStaffReflecting(now)) {
            if (bladeStaffReflectUntil != Long.MIN_VALUE && player.isUsingItem()
                && player.getUseItem().getItem() instanceof ArsenalWeaponItem weapon
                && weapon.kind() == WeaponKind.BLADE_STAFF) player.stopUsingItem();
            bladeStaffReflectUntil = Long.MIN_VALUE;
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BLADE_STAFF || !player.getOffhandItem().isEmpty()) {
            if(player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem active
                && active.kind()==WeaponKind.BLADE_STAFF)player.stopUsingItem();
            bladeStaffReflectUntil = Long.MIN_VALUE;
            return;
        }
        // Keep the visual active for the full one-second server-timed window;
        // releasing right click no longer releases the reflection early.
        if (!player.isUsingItem() || player.getUseItem() != player.getMainHandItem())
            player.startUsingItem(InteractionHand.MAIN_HAND);
    }

    public static boolean bladeStaffReflecting(long now) {
        return bladeStaffReflectUntil != Long.MIN_VALUE && now <= bladeStaffReflectUntil;
    }

    private static InteractionHand bulwarkHandWithFreeOpposite(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR
            && player.getOffhandItem().isEmpty()) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR
            && player.getMainHandItem().isEmpty()) return InteractionHand.OFF_HAND;
        return null;
    }

    private static boolean isScimitar(ItemStack stack) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.SCIMITAR;
    }

    private static void updateScimitarGuardModels(LocalPlayer player) {
        boolean guard = com.nanonaitor.arsenal.combat.ParityRules.guarding(player);
        if (com.nanonaitor.arsenal.combat.ParityRules.disabled(player) && isScimitar(player.getUseItem())) player.stopUsingItem();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isScimitar(stack)) stack.getOrCreateTag().putBoolean("ArsenalPaired",
                isScimitar(player.getMainHandItem()) && isScimitar(player.getOffhandItem())
                    && (stack==player.getMainHandItem() || stack==player.getOffhandItem()));
            if (isScimitar(stack)) setFlailFlag(stack,
                guard && stack == player.getOffhandItem());
        }
        if (isScimitar(player.getOffhandItem())) setFlailFlag(player.getOffhandItem(), guard);
    }

    private static double offhandScimitarAttackSpeed(LocalPlayer player) {
        double current = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double mainBase = 4.0D;
        if (player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main) {
            mainBase = Math.max(0.1D, 4.0D + main.kind().speedModifier);
        }
        double scimitarBase = 4.0D + WeaponKind.SCIMITAR.speedModifier;
        return current * scimitarBase / mainBase;
    }

    /** Long Chain contributes only to horizontal distance, never vertical reach. */
    private static Vec3 horizontalLongChainPath(LocalPlayer player, ItemStack stack, double configuredDistance) {
        double bonus = Math.min(configuredDistance - 0.01D,
            ChainWeaponStats.longChainBonus(player, stack));
        double baseDistance = Math.max(0.01D, configuredDistance - bonus);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) return look.scale(baseDistance);
        return look.scale(baseDistance).add(horizontal.normalize().scale(bonus));
    }
    private static void tickClawAutoAttacks(Minecraft minecraft, LocalPlayer player,
            ArsenalWeaponItem claws, long now) {
        double speed = Math.max(0.1D, player.getAttributeValue(Attributes.ATTACK_SPEED));
        double cooldown = 20.0D / speed;
        boolean paired = hasMatchingLinkedClaw(player, claws);
        boolean mainDown = minecraft.options.keyAttack.isDown();
        if (!minecraft.options.keyUse.isDown()) clawEntityInteraction = false;
        boolean offhandDown = paired && minecraft.options.keyUse.isDown()
            && !clawEntityInteraction;
        boolean fullyCharged = player.getAttackStrengthScale(0.5F) >= 1.0F;

        // A fresh press remains a normal manual attack and may be partially
        // charged. Continued holding only repeats after the hand is fully ready.
        if (mainDown && !mainClawWasDown) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            ModNetwork.send(ModNetwork.CLAW_MAIN, true);
            lastMainClawAttack = now;
            player.resetAttackStrengthTicker();
        } else if (mainDown && fullyCharged && elapsed(now, lastMainClawAttack, cooldown)) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            ModNetwork.send(ModNetwork.CLAW_MAIN, true);
            lastMainClawAttack = now;
            player.resetAttackStrengthTicker();
        }

        if (offhandDown && !offhandClawWasDown) {
            player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
            ModNetwork.send(ModNetwork.CLAW, true);
            lastOffhandClawAttack = now;
        } else if (offhandDown && elapsed(now, lastOffhandClawAttack, cooldown)) {
            player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
            ModNetwork.send(ModNetwork.CLAW, true);
            lastOffhandClawAttack = now;
        }
        mainClawWasDown = mainDown;
        offhandClawWasDown = offhandDown;
    }
    private static boolean elapsed(long now, long previous, double cooldown) {
        return previous == Long.MIN_VALUE || now < previous || now - previous + 0.5D >= cooldown;
    }
    private static boolean hasMatchingLinkedClaw(LocalPlayer player, ArsenalWeaponItem claws) {
        return player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked
            && linked.kind() == WeaponKind.LINKED_CLAWS && linked.tier() == claws.tier();
    }
    static boolean flailVisual(long now) { return now <= flailVisualUntil; }
    static boolean flailActive() { return flailWasDown; }
    static boolean ramActive() { return ramLocked; }
    static boolean morningStarCharging() { return morningStarCharging; }
    static float morningStarChargeProgress(long now) {
        if (!morningStarCharging || morningStarStarted == Long.MIN_VALUE) return 0.0F;
        double seconds = com.nanonaitor.arsenal.config.ArsenalConfig.MORNING_STAR_CHARGE_SECONDS.get();
        return (float)Math.min(1.0D, Math.max(0.0D, (now - morningStarStarted) / (seconds * 20.0D)));
    }
    static boolean morningStarSwing(long now) {
        return morningStarSwingStarted != Long.MIN_VALUE && now >= morningStarSwingStarted
            && now - morningStarSwingStarted < 7L;
    }
    static boolean morningStarFullSwing(long now) {
        return morningStarFullSwing && morningStarSwing(now);
    }
    static float morningStarSwingProgress(long now, float partial) {
        if (!morningStarSwing(now)) return 0.0F;
        return Math.min(1.0F, (now - morningStarSwingStarted + partial) / 7.0F);
    }
    static boolean ballWindup(long now) { return ballWasDown && !within(now, ballReleaseStarted, ballReleaseDuration); }
    static boolean ballWindBoost() { return ballWasDown && ballWindBoostWasDown; }
    static long ballStarted() { return ballStarted; }
    static boolean ballRelease(long now) { return within(now, ballReleaseStarted, ballReleaseDuration); }
    static long ballReleaseStarted() { return ballReleaseStarted; }
    static int ballReleaseDuration() { return ballReleaseDuration; }
    static int releasedCharge() { return releasedCharge; }
    static double releasedDistance() { return releasedDistance; }
    static Vec3 releasedDirection() { return releasedDirection; }
    private static boolean within(long now, long started, long duration) {
        return started != Long.MIN_VALUE && now >= started && now - started < duration;
    }
    private static double visibleThrowDistance(LocalPlayer player, Vec3 direction, double requested) {
        Vec3 start = player.getEyePosition();
        Vec3 normalized = direction.normalize();
        Vec3 side = new Vec3(-normalized.z, 0.0D, normalized.x);
        if (side.lengthSqr() > 0.0001D) side = side.normalize().scale(0.35D);
        Vec3[] offsets = { Vec3.ZERO, new Vec3(0.0D, -0.45D, 0.0D),
            new Vec3(0.0D, 0.35D, 0.0D), side, side.scale(-1.0D) };
        double closest = requested;
        for (Vec3 offset : offsets) {
            Vec3 rayStart = start.add(offset);
            BlockHitResult hit = player.level().clip(new ClipContext(rayStart,
                rayStart.add(normalized.scale(requested)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                closest = Math.min(closest, rayStart.distanceTo(hit.getLocation()));
            }
        }
        return closest;
    }
    private static boolean isBlockingConventionalShield(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack active = player.getUseItem();
        if (active.isEmpty() || active.getItem().getUseAnimation(active) != UseAnim.BLOCK) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(active.getItem());
        return id == null || !"defenders".equals(id.getNamespace());
    }
    private static void updateFlailSprite(ItemStack stack, boolean active) {
        if (!active) { clearFlailSprite(); return; }
        if (activeFlailSprite != stack) {
            clearFlailSprite();
            activeFlailSprite = stack;
        }
        setFlailFlag(stack, true);
    }
    private static void clearFlailSprite() {
        if (!activeFlailSprite.isEmpty()) setFlailFlag(activeFlailSprite, false);
        activeFlailSprite = ItemStack.EMPTY;
    }
    private static void updateBallSprite(ItemStack stack, boolean active) {
        if (!active) { clearBallSprite(); return; }
        if (activeBallSprite != stack) {
            clearBallSprite();
            activeBallSprite = stack;
        }
        setFlailFlag(stack, true);
    }
    private static void clearBallSprite() {
        if (!activeBallSprite.isEmpty()) setFlailFlag(activeBallSprite, false);
        activeBallSprite = ItemStack.EMPTY;
    }
    private static void setFlailFlag(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean("ArsenalActive", active);
    }
    private ClientControls() {}
}
