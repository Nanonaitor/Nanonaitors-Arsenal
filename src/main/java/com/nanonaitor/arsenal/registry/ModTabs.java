package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.enchantment.ModEnchantments;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsenalMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("arsenal", () -> CreativeModeTab.builder()
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .title(Component.translatable("itemGroup.nanonaitors_arsenal"))
        .icon(() -> ModItems.get(WeaponKind.MORNING_STAR, WeaponTier.DIAMOND).get().getDefaultInstance())
        .displayItems((parameters, output) -> {
            ModItems.VISIBLE.values().forEach(item -> output.accept(item.get()));
            var enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);
            addEnchantedBook(output, enchantments.getOrThrow(ModEnchantments.LONG_CHAIN));
            addEnchantedBook(output, enchantments.getOrThrow(ModEnchantments.ROTATION_FORCE));
        })
        .build());

    private static void addEnchantedBook(CreativeModeTab.Output output, Holder<Enchantment> enchantment) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book,
            stored -> stored.set(enchantment, enchantment.value().getMaxLevel()));
        output.accept(book);
    }

    private ModTabs() {}
}
