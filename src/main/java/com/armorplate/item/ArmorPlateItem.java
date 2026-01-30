package com.armorplate.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArmorPlateItem extends Item {

    // 插板属性
    private final double baseArmorBonus;
    private final double baseToughnessBonus;
    private final double baseDamageReduction; // 0.0-1.0
    private final double baseDamageThreshold;
    private final int maxDurability;
    private final PlateTier tier;

    // 修复：添加6参数构造函数用于NBT读取
    public ArmorPlateItem(Properties properties,
                          double armorBonus,
                          double toughnessBonus,
                          double damageReduction,
                          double damageThreshold,
                          int maxDurability) {
        super(properties.defaultDurability(maxDurability));
        this.tier = null; // 没有使用PlateTier
        this.baseArmorBonus = armorBonus;
        this.baseToughnessBonus = toughnessBonus;
        this.baseDamageReduction = Math.min(damageReduction, 1.0);
        this.baseDamageThreshold = damageThreshold;
        this.maxDurability = maxDurability;
    }

    // 新构造函数（使用PlateTier）
    public ArmorPlateItem(PlateTier tier, Properties properties) {
        super(properties.defaultDurability(tier.getMaxDurability()));
        this.tier = tier;

        // 从PlateTier获取配置值
        this.baseArmorBonus = tier.getArmorBonus();
        this.baseToughnessBonus = tier.getToughnessBonus();
        this.baseDamageReduction = Math.min(tier.getDamageReduction(), 1.0);
        this.baseDamageThreshold = tier.getDamageThreshold();
        this.maxDurability = tier.getMaxDurability();
    }

    // 获取有效的护甲加成
    public double getEffectiveArmorBonus(ItemStack stack) {
        if (isBroken(stack)) {
            return 0.0;
        }

        double durabilityPercent = getDurabilityPercent(stack);
        if (durabilityPercent <= 0.3) {
            return this.baseArmorBonus * 0.5;
        } else if (durabilityPercent <= 0.6) {
            return this.baseArmorBonus * 0.75;
        }

        return this.baseArmorBonus;
    }

    public double getEffectiveToughnessBonus(ItemStack stack) {
        if (isBroken(stack)) {
            return 0.0;
        }

        double durabilityPercent = getDurabilityPercent(stack);
        if (durabilityPercent <= 0.3) {
            return this.baseToughnessBonus * 0.5;
        } else if (durabilityPercent <= 0.6) {
            return this.baseToughnessBonus * 0.75;
        }

        return this.baseToughnessBonus;
    }

    public double getEffectiveDamageReduction(ItemStack stack) {
        if (isBroken(stack)) {
            return 0.0;
        }

        double durabilityPercent = getDurabilityPercent(stack);
        double reduction = this.baseDamageReduction;

        if (durabilityPercent <= 0.1) {
            reduction *= 0.3;
        } else if (durabilityPercent <= 0.3) {
            reduction *= 0.5;
        } else if (durabilityPercent <= 0.6) {
            reduction *= 0.75;
        }

        return Math.min(reduction, 1.0);
    }

    public double getEffectiveDamageThreshold(ItemStack stack) {
        if (isBroken(stack)) {
            return 0.0;
        }

        double durabilityPercent = getDurabilityPercent(stack);
        double threshold = this.baseDamageThreshold;

        if (durabilityPercent <= 0.1) {
            threshold *= 0.3;
        } else if (durabilityPercent <= 0.3) {
            threshold *= 0.5;
        } else if (durabilityPercent <= 0.6) {
            threshold *= 0.75;
        }

        return threshold;
    }

    private double getDurabilityPercent(ItemStack stack) {
        int currentDamage = stack.getDamageValue();
        return Math.max(0.0, (double)(this.maxDurability - currentDamage) / this.maxDurability);
    }

    public int getCurrentDurability(ItemStack stack) {
        return Math.max(0, this.maxDurability - stack.getDamageValue());
    }

    public int getMaxDurability() {
        return this.maxDurability;
    }

    public boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= this.maxDurability;
    }

    // 修复：优化耐久消耗方法
    public void damagePlate(ItemStack stack, int amount) {
        if (isBroken(stack)) {
            return;
        }

        int newDamage = stack.getDamageValue() + amount;

        if (newDamage >= this.maxDurability) {
            // 插板损坏
            stack.setDamageValue(this.maxDurability);

            // 添加损坏标记
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean("Broken", true);
            stack.setTag(tag);
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§6防弹插板").withStyle(ChatFormatting.GOLD));

        int current = getCurrentDurability(stack);
        int max = getMaxDurability();
        double percent = getDurabilityPercent(stack) * 100;

        ChatFormatting color;
        if (percent >= 70) color = ChatFormatting.GREEN;
        else if (percent >= 30) color = ChatFormatting.YELLOW;
        else color = ChatFormatting.RED;

        tooltip.add(Component.literal(" §7耐久: ")
                .append(Component.literal(current + "/" + max).withStyle(color))
                .append(Component.literal(" (" + String.format("%.0f", percent) + "%)").withStyle(ChatFormatting.GRAY)));

        if (isBroken(stack)) {
            tooltip.add(Component.literal(" §c§l已损坏").withStyle(ChatFormatting.RED));
            return;
        }

        double armorBonus = getEffectiveArmorBonus(stack);
        double toughnessBonus = getEffectiveToughnessBonus(stack);
        double damageReduction = getEffectiveDamageReduction(stack) * 100;
        double damageThreshold = getEffectiveDamageThreshold(stack);

        tooltip.add(Component.literal(" §7属性:"));

        if (armorBonus > 0) {
            tooltip.add(Component.literal("  §7+§a" + String.format("%.1f", armorBonus) + " 护甲值"));
        }
        if (toughnessBonus > 0) {
            tooltip.add(Component.literal("  §7+§a" + String.format("%.1f", toughnessBonus) + " 韧性"));
        }
        if (damageReduction > 0) {
            tooltip.add(Component.literal("  §7+§b" + String.format("%.1f", damageReduction) + "% 子弹减伤"));
        }
        if (damageThreshold > 0) {
            tooltip.add(Component.literal("  §7+§e" + String.format("%.1f", damageThreshold) + " 伤害阈值"));
        }

        double percentEffect = getDurabilityPercent(stack) * 100;
        if (percentEffect < 100) {
            tooltip.add(Component.literal(" §7当前效果: §e" + String.format("%.0f", percentEffect) + "%"));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float)(1.0 - (double)stack.getDamageValue() / this.maxDurability));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double percent = getDurabilityPercent(stack);
        if (percent > 0.7) return ChatFormatting.GREEN.getColor();
        if (percent > 0.3) return ChatFormatting.YELLOW.getColor();
        return ChatFormatting.RED.getColor();
    }

    public PlateTier getTier() {
        return this.tier;
    }

    // 修复：添加获取基础属性的方法，用于调试
    public double getBaseArmorBonus() {
        return this.baseArmorBonus;
    }

    public double getBaseToughnessBonus() {
        return this.baseToughnessBonus;
    }

    public double getBaseDamageReduction() {
        return this.baseDamageReduction;
    }

    public double getBaseDamageThreshold() {
        return this.baseDamageThreshold;
    }
}