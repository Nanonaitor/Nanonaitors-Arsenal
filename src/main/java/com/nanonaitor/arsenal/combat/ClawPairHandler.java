package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemLinkedClaw;
import com.nanonaitor.arsenal.registry.ModContent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ClawPairHandler {
    private ClawPairHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        ItemStack main = player.getHeldItemMainhand();
        ItemStack offhand = player.getHeldItemOffhand();

        removeStrayLinkedClaws(player, offhand);

        if (main.getItem() instanceof ItemClaws) {
            ItemClaws claws = (ItemClaws) main.getItem();
            ItemLinkedClaw expected = ModContent.LINKED_CLAWS.get(claws.getTier());
            if (offhand.isEmpty() && expected != null) {
                offhand = new ItemStack(expected);
                syncLinkedClaw(main, offhand);
                player.setHeldItem(EnumHand.OFF_HAND, offhand);
            } else if (offhand.getItem() instanceof ItemLinkedClaw
                && offhand.getItem() != expected && expected != null) {
                offhand = new ItemStack(expected);
                syncLinkedClaw(main, offhand);
                player.setHeldItem(EnumHand.OFF_HAND, offhand);
            } else if (!(offhand.getItem() instanceof ItemLinkedClaw)) {
                claws.resetPair(main);
            }
            if (offhand.getItem() == expected) {
                syncLinkedClaw(main, offhand);
            }
        } else if (offhand.getItem() instanceof ItemLinkedClaw) {
            player.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> drop.getItem().getItem() instanceof ItemLinkedClaw);
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityItem)) {
            return;
        }
        EntityItem entityItem = (EntityItem) event.getEntity();
        if (entityItem.getItem().getItem() instanceof ItemLinkedClaw) {
            entityItem.setDead();
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }

    public static boolean hasMatchingLinkedClaw(EntityPlayer player, ItemClaws claws) {
        ItemStack offhand = player.getHeldItemOffhand();
        return offhand.getItem() instanceof ItemLinkedClaw
            && ((ItemLinkedClaw) offhand.getItem()).getTier() == claws.getTier();
    }

    private static void removeStrayLinkedClaws(EntityPlayer player, ItemStack allowedOffhand) {
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ItemLinkedClaw && stack != allowedOffhand) {
                player.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
            }
        }
        if (player.inventory.getItemStack().getItem() instanceof ItemLinkedClaw) {
            player.inventory.setItemStack(ItemStack.EMPTY);
        }
    }

    private static void syncLinkedClaw(ItemStack main, ItemStack linked) {
        linked.setItemDamage(main.getItemDamage());
        NBTTagCompound linkedTag = new NBTTagCompound();
        if (main.hasTagCompound()) {
            NBTTagCompound mainTag = main.getTagCompound();
            copyTag(mainTag, linkedTag, "ench");
            if (Loader.isModLoaded("qualitytools")) {
                copyTag(mainTag, linkedTag, "Quality");
            }
        }
        linked.setTagCompound(linkedTag.hasNoTags() ? null : linkedTag);
    }

    private static void copyTag(NBTTagCompound source, NBTTagCompound destination,
                                String key) {
        if (source.hasKey(key)) {
            destination.setTag(key, source.getTag(key).copy());
        }
    }
}
