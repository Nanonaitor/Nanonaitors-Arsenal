package com.nanonaitor.arsenal.config;
import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;
public final class WeaponRecipeCondition implements IConditionFactory {
    @Override public BooleanSupplier parse(JsonContext context, JsonObject json) {
        String family = json.get("family").getAsString();
        return () -> ContentSwitches.enabled(family);
    }
}
