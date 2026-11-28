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
        // Custom materials decide which items exist, so they are read before anything registers.
        com.nanonaitor.arsenal.config.CustomMaterials.loadEarly();
        var group = FMLJavaModLoadingContext.get().getModEventBus();
        group.addListener(com.nanonaitor.arsenal.config.CustomMaterials::addPack);
        ModItems.ITEMS.register(group);
        ModEffects.EFFECTS.register(group);
        com.nanonaitor.arsenal.registry.ModSounds.SOUNDS.register(group);
        ModTabs.TABS.register(group);
        com.nanonaitor.arsenal.registry.ModRecipes.SERIALIZERS.register(group);
        com.nanonaitor.arsenal.enchantment.ModEnchantments.ENCHANTMENTS.register(group);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ArsenalConfig.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
            com.nanonaitor.arsenal.config.CustomMaterials.SPEC, com.nanonaitor.arsenal.config.CustomMaterials.FILE);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,
            com.nanonaitor.arsenal.config.ArsenalClientConfig.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
            com.nanonaitor.arsenal.compat.LevelRequirements.SPEC,com.nanonaitor.arsenal.compat.LevelRequirements.FILE);
        ModNetwork.init();
        net.minecraftforge.common.crafting.CraftingHelper.register(com.nanonaitor.arsenal.config.WeaponRecipeCondition.SERIALIZER);
        com.nanonaitor.arsenal.combat.TierEffects.register();
        com.nanonaitor.arsenal.combat.ScimitarCounter.register();
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener((LivingAttackEvent e) -> { if (CombatEvents.onLivingAttack(e)) e.setCanceled(true); });
        bus.addListener(CombatEvents::onLivingHurt);
        bus.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST, CombatEvents::prepareBulwarkDamage);
        bus.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, CombatEvents::protectStaffAtHurt);
        bus.addListener(CombatEvents::onLivingKnockBack);
        bus.addListener(CombatEvents::onLoggedOut);
        bus.addListener(StunnedEffect::onLivingTick);
        bus.addListener((AttackEntityEvent e) -> { if (CombatEvents.onAttackEntity(e)) e.setCanceled(true); });
        bus.addListener((PlayerInteractEvent.LeftClickBlock e) -> { if (CombatEvents.onLeftClickBlock(e)) e.setCanceled(true); });
        bus.addListener((EntityJoinLevelEvent e) -> { if (CombatEvents.onEntityJoin(e)) e.setCanceled(true); });
        bus.addListener((TickEvent.PlayerTickEvent e) -> { if (e.phase == TickEvent.Phase.END) CombatEvents.onPlayerTick(e); });
        // Players see and predict attacks with the server's combat settings, not their own file.
        bus.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) ModNetwork.syncConfig(player);
        });
        group.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Reloading e) -> {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            // Reloads come from the file watcher thread; send from the server thread.
            if (e.getConfig().getSpec() == ArsenalConfig.SPEC && server != null) server.execute(() -> ModNetwork.syncConfig(null));
        });
        if (FMLEnvironment.dist == Dist.CLIENT) ClientControls.register();
    }
}
