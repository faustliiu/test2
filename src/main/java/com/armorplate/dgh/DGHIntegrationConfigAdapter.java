package com.armorplate.dgh;

import com.armorplate.config.DGHIntegrationConfig;

/**
 * 只是为了避免 DghDamageReduction 直接依赖太多配置项，便于你按需删改。
 */
public final class DGHIntegrationConfigAdapter {
    private DGHIntegrationConfigAdapter() {}

    public static boolean isVerbose() {
        return DGHIntegrationConfig.ENABLE_BODY_PART_LOGGING.get();
    }
}
