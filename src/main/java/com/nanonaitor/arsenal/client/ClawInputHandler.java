package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.ClawPairHandler;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.network.OffhandClawAttackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

/** Gives paired Claws deterministic controls without requiring RLCombat. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ClawInputHandler {
    private static EntityPlayerSP rlCombatPlayer;
    private static ItemStack rlCombatHiddenLinkedClaw = ItemStack.EMPTY;
    private static long lastMainhandAutoAttackTick = Long.MIN_VALUE;
    private static long lastOffhandAutoAttackTick = Long.MIN_VALUE;

    private ClawInputHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(MouseEvent event) {
        if (event.getButton() != 1 || !event.isButtonstate()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.currentScreen != null) return;
        ItemStack main = player.getHeldItemMainhand();
        if (!(main.getItem() instanceof ItemClaws)) return;
        ItemClaws claws = (ItemClaws)main.getItem();
        if (!ClawPairHandler.hasMatchingLinkedClaw(player, claws)) return;

        lastOffhandAutoAttackTick = player.world.getTotalWorldTime();

        // Consume the physical right-click before vanilla or RLCombat can turn it
        // into an item interaction or a second offhand attack.
        event.setCanceled(true);
        // RLCombat 1.12.2 subscribes at NORMAL with receiveCanceled=true, so
        // cancellation alone cannot stop its second, weaker offhand packet.
        // Hide the generated linked claw only for the remainder of this client
        // mouse event; Arsenal's custom packet still resolves against the real
        // server-side pair and the claw is restored at LOWEST priority below.
        if (Loader.isModLoaded("bettercombatmod")) {
            restoreRlCombatOffhand();
            rlCombatPlayer = player;
            rlCombatHiddenLinkedClaw = player.getHeldItemOffhand();
            player.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        }
        RayTraceResult hit = minecraft.objectMouseOver;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
            && hit.entityHit instanceof EntityLivingBase) {
            // Send the custom full-damage attack before the vanilla animation
            // packet. RLCombat identifies a preceding offhand swing as one of
            // its own weaker offhand attacks and can reduce the following hit.
            ModNetwork.CHANNEL.sendToServer(new OffhandClawAttackMessage(hit.entityHit.getEntityId()));
        }
        // Animate locally without sending vanilla's offhand-animation packet.
        // RLCombat can process that packet before our scheduled custom attack
        // and incorrectly classify the linked hit as a weaker generic offhand hit.
        player.swingingHand = EnumHand.OFF_HAND;
        player.swingProgressInt = -1;
        player.isSwingInProgress = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void afterMouse(MouseEvent event) {
        if (event.getButton() == 1 && event.isButtonstate()) {
            restoreRlCombatOffhand();
        }
    }

    /**
     * Native paired-claw auto-attacks. Everything Nunchaku already handles the
     * main hand because claws are swords, so Arsenal only supplies the main-hand
     * loop when that mod is absent. The linked claw always uses Arsenal's custom
     * packet so its synchronized damage, enchantments and durability stay intact.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        restoreRlCombatOffhand();
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.currentScreen != null) return;
        ItemStack main = player.getHeldItemMainhand();
        if (!(main.getItem() instanceof ItemClaws)) return;
        ItemClaws claws = (ItemClaws) main.getItem();
        if (!ClawPairHandler.hasMatchingLinkedClaw(player, claws)) return;

        long now = player.world.getTotalWorldTime();
        double cooldownTicks = 20.0D / claws.getDisplayedAttackSpeed();
        if (event.phase == TickEvent.Phase.END
            && isMainhandAttackHeld(minecraft)
            && player.getCooledAttackStrength(0.5F) >= 1.0F
            && minecraft.playerController != null
            && (lastMainhandAutoAttackTick == Long.MIN_VALUE
                || now - lastMainhandAutoAttackTick + 0.5D >= cooldownTicks)) {
            RayTraceResult hit = minecraft.objectMouseOver;
            // Always animate a fully charged held attack, including whiffs.
            // Everything Nunchaku supplies the actual main-hand attack when
            // installed; Arsenal supplies it in standalone profiles.
            player.swingArm(EnumHand.MAIN_HAND);
            if (!Loader.isModLoaded("everythingnunchaku")
                && hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
                && hit.entityHit instanceof EntityLivingBase) {
                minecraft.playerController.attackEntity(player, hit.entityHit);
            }
            lastMainhandAutoAttackTick = now;
        }

        boolean offhandHeld = isOffhandAttackHeld(minecraft);
        if (event.phase == TickEvent.Phase.END && offhandHeld) {
            if (lastOffhandAutoAttackTick == Long.MIN_VALUE
                || now - lastOffhandAutoAttackTick + 0.5D >= cooldownTicks) {
                RayTraceResult hit = minecraft.objectMouseOver;
                if (hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
                    && hit.entityHit instanceof EntityLivingBase) {
                    ModNetwork.CHANNEL.sendToServer(
                        new OffhandClawAttackMessage(hit.entityHit.getEntityId()));
                }
                player.swingingHand = EnumHand.OFF_HAND;
                player.swingProgressInt = -1;
                player.isSwingInProgress = true;
                lastOffhandAutoAttackTick = now;
            }
        }

        // Prevent Everything Nunchaku/RLCombat from issuing a second generic
        // weaker-offhand hit beside Arsenal's packet. Keep the item visible when
        // idle so synchronization does not cause first-person jitter.
        if (Loader.isModLoaded("everythingnunchaku") && offhandHeld) {
            rlCombatPlayer = player;
            rlCombatHiddenLinkedClaw = player.getHeldItemOffhand();
            player.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        restoreRlCombatOffhand();
    }

    private static boolean isMainhandAttackHeld(Minecraft minecraft) {
        if (minecraft.gameSettings.keyBindAttack.isKeyDown()) {
            return true;
        }
        int keyCode = minecraft.gameSettings.keyBindAttack.getKeyCode();
        int mouseButton = keyCode + 100;
        return keyCode < 0 && mouseButton >= 0
            && mouseButton < Mouse.getButtonCount()
            && Mouse.isButtonDown(mouseButton);
    }

    private static boolean isOffhandAttackHeld(Minecraft minecraft) {
        // A canceled Forge MouseEvent can leave KeyBinding#isKeyDown false in
        // the RLCombat + Everything Nunchaku input path even while the physical
        // button remains held. Read a mouse-bound use key directly as a fallback;
        // keyboard/remapped bindings continue to use Minecraft's normal state.
        if (minecraft.gameSettings.keyBindUseItem.isKeyDown()) {
            return true;
        }
        int keyCode = minecraft.gameSettings.keyBindUseItem.getKeyCode();
        int mouseButton = keyCode + 100;
        return keyCode < 0 && mouseButton >= 0
            && mouseButton < Mouse.getButtonCount()
            && Mouse.isButtonDown(mouseButton);
    }

    private static void restoreRlCombatOffhand() {
        if (rlCombatPlayer != null && !rlCombatHiddenLinkedClaw.isEmpty()) {
            rlCombatPlayer.inventory.offHandInventory.set(0, rlCombatHiddenLinkedClaw);
        }
        rlCombatPlayer = null;
        rlCombatHiddenLinkedClaw = ItemStack.EMPTY;
    }
}
