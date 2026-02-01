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
        if (!isEnabled()) return 0.0;
        return switch (this) {
            case STEEL -> PlateConfig.STEEL_ARMOR_BONUS.get();
            case CERAMIC -> PlateConfig.CERAMIC_ARMOR_BONUS.get();
            case COMPOSITE -> PlateConfig.COMPOSITE_ARMOR_BONUS.get();
        };
    }

    public double getToughnessBonus() {
        if (!isEnabled()) return 0.0;
        return switch (this) {
            case STEEL -> PlateConfig.STEEL_TOUGHNESS_BONUS.get();
            case CERAMIC -> PlateConfig.CERAMIC_TOUGHNESS_BONUS.get();
            case COMPOSITE -> PlateConfig.COMPOSITE_TOUGHNESS_BONUS.get();
        };
    }

    public double getDamageReduction() {
        if (!isEnabled()) return 0.0;
        return switch (this) {
            case STEEL -> PlateConfig.STEEL_DAMAGE_REDUCTION_PERCENT.get();
            case CERAMIC -> PlateConfig.CERAMIC_DAMAGE_REDUCTION_PERCENT.get();
            case COMPOSITE -> PlateConfig.COMPOSITE_DAMAGE_REDUCTION_PERCENT.get();
        };
    }

    public double getDamageThreshold() {
        if (!isEnabled()) return 0.0;
        return switch (this) {
            case STEEL -> PlateConfig.STEEL_DAMAGE_THRESHOLD.get();
            case CERAMIC -> PlateConfig.CERAMIC_DAMAGE_THRESHOLD.get();
            case COMPOSITE -> PlateConfig.COMPOSITE_DAMAGE_THRESHOLD.get();
        };
    }

    public int getMaxDurability() {
        return switch (this) {
            case STEEL -> PlateConfig.STEEL_DURABILITY.get();
            case CERAMIC -> PlateConfig.CERAMIC_DURABILITY.get();
            case COMPOSITE -> PlateConfig.COMPOSITE_DURABILITY.get();
        };
    }

    public boolean isEnabled() {
        return switch (this) {
            case STEEL -> PlateConfig.STEEL_ENABLED.get();
            case CERAMIC -> PlateConfig.CERAMIC_ENABLED.get();
            case COMPOSITE -> PlateConfig.COMPOSITE_ENABLED.get();
        };
    }

    public String getConfigKey() {
        return switch (this) {
            case STEEL -> "steel";
            case CERAMIC -> "ceramic";
            case COMPOSITE -> "composite";
        };
    }

    public String getName() {
        return name;
    }
}