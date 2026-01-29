package com.armorplate.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class PlateConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Double> BASE_ARMOR_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> BASE_TOUGHNESS_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> DAMAGE_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Double> DAMAGE_REDUCTION_PERCENT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SPECIAL_PROTECTION;

    static {
        BUILDER.push("防弹插板设置");

        BASE_ARMOR_BONUS = BUILDER
                .comment("钢板插板的基准护甲加成，其他插板在此基础上按比例调整")
                .defineInRange("baseArmorBonus", 4.0, 0.0, 20.0);

        BASE_TOUGHNESS_BONUS = BUILDER
                .comment("钢板插板的基准护甲韧性加成，其他插板在此基础上按比例调整")
                .defineInRange("baseToughnessBonus", 2.0, 0.0, 10.0);

        DAMAGE_THRESHOLD = BUILDER
                .comment("钢板插板的基准伤害阈值（低于此值的子弹伤害完全抵消），其他插板在此基础上按比例调整")
                .defineInRange("damageThreshold", 6.0, 0.0, 100.0);

        DAMAGE_REDUCTION_PERCENT = BUILDER
                .comment("钢板插板的基准伤害减免百分比（0-1），其他插板在此基础上按比例调整")
                .defineInRange("damageReductionPercent", 0.3, 0.0, 1.0);

        ENABLE_SPECIAL_PROTECTION = BUILDER
                .comment("是否启用特殊伤害源保护（如子弹）")
                .define("enableSpecialProtection", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}