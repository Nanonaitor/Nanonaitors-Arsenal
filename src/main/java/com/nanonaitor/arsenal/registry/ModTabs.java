package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsenalMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("arsenal", () -> CreativeModeTab.builder()
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .title(Component.translatable("itemGroup.nanonaitors_arsenal"))
        .icon(() -> ModItems.get(WeaponKind.MORNING_STAR, WeaponTier.DIAMOND).get().getDefaultInstance())
        .displayItems((parameters, output) -> ModItems.VISIBLE.values().forEach(item -> output.accept(item.get())))
        .build());
    private ModTabs() {}
}
