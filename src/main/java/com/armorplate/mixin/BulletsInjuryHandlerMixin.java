package com.armorplate.mixin;

import com.armorplate.dgh.DGHIntegrationManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.lastimp.dgh.compact.TaZC.BulletsInjuryHandler", remap = false)
public abstract class BulletsInjuryHandlerMixin {

    @Inject(
            method = "handleBullet",
            at = @At("HEAD"),
            remap = false
    )
    private static void onHandleBullet(
            DamageSource source,
            float damageAmount,
            LivingEntity entity,
            LivingDamageEvent event,
            CallbackInfo ci) {

        if (!ModList.get().isLoaded("dgh")) return;
        if (entity instanceof Player player) {
            DGHIntegrationManager.setBulletDamageInProgress(player, true);
            System.out.println("[ArmorPlate-DGH] 标记子弹伤害: " + player.getName().getString());
        }
    }

    @Inject(
            method = "handleBulletByPass",
            at = @At("HEAD"),
            remap = false
    )
    private static void onHandleBulletByPass(
            DamageSource source,
            float damageAmount,
            LivingEntity entity,
            LivingDamageEvent event,
            CallbackInfo ci) {

        if (!ModList.get().isLoaded("dgh")) return;
        if (entity instanceof Player player) {
            DGHIntegrationManager.setBulletDamageInProgress(player, true);
            System.out.println("[ArmorPlate-DGH] 标记穿透子弹伤害: " + player.getName().getString());
        }
    }
}