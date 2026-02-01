package com.armorplate.mixin.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class DghMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("ArmorPlate-DGH-MixinPlugin");

    // 缓存一次结果：Mixin 早期阶段判断即可
    private static Boolean dghPresent;

    private static boolean isDghPresent() {
        if (dghPresent != null) return dghPresent;

        try {
            // 选一个 DGH 的稳定入口类即可
            Class.forName("com.lastimp.dgh.DontGetHurt", false, DghMixinPlugin.class.getClassLoader());
            dghPresent = true;
        } catch (Throwable t) {
            dghPresent = false;
        }

        LOGGER.warn("Detected DGH present = {}", dghPresent);
        return dghPresent;
    }

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.warn("onLoad mixinPackage={}", mixinPackage);
        isDghPresent(); // 提前计算一次，方便在日志里看到
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean present = isDghPresent();
        LOGGER.warn("shouldApplyMixin mixin={} target={} dghPresent={}", mixinClassName, targetClassName, present);
        return present;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}