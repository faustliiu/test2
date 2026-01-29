package com.armorplate.util;

import com.armorplate.event.PlateEvents;
import net.minecraft.world.item.ItemStack;

public class PlateNBTHelper {
    /**
     * 从胸甲NBT读取插板（使用统一的方法）
     */
    public static ItemStack getPlateFromNBT(ItemStack chestplate) {
        return PlateEvents.readPlateFromChestplate(chestplate);
    }

    /**
     * 保存插板到胸甲NBT（使用统一的方法）
     */
    public static void savePlateToNBT(ItemStack chestplate, ItemStack plate) {
        PlateEvents.savePlateToChestplate(chestplate, plate);
    }

    /**
     * 检查胸甲是否有插板
     */
    public static boolean hasPlate(ItemStack chestplate) {
        return !getPlateFromNBT(chestplate).isEmpty();
    }

    /**
     * 清除胸甲中的插板数据
     */
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
