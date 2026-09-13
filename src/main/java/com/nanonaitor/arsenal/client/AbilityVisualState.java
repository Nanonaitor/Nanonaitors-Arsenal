package com.nanonaitor.arsenal.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import com.nanonaitor.arsenal.item.*;
public final class AbilityVisualState {
    public static boolean staffReflecting(LivingEntity entity){
        if(entity==null || !entity.getOffhandItem().isEmpty()
            || !(entity.getMainHandItem().getItem() instanceof ArsenalWeaponItem item)
            || item.kind()!=WeaponKind.BLADE_STAFF)return false;
        if(entity==Minecraft.getInstance().player)
            return ClientControls.bladeStaffReflecting(entity.level().getGameTime());
        return entity.isUsingItem() && entity.getUseItem()==entity.getMainHandItem();
    }
}
