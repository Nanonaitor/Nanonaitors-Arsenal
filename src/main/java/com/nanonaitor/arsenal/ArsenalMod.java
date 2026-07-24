package com.nanonaitor.arsenal;

import com.nanonaitor.arsenal.client.ClientControls;
import com.nanonaitor.arsenal.combat.CombatEvents;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.registry.ModItems;
import com.nanonaitor.arsenal.registry.ModEffects;
import com.nanonaitor.arsenal.registry.ModTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ArsenalMod.MOD_ID)
public final class ArsenalMod {
    public static final String MOD_ID = "nanonaitors_arsenal";
    public ArsenalMod(FMLJavaModLoadingContext context) {
        var group = context.getModBusGroup();
        ModItems.ITEMS.register(group);
        ModEffects.EFFECTS.register(group);
        ModTabs.TABS.register(group);
        ModNetwork.init();
        LivingAttackEvent.BUS.addListener(CombatEvents::onLivingAttack);
        LivingHurtEvent.BUS.addListener(CombatEvents::onLivingHurt);
        AttackEntityEvent.BUS.addListener(CombatEvents::onAttackEntity);
        PlayerInteractEvent.LeftClickBlock.BUS.addListener(CombatEvents::onLeftClickBlock);
        EntityJoinLevelEvent.BUS.addListener(CombatEvents::onEntityJoin);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(CombatEvents::onPlayerTick);
        if (FMLEnvironment.dist == Dist.CLIENT) ClientControls.register();
    }
}
