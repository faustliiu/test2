package com.armorplate.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DGHIntegrationConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DGH_INTEGRATION;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_BODY_PART_LOGGING;

    public static final ForgeConfigSpec.ConfigValue<Boolean> CHEST_PROTECTS_ARMS;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_HEAD_PROTECTION;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_LEG_PROTECTION;

    static {
        BUILDER.push("DGH集成设置");

        ENABLE_DGH_INTEGRATION = BUILDER
                .comment("是否启用DGH模组集成")
                .define("enableDGHIntegration", true);

        ENABLE_BODY_PART_LOGGING = BUILDER
                .comment("是否启用身体部位命中日志")
                .define("enableBodyPartLogging", false);

        BUILDER.pop();

        BUILDER.push("身体部位保护设置");

        CHEST_PROTECTS_ARMS = BUILDER
                .comment("胸甲是否保护手臂（LEFT_ARM和RIGHT_ARM）")
                .define("chestProtectsArms", true);

        ENABLE_HEAD_PROTECTION = BUILDER
                .comment("是否启用头部防护（未来功能）")
                .define("enableHeadProtection", false);

        ENABLE_LEG_PROTECTION = BUILDER
                .comment("是否启用腿部防护（未来功能）")
                .define("enableLegProtection", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}