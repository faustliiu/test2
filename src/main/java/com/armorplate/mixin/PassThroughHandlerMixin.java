package com.armorplate.mixin;

import com.armorplate.dgh.DGHIntegrationManager;
import com.lastimp.dgh.api.bodyPart.AbstractVisibleBody;
import com.lastimp.dgh.source.core.capability.HealthCapability;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.lastimp.dgh.source.core.damageSystem.PassThroughHandler", remap = false)
public abstract class PassThroughHandlerMixin {

    @Inject(
            method = "handleEntityAttack",
            at = @At("HEAD"),
            remap = false
    )
    private static void onHandleEntityAttack(
            DamageSource source,
            LivingEntity entity,
            HealthCapability health,  // 注意：不是Object，是HealthCapability
            AbstractVisibleBody body, // 注意：不是Object，是AbstractVisibleBody
            float damageAmount,
            CallbackInfo ci) {

        if (!ModList.get().isLoaded("dgh")) return;
        if (!(entity instanceof Player player)) return;

        try {
            // 获取身体部位的短ID - 现在可以直接调用，因为知道类型
            String bodyPartId = body.getShortID();

            // 检查是否是子弹伤害
            boolean isBulletDamage = DGHIntegrationManager.isBulletDamageInProgress(player);

            if (isBulletDamage) {
                // 检查该部位是否有插板保护
                boolean hasPlate = DGHIntegrationManager.shouldApplyPlateProtection(player, bodyPartId);

                // 记录命中数据
                DGHIntegrationManager.recordHit(player, bodyPartId, hasPlate);

                // 清除子弹标记
                DGHIntegrationManager.setBulletDamageInProgress(player, false);

                System.out.println("[ArmorPlate-DGH] 记录穿透子弹命中: " +
                        player.getName().getString() + " -> " +
                        bodyPartId + ", 有防护: " + hasPlate);
            }

        } catch (Exception e) {
            System.err.println("[ArmorPlate-DGH] 记录穿透命中数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}