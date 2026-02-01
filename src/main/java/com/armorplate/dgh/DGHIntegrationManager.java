package com.armorplate.dgh;

import com.armorplate.config.DGHIntegrationConfig;
import com.armorplate.event.PlateEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class DGHIntegrationManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean isDGHLoaded = false;
    private static boolean isInitialized = false;

    private static final Map<UUID, HitData> hitDataMap = new HashMap<>();
    private static final Map<UUID, Boolean> bulletDamageMap = new HashMap<>();

    public static class HitData {
        public final String bodyPartId;
        public final String bodyPartName;
        public final boolean hasProtection;
        public final long timestamp;

        public HitData(String bodyPartId, String bodyPartName, boolean hasProtection) {
            this.bodyPartId = bodyPartId;
            this.bodyPartName = bodyPartName;
            this.hasProtection = hasProtection;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < 1000; // 1秒内有效
        }
    }

    public static void init() {
        if (isInitialized) return;

        isDGHLoaded = ModList.get().isLoaded("dgh");
        LOGGER.info("DGH模组加载状态: {}", isDGHLoaded);

        if (isDGHLoaded && DGHIntegrationConfig.ENABLE_DGH_INTEGRATION.get()) {
            LOGGER.info("启用DGH模组集成");
        } else {
            LOGGER.info("跳过DGH模组集成");
        }

        isInitialized = true;
    }

    public static boolean isIntegrationEnabled() {
        return isDGHLoaded && DGHIntegrationConfig.ENABLE_DGH_INTEGRATION.get();
    }

    public static void recordHit(LivingEntity entity, String bodyPartId, boolean hasProtection) {
        if (!isIntegrationEnabled()) return;

        String bodyPartName = DGHBodyPartManager.getBodyPartName(bodyPartId);
        HitData data = new HitData(bodyPartId, bodyPartName, hasProtection);
        hitDataMap.put(entity.getUUID(), data);

        if (DGHIntegrationConfig.ENABLE_BODY_PART_LOGGING.get()) {
            LOGGER.info("DGH命中记录: 实体={}, 部位={}({}), 有防护={}",
                    entity.getName().getString(), bodyPartName, bodyPartId, hasProtection);
        }
    }

    public static HitData getRecentHitData(LivingEntity entity) {
        if (!isIntegrationEnabled()) return null;

        HitData data = hitDataMap.get(entity.getUUID());
        if (data != null && data.isValid()) {
            return data;
        }
        return null;
    }


    public static boolean shouldApplyPlateProtection(LivingEntity entity, String bodyPartId) {
        if (!isIntegrationEnabled()) return false; // 关键修改：移除 Player 检查

        if (!DGHBodyPartManager.isVisibleBodyPart(bodyPartId)) {
            return false;
        }

        if (!DGHBodyPartManager.isBodyPartProtectedByChestplate(bodyPartId)) {
            LOGGER.debug("身体部位 {} 不受胸甲保护", bodyPartId);
            return false;
        }

        return PlateEvents.hasPlateInChest(entity);
    }


    public static void setBulletDamageInProgress(LivingEntity entity, boolean inProgress) {
        if (!isIntegrationEnabled()) return;

        if (inProgress) {
            bulletDamageMap.put(entity.getUUID(), true);
        } else {
            bulletDamageMap.remove(entity.getUUID());
        }
    }


    public static boolean isBulletDamageInProgress(LivingEntity entity) {
        if (!isIntegrationEnabled()) return false;

        return bulletDamageMap.getOrDefault(entity.getUUID(), false);
    }

    public static void clearHitData(LivingEntity entity) {
        hitDataMap.remove(entity.getUUID());
        bulletDamageMap.remove(entity.getUUID());
    }


    public static String getHitBodyPartName(LivingEntity entity) {
        HitData data = getRecentHitData(entity);
        return data != null ? data.bodyPartName : null;
    }


    public static boolean shouldApplyDefaultProtection(LivingEntity entity) {
        if (!isIntegrationEnabled()) return false;

        if (isBulletDamageInProgress(entity)) {
            setBulletDamageInProgress(entity, false);
            return PlateEvents.hasPlateInChest(entity);
        }

        return false;
    }
}