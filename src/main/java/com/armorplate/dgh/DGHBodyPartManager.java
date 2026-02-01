package com.armorplate.dgh;

import com.armorplate.config.DGHIntegrationConfig;
import com.lastimp.dgh.api.bodyPart.AbstractBody;
import com.lastimp.dgh.api.bodyPart.AbstractVisibleBody;

/**
 * DGH 部位判定工具：
 * - 新实现：优先使用 body 的 className / component 文本判断 torso/arm
 * - 兼容旧代码：保留 String 入参版本，避免改 DGHIntegrationManager
 */
public final class DGHBodyPartManager {

    private DGHBodyPartManager() {}

    // =========================
    // 新接口（推荐在 handler 内使用）
    // =========================

    public static boolean isProtectedByChestplate(AbstractVisibleBody body) {
        if (body == null) return false;

        // 1) 类名兜底（不受语言包影响）
        String cls = body.getClass().getSimpleName().toLowerCase();
        if (cls.contains("torso")) return true;

        if (DGHIntegrationConfig.CHEST_PROTECTS_ARMS.get()) {
            if (cls.contains("arm")) return true;
        }

        // 2) 显示名补充（可能受语言影响）
        String display = "";
        try {
            display = body.getComponent() != null ? body.getComponent().getString() : "";
        } catch (Throwable ignored) {}

        String d = display.toLowerCase();
        if (d.contains("torso") || d.contains("chest") || d.contains("body")) return true;

        if (DGHIntegrationConfig.CHEST_PROTECTS_ARMS.get() && d.contains("arm")) return true;

        return false;
    }

    // =========================
    // 旧接口（AbstractBody 入参）
    // =========================

    public static boolean isVisibleBodyPart(AbstractBody body) {
        return body instanceof AbstractVisibleBody;
    }

    public static String getBodyPartName(AbstractBody body) {
        if (!(body instanceof AbstractVisibleBody visible)) return "unknown";

        String cls = visible.getClass().getSimpleName();
        if (cls != null && !cls.isBlank()) return cls;

        try {
            if (visible.getComponent() != null) return visible.getComponent().getString();
        } catch (Throwable ignored) {}

        return "unknown";
    }

    // =========================
    // 旧接口（String 入参，给 DGHIntegrationManager 兼容用）
    // 注意：String 版本无法可靠判断“是不是可见部位”，这里只能做保守返回
    // =========================

    public static boolean isVisibleBodyPart(String bodyPartId) {
        // 旧实现里你把 bodyPartId 当作字符串传递，这里只能认为它“可能可见”
        // 不要在这里做严格过滤，否则会误判导致逻辑不走
        return bodyPartId != null && !bodyPartId.isBlank();
    }

    public static String getBodyPartName(String bodyPartId) {
        // 旧调用点只给了一个 string，我们没有 body 实例，拿不到 class/component
        // 这里返回 id 仅用于日志/调试
        return bodyPartId == null ? "unknown" : bodyPartId;
    }

    public static boolean isBodyPartProtectedByChestplate(String bodyPartId) {
        // 兼容旧代码只传一个参数的调用
        return isBodyPartProtectedByChestplate(bodyPartId, bodyPartId);
    }

    public static boolean isBodyPartProtectedByChestplate(String bodyPartId, String bodyPartName) {
        // 兼容你旧逻辑可能传入的 "TORSO/LEFT_ARM/RIGHT_ARM"
        if (bodyPartId != null) {
            String id = bodyPartId.toUpperCase();
            if (id.contains("TORSO")) return true;

            if (DGHIntegrationConfig.CHEST_PROTECTS_ARMS.get()
                    && (id.contains("LEFT_ARM") || id.contains("RIGHT_ARM"))) {
                return true;
            }
        }

        if (bodyPartName == null) return false;
        String n = bodyPartName.toLowerCase();

        if (n.contains("torso")) return true;

        if (DGHIntegrationConfig.CHEST_PROTECTS_ARMS.get()) {
            if (n.contains("arm") || n.contains("leftarm") || n.contains("rightarm")
                    || n.contains("left_arm") || n.contains("right_arm")) {
                return true;
            }
        }

        return false;
    }
}