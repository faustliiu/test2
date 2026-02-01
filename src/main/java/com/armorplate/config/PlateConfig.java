package com.armorplate.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class PlateConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SPECIAL_PROTECTION;

    public static final ForgeConfigSpec.ConfigValue<Double> STEEL_ARMOR_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> STEEL_TOUGHNESS_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> STEEL_DAMAGE_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Double> STEEL_DAMAGE_REDUCTION_PERCENT;
    public static final ForgeConfigSpec.IntValue STEEL_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> STEEL_ENABLED;

    public static final ForgeConfigSpec.ConfigValue<Double> CERAMIC_ARMOR_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> CERAMIC_TOUGHNESS_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> CERAMIC_DAMAGE_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Double> CERAMIC_DAMAGE_REDUCTION_PERCENT;
    public static final ForgeConfigSpec.IntValue CERAMIC_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CERAMIC_ENABLED;

    public static final ForgeConfigSpec.ConfigValue<Double> COMPOSITE_ARMOR_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> COMPOSITE_TOUGHNESS_BONUS;
    public static final ForgeConfigSpec.ConfigValue<Double> COMPOSITE_DAMAGE_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Double> COMPOSITE_DAMAGE_REDUCTION_PERCENT;
    public static final ForgeConfigSpec.IntValue COMPOSITE_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> COMPOSITE_ENABLED;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DURABILITY_EFFECT;
    public static final ForgeConfigSpec.ConfigValue<Double> DURABILITY_EFFECT_THRESHOLD_1;
    public static final ForgeConfigSpec.ConfigValue<Double> DURABILITY_EFFECT_THRESHOLD_2;
    public static final ForgeConfigSpec.ConfigValue<Double> DURABILITY_EFFECT_THRESHOLD_3;
    public static final ForgeConfigSpec.ConfigValue<Double> DURABILITY_EFFECT_MULTIPLIER_1;
    public static final ForgeConfigSpec.ConfigValue<Double> DURABILITY_EFFECT_MULTIPLIER_2;
    public static final ForgeConfigSpec.ConfigValue<Double> DURABILITY_EFFECT_MULTIPLIER_3;
    public static final ForgeConfigSpec.ConfigValue<Double> DURABILITY_EFFECT_MULTIPLIER_4;

    public static final ForgeConfigSpec.ConfigValue<Integer> BASE_DURABILITY_COST;
    public static final ForgeConfigSpec.ConfigValue<Double> DAMAGE_TO_DURABILITY_RATIO;
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_DURABILITY_COST_PER_HIT;

    static {
        BUILDER.push("防弹插板设置");

        BUILDER.comment("全局设置").push("global");
        ENABLE_SPECIAL_PROTECTION = BUILDER
                .comment("是否启用特殊伤害源保护（如子弹）")
                .define("enableSpecialProtection", true);
        BUILDER.pop();

        BUILDER.comment("钢插板设置").push("steel_plate");
        STEEL_ENABLED = BUILDER
                .comment("是否启用钢插板")
                .define("enabled", true);
        STEEL_ARMOR_BONUS = BUILDER
                .comment("钢插板的护甲加成")
                .defineInRange("armorBonus", 4.0, 0.0, 50.0);
        STEEL_TOUGHNESS_BONUS = BUILDER
                .comment("钢插板的护甲韧性加成")
                .defineInRange("toughnessBonus", 2.0, 0.0, 20.0);
        STEEL_DAMAGE_THRESHOLD = BUILDER
                .comment("钢插板的伤害阈值（低于此值的子弹伤害完全抵消）")
                .defineInRange("damageThreshold", 6.0, 0.0, 100.0);
        STEEL_DAMAGE_REDUCTION_PERCENT = BUILDER
                .comment("钢插板的伤害减免百分比（0-1）")
                .defineInRange("damageReductionPercent", 0.3, 0.0, 1.0);
        STEEL_DURABILITY = BUILDER
                .comment("钢插板的最大耐久")
                .defineInRange("durability", 200, 1, 10000);
        BUILDER.pop();

        BUILDER.comment("陶瓷插板设置").push("ceramic_plate");
        CERAMIC_ENABLED = BUILDER
                .comment("是否启用陶瓷插板")
                .define("enabled", true);
        CERAMIC_ARMOR_BONUS = BUILDER
                .comment("陶瓷插板的护甲加成")
                .defineInRange("armorBonus", 6.0, 0.0, 50.0);
        CERAMIC_TOUGHNESS_BONUS = BUILDER
                .comment("陶瓷插板的护甲韧性加成")
                .defineInRange("toughnessBonus", 1.6, 0.0, 20.0);
        CERAMIC_DAMAGE_THRESHOLD = BUILDER
                .comment("陶瓷插板的伤害阈值（低于此值的子弹伤害完全抵消）")
                .defineInRange("damageThreshold", 8.0, 0.0, 100.0);
        CERAMIC_DAMAGE_REDUCTION_PERCENT = BUILDER
                .comment("陶瓷插板的伤害减免百分比（0-1）")
                .defineInRange("damageReductionPercent", 0.36, 0.0, 1.0);
        CERAMIC_DURABILITY = BUILDER
                .comment("陶瓷插板的最大耐久")
                .defineInRange("durability", 150, 1, 10000);
        BUILDER.pop();

        BUILDER.comment("复合插板设置").push("composite_plate");
        COMPOSITE_ENABLED = BUILDER
                .comment("是否启用复合插板")
                .define("enabled", true);
        COMPOSITE_ARMOR_BONUS = BUILDER
                .comment("复合插板的护甲加成")
                .defineInRange("armorBonus", 8.0, 0.0, 50.0);
        COMPOSITE_TOUGHNESS_BONUS = BUILDER
                .comment("复合插板的护甲韧性加成")
                .defineInRange("toughnessBonus", 3.0, 0.0, 20.0);
        COMPOSITE_DAMAGE_THRESHOLD = BUILDER
                .comment("复合插板的伤害阈值（低于此值的子弹伤害完全抵消）")
                .defineInRange("damageThreshold", 10.0, 0.0, 100.0);
        COMPOSITE_DAMAGE_REDUCTION_PERCENT = BUILDER
                .comment("复合插板的伤害减免百分比（0-1）")
                .defineInRange("damageReductionPercent", 0.45, 0.0, 1.0);
        COMPOSITE_DURABILITY = BUILDER
                .comment("复合插板的最大耐久")
                .defineInRange("durability", 300, 1, 10000);
        BUILDER.pop();

        BUILDER.comment("高级设置").push("advanced");

        ENABLE_DURABILITY_EFFECT = BUILDER
                .comment("是否启用耐久度影响属性效果")
                .define("enableDurabilityEffect", true);

        DURABILITY_EFFECT_THRESHOLD_1 = BUILDER
                .comment("耐久度影响阈值1（耐久低于此百分比时应用效果1）")
                .defineInRange("durabilityEffectThreshold1", 0.1, 0.0, 1.0);
        DURABILITY_EFFECT_THRESHOLD_2 = BUILDER
                .comment("耐久度影响阈值2")
                .defineInRange("durabilityEffectThreshold2", 0.3, 0.0, 1.0);
        DURABILITY_EFFECT_THRESHOLD_3 = BUILDER
                .comment("耐久度影响阈值3")
                .defineInRange("durabilityEffectThreshold3", 0.6, 0.0, 1.0);

        DURABILITY_EFFECT_MULTIPLIER_1 = BUILDER
                .comment("耐久度低于阈值1时的效果乘数")
                .defineInRange("durabilityEffectMultiplier1", 0.3, 0.0, 1.0);
        DURABILITY_EFFECT_MULTIPLIER_2 = BUILDER
                .comment("耐久度在阈值1-2之间的效果乘数")
                .defineInRange("durabilityEffectMultiplier2", 0.5, 0.0, 1.0);
        DURABILITY_EFFECT_MULTIPLIER_3 = BUILDER
                .comment("耐久度在阈值2-3之间的效果乘数")
                .defineInRange("durabilityEffectMultiplier3", 0.75, 0.0, 1.0);
        DURABILITY_EFFECT_MULTIPLIER_4 = BUILDER
                .comment("耐久度高于阈值3时的效果乘数")
                .defineInRange("durabilityEffectMultiplier4", 1.0, 0.0, 1.0);

        BASE_DURABILITY_COST = BUILDER
                .comment("每次命中基础耐久消耗")
                .defineInRange("baseDurabilityCost", 1, 0, 100);
        DAMAGE_TO_DURABILITY_RATIO = BUILDER
                .comment("伤害转耐久消耗比例（每点伤害额外消耗的耐久）")
                .defineInRange("damageToDurabilityRatio", 0.2, 0.0, 10.0);
        MAX_DURABILITY_COST_PER_HIT = BUILDER
                .comment("单次命中最大耐久消耗")
                .defineInRange("maxDurabilityCostPerHit", 10, 1, 1000);

        BUILDER.pop();
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}