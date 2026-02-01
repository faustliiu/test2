package com.armorplate.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;

public final class BulletDamageUtil {
    private BulletDamageUtil() {}

    public static boolean isBulletDamage(DamageSource source) {
        try {
            var damageTypeHolder = source.typeHolder();
            if (damageTypeHolder != null) {
                var damageTypeKey = damageTypeHolder.unwrapKey();
                if (damageTypeKey.isPresent()) {
                    ResourceLocation damageTypeId = damageTypeKey.get().location();
                    String namespace = damageTypeId.getNamespace();
                    String path = damageTypeId.getPath();

                    if ("tacz".equals(namespace)) {
                        return path.contains("bullet") || path.contains("gun");
                    }

                    if ("superbwarfare".equals(namespace)) {
                        return path.contains("gunfire")
                                || path.contains("laser")
                                || path.contains("projectile")
                                || path.contains("bullet")
                                || path.contains("shock")
                                || path.contains("phosphorus")
                                || path.contains("explosion");
                    }
                }
            }

            if (source.getDirectEntity() != null) {
                String entityType = source.getDirectEntity().getType().toString();
                if (entityType.contains("bullet")
                        || entityType.contains("projectile")
                        || entityType.contains("tacz")
                        || entityType.contains("gun")) {
                    return true;
                }
            }

            String sourceString = source.toString();
            return sourceString.contains("tacz")
                    || sourceString.contains("bullet")
                    || sourceString.contains("gun")
                    || sourceString.contains("superbwarfare");

        } catch (Exception e) {
            return false;
        }
    }
}
