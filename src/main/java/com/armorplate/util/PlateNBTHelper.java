package com.armorplate.util;

import com.armorplate.event.PlateEvents;
import net.minecraft.world.item.ItemStack;

public class PlateNBTHelper {
    public static ItemStack getPlateFromNBT(ItemStack chestplate) {
        return PlateEvents.readPlateFromChestplate(chestplate);
    }

    public static void savePlateToNBT(ItemStack chestplate, ItemStack plate) {
        PlateEvents.savePlateToChestplate(chestplate, plate);
    }

    public static boolean hasPlate(ItemStack chestplate) {
        return !getPlateFromNBT(chestplate).isEmpty();
    }

    public static void removePlateFromNBT(ItemStack chestplate) {
        if (chestplate.hasTag() && chestplate.getTag().contains("ArmorPlate")) {
            chestplate.getTag().remove("ArmorPlate");
            // 如果NBT为空，设置为null
            if (chestplate.getTag().isEmpty()) {
                chestplate.setTag(null);
            }
        }
    }
}
