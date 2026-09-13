package com.nanonaitor.arsenal;

import com.nanonaitor.arsenal.client.ClientControls;
import com.nanonaitor.arsenal.combat.CombatEvents;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.registry.ModItems;
import com.nanonaitor.arsenal.registry.ModEffects;
import com.nanonaitor.arsenal.registry.ModTabs;
import com.nanonaitor.arsenal.effect.StunnedEffect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ArsenalMod.MOD_ID)
public final class ArsenalMod {
    public static final String MOD_ID = "nanonaitors_arsenal";
    public ArsenalMod() {
        var group = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(group);
        ModEffects.EFFECTS.register(group);
        ModTabs.TABS.register(group);
        com.nanonaitor.arsenal.registry.ModRecipes.SERIALIZERS.register(group);
        com.nanonaitor.arsenal.enchantment.ModEnchantments.ENCHANTMENTS.register(group);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ArsenalConfig.SPEC);
        ModNetwork.init();
        net.minecraftforge.common.crafting.CraftingHelper.register(com.nanonaitor.arsenal.config.WeaponRecipeCondition.SERIALIZER);
        com.nanonaitor.arsenal.combat.TierEffects.register();
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener((LivingAttackEvent e) -> { if (CombatEvents.onLivingAttack(e)) e.setCanceled(true); });
        bus.addListener(CombatEvents::onLivingHurt);
        bus.addListener(CombatEvents::onLivingKnockBack);
        bus.addListener(StunnedEffect::onLivingTick);
        bus.addListener((AttackEntityEvent e) -> { if (CombatEvents.onAttackEntity(e)) e.setCanceled(true); });
        bus.addListener((PlayerInteractEvent.LeftClickBlock e) -> { if (CombatEvents.onLeftClickBlock(e)) e.setCanceled(true); });
        bus.addListener((EntityJoinLevelEvent e) -> { if (CombatEvents.onEntityJoin(e)) e.setCanceled(true); });
        bus.addListener((TickEvent.PlayerTickEvent e) -> { if (e.phase == TickEvent.Phase.END) CombatEvents.onPlayerTick(e); });
        if (FMLEnvironment.dist == Dist.CLIENT) ClientControls.register();
    }
}
