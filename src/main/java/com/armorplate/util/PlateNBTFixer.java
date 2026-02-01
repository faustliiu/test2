package com.armorplate.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlateNBTFixer {
    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean fixBrokenArmorPlateTag(ItemStack chestplate) {
        if (!chestplate.hasTag()) {
            return false;
        }

        CompoundTag tag = chestplate.getTag();
        if (!tag.contains("ArmorPlate")) {
            return false;
        }

        Tag armorPlateTag = tag.get("ArmorPlate");

        if (armorPlateTag instanceof net.minecraft.nbt.DoubleTag) {
            LOGGER.error("检测到损坏的DoubleTag格式的ArmorPlate标签，尝试修复");

            tag.remove("ArmorPlate");

            if (tag.isEmpty()) {
                chestplate.setTag(null);
            } else {
                chestplate.setTag(tag);
            }

            LOGGER.error("已修复损坏的ArmorPlate标签（移除）");
            return true;
        }

        if (armorPlateTag instanceof CompoundTag) {
            CompoundTag plateTag = (CompoundTag) armorPlateTag;
            if (plateTag.isEmpty()) {
                LOGGER.error("检测到空的ArmorPlate CompoundTag，尝试修复");

                tag.remove("ArmorPlate");

                if (tag.isEmpty()) {
                    chestplate.setTag(null);
                } else {
                    chestplate.setTag(tag);
                }

                LOGGER.error("已修复空的ArmorPlate标签（移除）");
                return true;
            }
        }

        return false;
    }

    public static CompoundTag getSafeArmorPlateTag(ItemStack chestplate) {
        if (!chestplate.hasTag()) {
            return null;
        }

        CompoundTag tag = chestplate.getTag();
        if (!tag.contains("ArmorPlate")) {
            return null;
        }

        Tag armorPlateTag = tag.get("ArmorPlate");
        if (!(armorPlateTag instanceof CompoundTag)) {
            fixBrokenArmorPlateTag(chestplate);
            return null;
        }

        return (CompoundTag) armorPlateTag;
    }
}
