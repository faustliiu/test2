package com.armorplate.mixin;

import com.armorplate.dgh.DGHIntegrationManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 安全的InjuryEventHandler Mixin
 * 使用反射来避免直接依赖DGH类
 */
@Mixin(targets = "com.lastimp.dgh.source.core.damageSystem.InjuryEventHandler", remap = false)
public abstract class InjuryEventHandlerMixin {

    @Inject(
            method = "handleEntityAttack",
            at = @At("HEAD"),
            remap = false
    )
    private static void onHandleEntityAttack(
            net.minecraft.world.damagesource.DamageSource source,
            float damageAmount,
            net.minecraft.world.entity.LivingEntity entity,
            net.minecraftforge.event.entity.living.LivingDamageEvent event,
            CallbackInfo ci) {

        if (!ModList.get().isLoaded("dgh")) return;
        if (!(entity instanceof Player player)) return;

        try {
            // 使用反射获取DGH的数据
            Class<?> healthCapabilityClass = Class.forName("com.lastimp.dgh.source.core.capability.HealthCapability");
            java.lang.reflect.Method getMethod = healthCapabilityClass.getMethod("get", LivingEntity.class);
            Object healthCapability = getMethod.invoke(null, entity);

            // 获取VISIBLE_BODIES
            Class<?> bodyComponentsClass = Class.forName("com.lastimp.dgh.api.enums.BodyComponents");
            java.util.List<?> visibleBodies = (java.util.List<?>) bodyComponentsClass.getField("VISIBLE_BODIES").get(null);

            // 获取INJURY_WEIGHT
            Class<?> injuryEventHandlerClass = Class.forName("com.lastimp.dgh.source.core.damageSystem.InjuryEventHandler");
            float[] injuryWeight = (float[]) injuryEventHandlerClass.getField("INJURY_WEIGHT").get(null);

            // 获取随机索引
            Class<?> utilsClass = Class.forName("com.lastimp.dgh.source.core.Utils");
            java.lang.reflect.Method getRandomIndexMethod = utilsClass.getMethod("getRandomIndex", float[].class);
            int randomIndex = (int) getRandomIndexMethod.invoke(null, (Object) injuryWeight);

            // 获取选中的身体部位
            Object bodyComponent = visibleBodies.get(randomIndex);

            // 获取AbstractBody
            java.lang.reflect.Method getComponentMethod = healthCapabilityClass.getMethod("getComponent", bodyComponent.getClass());
            Object body = getComponentMethod.invoke(healthCapability, bodyComponent);

            // 获取身体部位ID
            java.lang.reflect.Method getShortIDMethod = body.getClass().getMethod("getShortID");
            String bodyPartId = (String) getShortIDMethod.invoke(body);

            // 检查是否是子弹伤害
            boolean isBulletDamage = DGHIntegrationManager.isBulletDamageInProgress(player);

            if (isBulletDamage) {
                // 检查该部位是否有插板保护
                boolean hasPlate = DGHIntegrationManager.shouldApplyPlateProtection(player, bodyPartId);

                // 记录命中数据
                DGHIntegrationManager.recordHit(player, bodyPartId, hasPlate);

                // 清除子弹标记
                DGHIntegrationManager.setBulletDamageInProgress(player, false);

                System.out.println("[ArmorPlate-DGH] 记录子弹命中: " +
                        player.getName().getString() + " -> " +
                        bodyPartId + ", 有防护: " + hasPlate);
            }

        } catch (Exception e) {
            System.err.println("[ArmorPlate-DGH] 反射获取DGH数据失败: " + e.getMessage());
            // 不打印完整堆栈，避免日志过多
        }
    }
}