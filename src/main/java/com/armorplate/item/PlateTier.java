package com.armorplate.item;

import com.armorplate.config.PlateConfig;

public enum PlateTier {
    STEEL("steel"),
    CERAMIC("ceramic"),
    COMPOSITE("composite");

    private final String name;

    PlateTier(String name) {
        this.name = name;
    }

    public double getArmorBonus() {
        // 使用配置值，不同类型可以有不同加成
        return switch (this) {
            case STEEL -> PlateConfig.BASE_ARMOR_BONUS.get() * 1.0;
            case CERAMIC -> PlateConfig.BASE_ARMOR_BONUS.get() * 1.5;
            case COMPOSITE -> PlateConfig.BASE_ARMOR_BONUS.get() * 2.0;
        };
    }

    public double getToughnessBonus() {
        return switch (this) {
            case STEEL -> PlateConfig.BASE_TOUGHNESS_BONUS.get() * 1.0;
            case CERAMIC -> PlateConfig.BASE_TOUGHNESS_BONUS.get() * 0.8;
            case COMPOSITE -> PlateConfig.BASE_TOUGHNESS_BONUS.get() * 1.5;
        };
    }

    public double getDamageReduction() {
        // 使用配置百分比，不同类型有不同加成
        return switch (this) {
            case STEEL -> PlateConfig.DAMAGE_REDUCTION_PERCENT.get() * 0.8;
            case CERAMIC -> PlateConfig.DAMAGE_REDUCTION_PERCENT.get() * 1.2;
            case COMPOSITE -> PlateConfig.DAMAGE_REDUCTION_PERCENT.get() * 1.5;
        };
    }

    public double getDamageThreshold() {
        // 使用配置阈值，不同类型有不同加成
        return switch (this) {
            case STEEL -> PlateConfig.DAMAGE_THRESHOLD.get() * 0.8;
            case CERAMIC -> PlateConfig.DAMAGE_THRESHOLD.get() * 1.0;
            case COMPOSITE -> PlateConfig.DAMAGE_THRESHOLD.get() * 1.2;
        };
    }

    // 添加最大耐久度方法
    public int getMaxDurability() {
        return switch (this) {
            case STEEL -> PlateConfig.STEEL_PLATE_DURABILITY.get();
            case CERAMIC -> PlateConfig.CERAMIC_PLATE_DURABILITY.get();
            case COMPOSITE -> PlateConfig.COMPOSITE_PLATE_DURABILITY.get();
        };
    }

    public String getName() { return name; }
}