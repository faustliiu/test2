package com.armorplate.mixin;

import com.armorplate.dgh.DghPlateReduction;
import com.lastimp.dgh.api.bodyPart.AbstractVisibleBody;
import com.lastimp.dgh.source.core.capability.HealthCapability;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "com.lastimp.dgh.source.core.damageSystem.OpenWoundHandler", remap = false)
public abstract class OpenWoundHandlerMixin {
    @Unique private static final Logger ARMORPLATE$LOGGER = LogManager.getLogger("ArmorPlate-DGH-OpenWound");
    @Unique private static final boolean ARMORPLATE$DEBUG = true;
    @Unique private static long ARMORPLATE$lastDebugTick = -1;

    @ModifyVariable(
            method = "handleEntityAttack",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4,
            remap = false
    )
    private static float armorplate$reduceDghDamageAmount(
            float damageAmount,
            DamageSource source,
            LivingEntity entity,
            HealthCapability health,
            AbstractVisibleBody body
    ) {
        float newDamage = DghPlateReduction.applyIfProtected(entity, source, damageAmount, body);

        if (ARMORPLATE$DEBUG) {
            long tick = entity.level().getGameTime();
            if (tick != ARMORPLATE$lastDebugTick) {
                ARMORPLATE$lastDebugTick = tick;

                String componentStr;
                try {
                    componentStr = body.getComponent() != null ? body.getComponent().getString() : "null";
                } catch (Throwable t) {
                    componentStr = "<error:" + t.getClass().getSimpleName() + ">";
                }

                ARMORPLATE$LOGGER.warn(
                        "tick={} entity={} sourceMsgId={} bodyClass={} component='{}' shortId={} in={} out={}",
                        tick,
                        entity.getName().getString(),
                        source.getMsgId(),
                        body.getClass().getName(),
                        componentStr,
                        body.getShortID(),
                        damageAmount,
                        newDamage
                );
            }
        }

        return newDamage;
    }
}