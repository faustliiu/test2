package com.armorplate.dgh;

import com.armorplate.event.PlateEvents;
import com.armorplate.item.ArmorPlateItem;
import com.armorplate.util.BulletDamageUtil;
import com.lastimp.dgh.api.bodyPart.AbstractVisibleBody;
import com.lastimp.dgh.source.core.capability.HealthCapability;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;

/**
 * 在 DGH 体系内对“器官伤害 damageAmount”应用插板减伤，并把插板阈值从原版HP尺度换算到 DGH 尺度。
 *
 * DGH 的换算在 InjuryEventHandler.onInjury 中（不同版本包名可能不同，但公式一致）：
 *   dghDamage = vanillaDamage / (maxHealth * body_life_factor) / (dying ? 10 : 1)
 *
 * 因此：
 *   dghThreshold = vanillaThreshold / (maxHealth * body_life_factor) / (dying ? 10 : 1)
 *
 * 注意：这里不直接依赖 DGH 的 Config 类（版本差异较大），用反射读取 body_life_factor，读取失败则按 1.0 处理。
 */
public final class DghPlateReduction {

    private DghPlateReduction() {}

    public static float applyIfProtected(
            LivingEntity entity,
            DamageSource source,
            float dghDamageAmount,
            AbstractVisibleBody body
    ) {
        if (!DGHIntegrationManager.isIntegrationEnabled()) return dghDamageAmount;

        // 只对子弹类伤害生效
        if (!BulletDamageUtil.isBulletDamage(source)) return dghDamageAmount;

        // 只保护胸部 +（可选）双臂
        if (!DGHBodyPartManager.isProtectedByChestplate(body)) return dghDamageAmount;

        ItemStack chestplate = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) return dghDamageAmount;

        ItemStack plate = PlateEvents.readPlateFromChestplate(chestplate);
        if (plate.isEmpty()) return dghDamageAmount;
        if (!(plate.getItem() instanceof ArmorPlateItem armorPlate)) return dghDamageAmount;
        if (armorPlate.isBroken(plate)) return dghDamageAmount;

        // 插板参数（vanilla尺度：以原版HP伤害为单位）
        double reduction = armorPlate.getEffectiveDamageReduction(plate);
        double vanillaThreshold = armorPlate.getEffectiveDamageThreshold(plate);

        // ---- 读取 DGH 的 body_life_factor（反射，兼容不同版本包名）----
        float bodyLifeFactor = readDghBodyLifeFactorOrDefault(1.0f);

        // ---- 阈值换算：vanilla -> dgh ----
        float denom = entity.getMaxHealth() * bodyLifeFactor;
        if (denom <= 0f) return dghDamageAmount;

        float dyingDiv = HealthCapability.isDying(entity) ? 10f : 1f;

        float dghThreshold = (float) vanillaThreshold / denom / dyingDiv;

        // ---- 你的原版公式在 DGH 尺度下执行 ----
        float newDghDamage;
        if (dghDamageAmount <= dghThreshold) {
            newDghDamage = 0f;
        } else {
            newDghDamage = (dghDamageAmount - dghThreshold) * (float) (1.0 - reduction);
            if (newDghDamage < 0f) newDghDamage = 0f;
        }

        // ---- 耐久消耗：建议仍按 vanilla 尺度算，保持与未开启 DGH 时一致 ----
        float vanillaDamageAmount = dghDamageAmount * denom * dyingDiv;

        int durabilityCost = PlateEvents.calculateDurabilityCost(vanillaDamageAmount, vanillaThreshold, reduction);
        int remaining = plate.getMaxDamage() - plate.getDamageValue();
        if (durabilityCost > remaining) durabilityCost = remaining;

        armorPlate.damagePlate(plate, durabilityCost);

        PlateEvents.savePlateToChestplate(chestplate, plate);
        entity.setItemSlot(EquipmentSlot.CHEST, chestplate);

        return newDghDamage;
    }

    /**
     * 兼容读取 DGH 的 body_life_factor。
     * 已知可能位置：
     * - com.lastimp.dgh.Config.body_life_factor
     * - com.lastimp.dgh.config.Config.body_life_factor
     *
     * 读取失败则返回 defaultValue（通常用 1.0f）。
     */
    private static float readDghBodyLifeFactorOrDefault(float defaultValue) {
        Float v;

        v = readStaticFloatField("com.lastimp.dgh.Config", "body_life_factor");
        if (v != null) return v;

        v = readStaticFloatField("com.lastimp.dgh.config.Config", "body_life_factor");
        if (v != null) return v;

        return defaultValue;
    }

    private static Float readStaticFloatField(String className, String fieldName) {
        try {
            Class<?> cls = Class.forName(className, false, DghPlateReduction.class.getClassLoader());
            Field f = cls.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object o = f.get(null);

            if (o instanceof Float fl) return fl;
            if (o instanceof Double db) return db.floatValue();
            if (o instanceof Number n) return n.floatValue();
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}