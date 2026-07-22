package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.WeaponTier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.oredict.OreDictionary;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class BatteringRamCombat {
    private static final double CHARGE_SPEED = 0.38D;
    private static final int MINIMUM_FOOD_TO_CHARGE = 6;
    private static final float CHARGE_EXHAUSTION_PER_TICK = 0.08F;
    private static final float BLOCK_BREAK_EXHAUSTION = 0.15F;
    private static final float ENTITY_HIT_EXHAUSTION = 0.30F;
    private static final Map<EntityPlayer, ChargeState> CHARGES = new WeakHashMap<>();

    private BatteringRamCombat() {}

    public static void markCharging(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemBatteringRam)
            || !player.getHeldItemOffhand().isEmpty() || player.isSpectator()
            || !hasChargeEnergy(player)) {
            return;
        }
        ChargeState state = CHARGES.computeIfAbsent(player,
            ignored -> new ChargeState(player.rotationYaw, player.rotationPitch));
        state.lastHeartbeatTick = player.world.getTotalWorldTime();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
            || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        ChargeState state = CHARGES.get(player);
        ItemStack stack = player.getHeldItemMainhand();
        long now = player.world.getTotalWorldTime();
        boolean active = state != null && now - state.lastHeartbeatTick <= 3L
            && stack.getItem() instanceof ItemBatteringRam
            && player.getHeldItemOffhand().isEmpty() && player.isEntityAlive()
            && !player.isSpectator() && hasChargeEnergy(player);
        if (!active) {
            if (state != null) {
                state.hitEntities.clear();
                CHARGES.remove(player);
            }
            if (player.isHandActive() && stack.getItem() instanceof ItemBatteringRam) {
                player.resetActiveHand();
            }
            return;
        }

        if (!player.isHandActive()) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        player.rotationYaw = state.lockedYaw;
        player.rotationYawHead = state.lockedYaw;
        player.renderYawOffset = state.lockedYaw;
        player.rotationPitch = state.lockedPitch;
        double yaw = Math.toRadians(state.lockedYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        player.motionX = forwardX * CHARGE_SPEED;
        player.motionZ = forwardZ * CHARGE_SPEED;
        player.velocityChanged = true;
        if (!player.capabilities.isCreativeMode) {
            player.addExhaustion(CHARGE_EXHAUSTION_PER_TICK);
        }

        breakRamBlocks(player, stack, forwardX, forwardZ, state.lockedYaw);
        hitRamTargets(player, state, stack, forwardX, forwardZ);
    }

    private static void breakRamBlocks(EntityPlayerMP player, ItemStack stack,
                                       double forwardX, double forwardZ,
                                       float lockedYaw) {
        double rightX = forwardZ;
        double rightZ = -forwardX;
        Set<BlockPos> columns = new HashSet<>();
        double[] forwardSamples = {0.55D, 1.05D, 1.55D};
        double[] widthSamples = {-1.0D, -0.5D, 0.0D, 0.5D, 1.0D};
        for (double distance : forwardSamples) {
            for (double width : widthSamples) {
                columns.add(new BlockPos(player.posX + forwardX * distance + rightX * width,
                    player.posY + 0.5D,
                    player.posZ + forwardZ * distance + rightZ * width));
            }
        }
        EnumFacing facing = EnumFacing.getHorizontal(
            MathHelper.floor(lockedYaw * 4.0F / 360.0F + 0.5D) & 3);

        WeaponTier tier = ((ItemBatteringRam) stack.getItem()).getTier();
        for (BlockPos column : columns) {
            for (int vertical = 0; vertical <= 2 && !stack.isEmpty(); vertical++) {
                BlockPos pos = column.up(vertical);
                IBlockState blockState = player.world.getBlockState(pos);
                if (!isBreakableRamBlock(blockState, tier) || blockState.getBlockHardness(
                    player.world, pos) < 0.0F || !player.canPlayerEdit(pos, facing, stack)) {
                    continue;
                }
                if (player.world.destroyBlock(pos, true)) {
                    int previousDamage = stack.getItemDamage();
                    stack.damageItem(1, player);
                    if (!player.capabilities.isCreativeMode) {
                        if (!stack.isEmpty() && stack.getItemDamage() == previousDamage
                            && EnchantmentHelper.getEnchantmentLevel(
                                Enchantments.UNBREAKING, stack) == 0) {
                            stack.setItemDamage(previousDamage + 1);
                        }
                        player.addExhaustion(BLOCK_BREAK_EXHAUSTION);
                        player.inventory.markDirty();
                    }
                }
            }
        }
    }

    private static boolean isBreakableRamBlock(IBlockState state, WeaponTier tier) {
        Block block = state.getBlock();
        if (block.hasTileEntity(state)) {
            return false;
        }
        Material material = state.getMaterial();
        boolean softSoil = material == Material.GROUND || material == Material.GRASS
            || material == Material.SAND;
        if (softSoil) {
            return true;
        }
        boolean glass = material == Material.GLASS || block == Blocks.GLASS
            || block == Blocks.GLASS_PANE || block == Blocks.STAINED_GLASS
            || block == Blocks.STAINED_GLASS_PANE;
        if (glass) {
            return true;
        }
        String path = registryPath(block);
        boolean firedClay = material == Material.CLAY || block == Blocks.HARDENED_CLAY
            || block == Blocks.STAINED_HARDENED_CLAY || path.contains("terracotta")
            || path.contains("hardened_clay");
        boolean log = isOreDictionaryMatch(state, "logWood")
            || registryPath(block).contains("log");
        boolean craftedWood = material == Material.WOOD && !log;
        if (tier.getRamBreakLevel() >= 1 && craftedWood) {
            return true;
        }
        if (tier.getRamBreakLevel() >= 2 && log) {
            return true;
        }
        if (tier.getRamBreakLevel() >= 2 && (isCobblestoneDerivative(state) || firedClay)) {
            return true;
        }
        return tier.getRamBreakLevel() >= 3 && isOrdinaryStoneDerivative(state);
    }

    private static boolean isCobblestoneDerivative(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.COBBLESTONE || block == Blocks.STONE_STAIRS
            || block == Blocks.COBBLESTONE_WALL) {
            return true;
        }
        if ((block == Blocks.STONE_SLAB || block == Blocks.DOUBLE_STONE_SLAB)
            && state.getValue(BlockStoneSlab.VARIANT)
                == BlockStoneSlab.EnumType.COBBLESTONE) {
            return true;
        }
        String path = registryPath(block);
        if (path.contains("cobble")) {
            return true;
        }
        return isOreDictionaryNameContaining(state, "cobble");
    }

    private static boolean isOrdinaryStoneDerivative(IBlockState state) {
        Block block = state.getBlock();
        String path = registryPath(block);
        if (state.getMaterial() != Material.ROCK || path.contains("ore")
            || path.contains("obsidian")
            || path.contains("bedrock")) {
            return false;
        }
        return block == Blocks.STONE || block == Blocks.STONEBRICK
            || block == Blocks.STONE_BRICK_STAIRS
            || path.contains("stone") || path.contains("granite")
            || path.contains("diorite") || path.contains("andesite")
            || path.contains("rock");
    }

    private static String registryPath(Block block) {
        ResourceLocation registryName = block.getRegistryName();
        return registryName == null ? "" : registryName.getResourcePath().toLowerCase();
    }

    private static boolean isOreDictionaryMatch(IBlockState state, String exactName) {
        return hasOreName(state, name -> name.equals(exactName));
    }

    private static boolean isOreDictionaryNameContaining(IBlockState state,
                                                          String fragment) {
        return hasOreName(state, name -> name.toLowerCase().contains(fragment));
    }

    private static boolean hasOreName(IBlockState state,
                                      java.util.function.Predicate<String> predicate) {
        Block block = state.getBlock();
        net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
        if (item == Items.AIR) {
            return false;
        }
        ItemStack stack = new ItemStack(item, 1, block.getMetaFromState(state));
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            if (predicate.test(OreDictionary.getOreName(oreId))) {
                return true;
            }
        }
        return false;
    }

    private static void hitRamTargets(EntityPlayerMP player, ChargeState state,
                                      ItemStack stack, double forwardX, double forwardZ) {
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class,
            player.getEntityBoundingBox().offset(forwardX * 0.8D, 0.0D, forwardZ * 0.8D)
                .grow(0.7D, 0.4D, 0.7D),
            target -> target != player && target.isEntityAlive()
                && !player.isOnSameTeam(target)
                && !state.hitEntities.contains(target.getEntityId()));
        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        for (EntityLivingBase target : targets) {
            float enchantmentDamage = EnchantmentHelper.getModifierForCreature(
                stack, target.getCreatureAttribute());
            if (target.attackEntityFrom(DamageSource.causePlayerDamage(player),
                baseDamage + enchantmentDamage)) {
                state.hitEntities.add(target.getEntityId());
                target.knockBack(player, 1.5F, forwardX, forwardZ);
                stack.damageItem(1, player);
                if (!player.capabilities.isCreativeMode) {
                    player.addExhaustion(ENTITY_HIT_EXHAUSTION);
                }
            }
        }
    }

    private static boolean hasChargeEnergy(EntityPlayer player) {
        return player.capabilities.isCreativeMode
            || player.getFoodStats().getFoodLevel() > MINIMUM_FOOD_TO_CHARGE;
    }

    private static final class ChargeState {
        private final float lockedYaw;
        private final float lockedPitch;
        private long lastHeartbeatTick;
        private final Set<Integer> hitEntities = new HashSet<>();

        private ChargeState(float lockedYaw, float lockedPitch) {
            this.lockedYaw = lockedYaw;
            this.lockedPitch = lockedPitch;
        }
    }
}
