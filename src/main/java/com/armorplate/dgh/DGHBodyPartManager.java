package com.armorplate.dgh;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DGHBodyPartManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, String> BODY_PART_NAME_MAP = new HashMap<>();

    private static final Map<String, EquipmentSlot> BODY_PART_TO_SLOT = new HashMap<>();

    static {
        BODY_PART_NAME_MAP.put("HEAD", "头部");
        BODY_PART_TO_SLOT.put("HEAD", EquipmentSlot.HEAD);

        BODY_PART_NAME_MAP.put("TORSO", "躯干");
        BODY_PART_TO_SLOT.put("TORSO", EquipmentSlot.CHEST);

        BODY_PART_NAME_MAP.put("LEFT_ARM", "左臂");
        BODY_PART_TO_SLOT.put("LEFT_ARM", EquipmentSlot.CHEST);

        BODY_PART_NAME_MAP.put("RIGHT_ARM", "右臂");
        BODY_PART_TO_SLOT.put("RIGHT_ARM", EquipmentSlot.CHEST);

        BODY_PART_NAME_MAP.put("LEFT_LEG", "左腿");
        BODY_PART_TO_SLOT.put("LEFT_LEG", EquipmentSlot.LEGS);

        BODY_PART_NAME_MAP.put("RIGHT_LEG", "右腿");
        BODY_PART_TO_SLOT.put("RIGHT_LEG", EquipmentSlot.LEGS);

        BODY_PART_NAME_MAP.put("BLOOD", "血液");
        BODY_PART_TO_SLOT.put("BLOOD", null);

        BODY_PART_NAME_MAP.put("WHOLE_BODY", "全身");
        BODY_PART_TO_SLOT.put("WHOLE_BODY", null);
    }

    public static String getBodyPartName(String bodyPartId) {
        if (bodyPartId == null) return "未知部位";
        return BODY_PART_NAME_MAP.getOrDefault(bodyPartId, bodyPartId);
    }

    public static EquipmentSlot getSlotForBodyPart(String bodyPartId) {
        if (bodyPartId == null) return null;
        return BODY_PART_TO_SLOT.get(bodyPartId);
    }

    public static boolean isBodyPartProtectedByChestplate(String bodyPartId) {
        if (bodyPartId == null) return false;

        if ("TORSO".equals(bodyPartId)) return true;

        if (com.armorplate.config.DGHIntegrationConfig.CHEST_PROTECTS_ARMS.get()) {
            return "LEFT_ARM".equals(bodyPartId) || "RIGHT_ARM".equals(bodyPartId);
        }

        return false;
    }

    public static boolean isVisibleBodyPart(String bodyPartId) {
        if (bodyPartId == null) return false;
        return "HEAD".equals(bodyPartId) || "TORSO".equals(bodyPartId) ||
                "LEFT_ARM".equals(bodyPartId) || "RIGHT_ARM".equals(bodyPartId) ||
                "LEFT_LEG".equals(bodyPartId) || "RIGHT_LEG".equals(bodyPartId);
    }
}