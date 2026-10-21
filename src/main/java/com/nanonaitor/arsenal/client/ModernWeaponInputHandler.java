package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.network.ModernWeaponControlMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;
import net.minecraft.client.renderer.GlStateManager;

/** Client controls and lightweight poses for the modern-mechanics backport. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ModernWeaponInputHandler {
    private static boolean morning, menuGuard, ballBoost, attackWasDown;
    private static boolean offhandWasDown;
    private static long morningStarted = Long.MIN_VALUE, heartbeat = Long.MIN_VALUE;
    private static double nextScimitarTurnAt = Double.NEGATIVE_INFINITY;
    private static boolean nextScimitarOffhand, scimitarPressedThisTick;
    private static boolean cancelNativeMainhandSweep;
    private static boolean scimitarBashOwnsAttack;
    private static boolean temporaryScimitarUse;
    private static EntityPlayerSP rlCombatScimitarPlayer;
    private static ItemStack rlCombatHiddenScimitar = ItemStack.EMPTY;

    private ModernWeaponInputHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void mouse(MouseEvent event) {
        if (ShieldInputHandler.handleShieldAttack(event)) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        boolean dualScimitars = player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar;
        boolean dualScimitarsDisabled = dualScimitars
            && com.nanonaitor.arsenal.compat.ScimitarShieldCompat.disabled(player);
        int useKey = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode();
        boolean usePress = useKey < 0 && event.getButton() == useKey + 100;
        if (dualScimitars && usePress && event.isButtonstate() && !player.isHandActive()) {
            // RLCombat listens to canceled MouseEvents and otherwise interprets
            // right click as an offhand attack. Marking a Scimitar active before
            // its listener runs reserves this click from RLCombat. The marker is
            // removed at LOWEST priority so vanilla can still open containers,
            // use blocks and trade with entities before starting cross-guard.
            player.setActiveHand(EnumHand.MAIN_HAND);
            temporaryScimitarUse=true;
        }
        if (dualScimitars && usePress && event.isButtonstate()
            && Loader.isModLoaded("bettercombatmod")) {
            // RLCombat treats every right click with an offhand weapon as an
            // offhand strike, even when that click is raising this pair's guard
            // or using a block/entity. Hide only the client-side offhand for its
            // NORMAL-priority mouse listener, then restore it at LOWEST before
            // vanilla handles the actual right-click interaction.
            restoreRlCombatScimitar();
            rlCombatScimitarPlayer=player;
            rlCombatHiddenScimitar=player.getHeldItemOffhand();
            player.inventory.offHandInventory.set(0,ItemStack.EMPTY);
        }
        if (event.getButton() == 0 && player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar) {
            event.setCanceled(true);
            if (!event.isButtonstate()) {
                scimitarBashOwnsAttack=false;
                return;
            }
            if (scimitarBashOwnsAttack) return;
            if (dualScimitarsDisabled) {
                // The pair's shield cooldown disables both native and scheduled
                // attacks. This temporary active item also makes RLCombat's
                // receiveCanceled mouse listener decline the same left click.
                if(!player.isHandActive()) {
                    player.setActiveHand(EnumHand.MAIN_HAND);
                    temporaryScimitarUse=true;
                }
                return;
            }
            boolean guardIntent = useKey < 0 && Mouse.isButtonDown(useKey + 100);
            if (guardIntent && !player.isHandActive()) {
                player.setActiveHand(EnumHand.MAIN_HAND);
                temporaryScimitarUse=true;
            }
            if(event.isButtonstate() && Minecraft.getMinecraft().currentScreen==null
                && !guardIntent
                && !com.nanonaitor.arsenal.compat.ScimitarShieldCompat.isGuarding(player)) {
                scimitarPressedThisTick=true;
                Boolean attackedOffhand=attackNextReadyScimitar(
                    player,player.world.getTotalWorldTime());
                cancelNativeMainhandSweep=Boolean.TRUE.equals(attackedOffhand);
                // Arsenal owns this physical click. RLCombat listens to canceled
                // mouse events and would otherwise send a second main-hand packet
                // after Arsenal has already consumed that hand's cooldown. Its
                // duplicate therefore resolves at RLCombat's 20% minimum charge.
                if(!player.isHandActive()) {
                    player.setActiveHand(EnumHand.MAIN_HAND);
                    temporaryScimitarUse=true;
                }
            }
            return;
        }
        if (event.getButton() == 0
            && player.getHeldItemMainhand().getItem() instanceof ItemMorningStar
            && !isUsingShield(player)) {
            event.setCanceled(true);
            if (event.isButtonstate()) beginMorning(player);
            else releaseMorning(player);
        }
        if (event.getButton() == 1
            && player.getHeldItemMainhand().getItem() instanceof ItemMorningStar) {
            // Only actual ItemShield implementations get right-click priority.
            // Defenders and Scimitars intentionally do not count as shields here.
            if (player.getHeldItemOffhand().getItem() instanceof ItemShield) {
                if (morning) cancelMorningCharge(player);
            } else if (!(player.getHeldItemOffhand().getItem() instanceof ItemScimitar)) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getButton() == 1) {
            boolean ballWinding = player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain
                && player.getHeldItemOffhand().isEmpty()
                && !ShieldUsePriority.requested(player)
                && BallAndChainInputHandler.isSwinging()
                && !BallAndChainInputHandler.isGuardingInput();
            if (ballWinding) event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void afterBetterCombatMouse(MouseEvent event) {
        if (event.isButtonstate()) restoreRlCombatScimitar();
        if (temporaryScimitarUse && event.isButtonstate()) {
            EntityPlayerSP player=Minecraft.getMinecraft().player;
            if(player!=null && player.isHandActive()
                && player.getActiveItemStack().getItem() instanceof ItemScimitar)
                player.resetActiveHand();
            temporaryScimitarUse=false;
        }
        if(event.getButton()==0 && event.isButtonstate() && cancelNativeMainhandSweep) {
            com.nanonaitor.arsenal.compat.BetterCombatScimitarAnimation.cancelNativeMainhandSweep();
            cancelNativeMainhandSweep=false;
        }
    }

    // Vanilla reaches item-use only after block/entity interaction has passed.
    // Do not consume the raw mouse click: boats, villagers and other usable
    // entities must get their normal interaction before the offhand attack.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void auxiliaryScimitarUse(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        Minecraft mc=Minecraft.getMinecraft();
        EntityPlayerSP player=mc.player;
        if(player==null || event.getEntityPlayer()!=player || event.getHand()!=EnumHand.OFF_HAND
            || !(player.getHeldItemOffhand().getItem() instanceof ItemScimitar)
            || player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            || mainHandUseHasPriority(mc,player) || ShieldUsePriority.requested(player))return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);
        if(offhandWasDown)return;
        offhandWasDown=true;
        player.swingArm(EnumHand.OFF_HAND);
        int targetId=scimitarTargetId();
        if(!com.nanonaitor.arsenal.compat.ScimitarAttackBridge.sendNativeAttack(
                player,targetId,true))
            ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
                ModernWeaponControlMessage.SCIMITAR_ATTACK,true,targetId));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) { reset(); return; }
        long now = player.world.getTotalWorldTime();
        boolean attack = Mouse.isButtonDown(0);

        if (mc.currentScreen == null && attack && !attackWasDown
            && player.getHeldItemMainhand().isEmpty()
            && player.getHeldItemOffhand().getItem() instanceof ItemSunWarBulwark
            && !player.isHandActive()) {
            player.swingArm(EnumHand.OFF_HAND);
            ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
                ModernWeaponControlMessage.BULWARK_ATTACK, true));
        }

        boolean boost = mc.currentScreen == null && Mouse.isButtonDown(1)
            && Mouse.isButtonDown(0)
            && player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain
            && player.getHeldItemOffhand().isEmpty()
            && BallAndChainInputHandler.isSwinging()
            && !BallAndChainInputHandler.isGuardingInput()
            && player.getEntityData().getBoolean("ArsenalBallAndChainActive");
        // The world renderer runs client-side, so mirror the authoritative flag
        // locally as well as sending it to the server combat calculation.
        player.getEntityData().setBoolean("ArsenalBallWindBoost", boost);
        if (boost != ballBoost) {
            ballBoost = boost;
            ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
                ModernWeaponControlMessage.BALL_WIND_BOOST, boost));
        }

        if (mc.currentScreen == null
            && player.getHeldItemMainhand().getItem() instanceof ItemMorningStar
            && !isUsingShield(player)) {
            if (attack && !morning) beginMorning(player);
            if (morning && attack && (heartbeat == Long.MIN_VALUE || now - heartbeat >= 2L)) {
                heartbeat = now;
                ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
                    ModernWeaponControlMessage.MORNING_STAR, true));
            }
            if (morning) player.getEntityData().setFloat("ArsenalMorningChargeVisual",
                morningCharge(player));
            if (morning && !attack) releaseMorning(player);
        } else if (morning) {
            cancelMorningCharge(player);
        }

        boolean dualScimitars = player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar;
        boolean auxiliaryScimitar = !(player.getHeldItemMainhand().getItem() instanceof ItemScimitar)
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar;
        boolean offhandButton = Mouse.isButtonDown(1);
        boolean disabled = com.nanonaitor.arsenal.compat.ScimitarShieldCompat.disabled(player);
        if (disabled && player.isHandActive()) player.resetActiveHand();
        boolean guard = dualScimitars && !disabled && (player.isHandActive() || offhandButton);
        player.getEntityData().setBoolean("ArsenalScimitarGuard", guard);
        if (mc.currentScreen == null && guard && attack && !attackWasDown
            && com.nanonaitor.arsenal.compat.ScimitarShieldCompat.isGuarding(player)) {
            scimitarBashOwnsAttack=true;
            ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
                ModernWeaponControlMessage.SCIMITAR_BASH, true));
        }
        if (mc.currentScreen == null && !scimitarBashOwnsAttack
            && !guard && !offhandButton && !disabled && dualScimitars
            && attack && attackWasDown && !scimitarPressedThisTick) {
            attackNextReadyScimitar(player,now);
        } else if (!dualScimitars) {
            nextScimitarTurnAt=Double.NEGATIVE_INFINITY;
            nextScimitarOffhand=false;
        }

        boolean shouldMenuGuard = mc.currentScreen != null && hasFreeHandBulwark(player);
        if (shouldMenuGuard) {
            player.getEntityData().setBoolean("ArsenalBulwarkMenuGuard", true);
        }
        if (shouldMenuGuard != menuGuard) {
            menuGuard = shouldMenuGuard;
            if (!menuGuard) player.getEntityData().setBoolean("ArsenalBulwarkMenuGuard", false);
            ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
                ModernWeaponControlMessage.BULWARK_MENU_GUARD, menuGuard));
        }
        attackWasDown = attack;
        offhandWasDown = offhandButton;
        scimitarPressedThisTick=false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void renderHand(RenderSpecificHandEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        if (BallAndChainAnimationHandler.isGuarding(player)) {
            event.setCanceled(true);
            if (event.getHand() == EnumHand.MAIN_HAND)
                WeaponPartRenderer.renderCarryOnShieldBall(player.getHeldItemMainhand());
            return;
        }
        if (morning && event.getHand() == EnumHand.MAIN_HAND
            && event.getItemStack().getItem() instanceof ItemMorningStar) {
            event.setCanceled(true);
            WeaponPartRenderer.renderChargingMorningStar(event.getItemStack(),
                morningCharge(player));
            return;
        }
        if (!com.nanonaitor.arsenal.compat.ScimitarShieldCompat.isGuarding(player)
            || !(event.getItemStack().getItem() instanceof ItemScimitar)) return;
        // Render both blades ourselves so vanilla cannot move one outside the
        // camera. They are mirrored into the classic crossed-sword guard.
        event.setCanceled(true);
        WeaponPartRenderer.renderBlockingScimitar(event.getItemStack(), event.getHand());
    }

    public static float morningCharge(EntityPlayerSP player) {
        if (!morning || morningStarted == Long.MIN_VALUE) return 0;
        return (float)Math.min(1.0D, (player.world.getTotalWorldTime() - morningStarted)
            / (ArsenalConfig.morningStar.fullChargeSeconds * 20.0D));
    }

    private static boolean hasFreeHandBulwark(EntityPlayerSP player) {
        return com.nanonaitor.arsenal.combat.ShieldCombat.isBulwarkOffCooldown(player)
            && (player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark
            && player.getHeldItemOffhand().isEmpty()
            || player.getHeldItemOffhand().getItem() instanceof ItemSunWarBulwark
            && player.getHeldItemMainhand().isEmpty());
    }

    private static void automaticScimitar(EntityPlayerSP player, boolean offhand) {
        player.isSwingInProgress=false;
        player.swingArm(offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
        com.nanonaitor.arsenal.compat.BetterCombatScimitarAnimation.play(player,offhand);
        int targetId=scimitarTargetId();
        // Do not send RLCombat's raw attack packet for paired automatic turns.
        // Its client and server cooldown counters can be a tick apart after the
        // alternating hand animation, causing the server to resolve an otherwise
        // ready main-hand turn at RLCombat's 20% minimum damage.  The Arsenal
        // packet applies the pair cadence server-side, restores the selected
        // hand to a full charge, then delegates the actual hit to RLCombat.
        ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
            ModernWeaponControlMessage.SCIMITAR_ATTACK,offhand,targetId));
    }

    private static int scimitarTargetId() {
        RayTraceResult hit=Minecraft.getMinecraft().objectMouseOver;
        return hit!=null && hit.typeOfHit==RayTraceResult.Type.ENTITY && hit.entityHit!=null
            ? hit.entityHit.getEntityId():-1;
    }

    private static void beginMorning(EntityPlayerSP player) {
        if (morning || Minecraft.getMinecraft().currentScreen != null
            || isUsingShield(player)
            || player.getCooledAttackStrength(0.5F) < 0.95F) return;
        morning = true;
        morningStarted = player.world.getTotalWorldTime();
        heartbeat = morningStarted;
        player.setActiveHand(EnumHand.MAIN_HAND);
        player.getEntityData().setBoolean("ArsenalMorningCharging", true);
        ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
            ModernWeaponControlMessage.MORNING_STAR, true));
    }

    private static void releaseMorning(EntityPlayerSP player) {
        if (!morning) return;
        morning = false;
        player.getEntityData().setBoolean("ArsenalMorningCharging", false);
        player.getEntityData().setFloat("ArsenalMorningChargeVisual", 0.0F);
        ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
            ModernWeaponControlMessage.MORNING_STAR, false));
        player.swingArm(EnumHand.MAIN_HAND);
        player.resetCooldown();
    }

    public static void cancelMorningCharge(EntityPlayerSP player) {
        if (!morning) return;
        morning = false;
        morningStarted = heartbeat = Long.MIN_VALUE;
        player.getEntityData().setBoolean("ArsenalMorningCharging", false);
        player.getEntityData().setFloat("ArsenalMorningChargeVisual", 0.0F);
        if (player.isHandActive()
            && player.getActiveItemStack().getItem() instanceof ItemMorningStar)
            player.resetActiveHand();
        ModNetwork.CHANNEL.sendToServer(new ModernWeaponControlMessage(
            ModernWeaponControlMessage.MORNING_CANCEL, false));
    }

    private static boolean isUsingShield(EntityPlayerSP player) {
        return ShieldUsePriority.requested(player)
            || com.nanonaitor.arsenal.combat.AbilityUseRules.cooling(player,player.getHeldItemMainhand());
    }

    private static boolean mainHandUseHasPriority(Minecraft minecraft,
                                                   EntityPlayerSP player) {
        ItemStack main = player.getHeldItemMainhand();
        if (main.getItem() instanceof ItemShield) return true;
        if (main.isEmpty()) return false;
        if (minecraft.objectMouseOver != null
            && minecraft.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) return true;
        // A Ball and Chain cannot guard or use its wind-up acceleration with
        // an occupied offhand. In that loadout, right-click remains available
        // to an off-hand Scimitar instead of being consumed by BLOCK action.
        if ((main.getItem() instanceof ItemBallAndChain
            || main.getItem() instanceof ItemMorningStar
            || main.getItem() instanceof com.nanonaitor.arsenal.item.ItemFlail
            || (main.getItem() instanceof com.nanonaitor.arsenal.item.ItemBatteringRam
                && !com.nanonaitor.arsenal.compat.ArsenalCompatManager.canUseTwoHanded(player)))
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar) return false;
        if (main.getItemUseAction() != EnumAction.NONE) return true;
        return !(main.getItem() instanceof ItemSword
            || main.getItem() instanceof ItemTool
            || main.getItem() instanceof com.nanonaitor.arsenal.item.ItemArsenalWeapon);
    }

    private static void reset() {
        morning = menuGuard = ballBoost = attackWasDown = offhandWasDown = false;
        morningStarted = heartbeat = Long.MIN_VALUE;
        nextScimitarTurnAt=Double.NEGATIVE_INFINITY;
        nextScimitarOffhand=scimitarPressedThisTick=cancelNativeMainhandSweep=false;
        scimitarBashOwnsAttack=false;
        temporaryScimitarUse=false;
        restoreRlCombatScimitar();
    }

    private static void restoreRlCombatScimitar() {
        if(rlCombatScimitarPlayer!=null && !rlCombatHiddenScimitar.isEmpty())
            rlCombatScimitarPlayer.inventory.offHandInventory.set(0,rlCombatHiddenScimitar);
        rlCombatScimitarPlayer=null;
        rlCombatHiddenScimitar=ItemStack.EMPTY;
    }

    private static Boolean attackNextReadyScimitar(EntityPlayerSP player,long now) {
        boolean preferred=nextScimitarOffhand;
        if(scimitarTurnReady(player,now)
            && com.nanonaitor.arsenal.compat.ScimitarAttackBridge.ready(player,preferred)) {
            automaticScimitar(player,preferred);
            double interval=com.nanonaitor.arsenal.compat.ScimitarAttackBridge.pairedDamageInterval(player);
            nextScimitarTurnAt=com.nanonaitor.arsenal.combat.GuardRules.nextTurnDeadline(
                nextScimitarTurnAt,now,interval);
            nextScimitarOffhand=!preferred;
            return preferred;
        }
        return null;
    }

    private static boolean scimitarTurnReady(EntityPlayerSP player,long now) {
        return now+0.5D>=nextScimitarTurnAt;
    }
}
