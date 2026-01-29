package com.armorplate.capability;

import net.minecraft.world.item.ItemStack;

public interface IPlateCapability {
    boolean hasPlate();
    ItemStack getPlate();
    void setPlate(ItemStack plate);
    void removePlate();

    double getArmorBonus();
    double getToughnessBonus();
    double getDamageReduction();
    double getDamageThreshold();
}
