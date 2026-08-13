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
    private static long lastEverythingNunchakuAttackTick = Long.MIN_VALUE;

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

        if (Loader.isModLoaded("everythingnunchaku")) {
            lastEverythingNunchakuAttackTick = player.world.getTotalWorldTime();
        }

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
     * Everything Nunchaku invokes RLCombat directly from its client tick handler
     * while the use button is held. Letting that path run beside Arsenal's paired
     * claw packet produces a second generic offhand hit, whose reduced damage may
     * consume the victim's invulnerability frames first. This bridge preserves
     * the mod's hold-to-attack control while routing each linked-claw strike
     * through Arsenal's full-damage/shared-enchantment implementation.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (!Loader.isModLoaded("everythingnunchaku")) return;

        // Everything Nunchaku listens without a phase filter. Hide the generated
        // partner during both START and END dispatches, then restore it at LOWEST.
        restoreRlCombatOffhand();
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.currentScreen != null) return;
        ItemStack main = player.getHeldItemMainhand();
        if (!(main.getItem() instanceof ItemClaws)) return;
        ItemClaws claws = (ItemClaws) main.getItem();
        if (!ClawPairHandler.hasMatchingLinkedClaw(player, claws)) return;

        if (event.phase == TickEvent.Phase.END && isOffhandAttackHeld(minecraft)) {
            long now = player.world.getTotalWorldTime();
            double cooldownTicks = 20.0D / claws.getDisplayedAttackSpeed();
            if (lastEverythingNunchakuAttackTick == Long.MIN_VALUE
                || now - lastEverythingNunchakuAttackTick + 0.5D >= cooldownTicks) {
                RayTraceResult hit = minecraft.objectMouseOver;
                if (hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
                    && hit.entityHit instanceof EntityLivingBase) {
                    ModNetwork.CHANNEL.sendToServer(
                        new OffhandClawAttackMessage(hit.entityHit.getEntityId()));
                }
                player.swingingHand = EnumHand.OFF_HAND;
                player.swingProgressInt = -1;
                player.isSwingInProgress = true;
                lastEverythingNunchakuAttackTick = now;
            }
        }

        rlCombatPlayer = player;
        rlCombatHiddenLinkedClaw = player.getHeldItemOffhand();
        player.inventory.offHandInventory.set(0, ItemStack.EMPTY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if (Loader.isModLoaded("everythingnunchaku")) {
            restoreRlCombatOffhand();
        }
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
