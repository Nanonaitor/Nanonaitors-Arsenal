package com.nanonaitor.arsenal.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Chest-loot counterpart of mob equipment, as the 1.12.2 lootIntegration option did:
 * each plain sword or axe a loot table rolls may become a same-tier Arsenal weapon.
 * Named, lore, attribute or otherwise customized loot (a pack's legendary items) is kept,
 * because {@link WorldEquipmentCompat#alternativeFor} skips special stacks.
 */
public final class LootAlternativesModifier extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS=
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,ArsenalMod.MOD_ID);
    public static final RegistryObject<Codec<LootAlternativesModifier>> CODEC=SERIALIZERS.register("weapon_alternatives",
        ()->RecordCodecBuilder.create(inst->codecStart(inst).apply(inst,LootAlternativesModifier::new)));

    public LootAlternativesModifier(LootItemCondition[] conditions){super(conditions);}

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        if(!ArsenalConfig.LOOT_INTEGRATION.get())return loot;
        return replace(loot,context.getRandom(),ArsenalConfig.LOOT_REPLACEMENT_CHANCE.get());
    }

    /** Each eligible stack rolls on its own, like each 1.12.2 loot entry did. */
    public static ObjectArrayList<ItemStack> replace(ObjectArrayList<ItemStack> loot,net.minecraft.util.RandomSource random,double chance){
        for(int i=0;i<loot.size();i++){
            ItemStack replacement=WorldEquipmentCompat.alternativeFor(loot.get(i),random,chance);
            if(replacement!=null)loot.set(i,replacement);
        }
        return loot;
    }

    @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC.get();}
}
